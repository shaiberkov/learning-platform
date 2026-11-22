package org.example.learningprojectserver.service.livequiz;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.*;
import org.example.learningprojectserver.entities.ClassRoomEntity;
import org.example.learningprojectserver.entities.StudentEntity;
import org.example.learningprojectserver.entities.TeacherEntity;
import org.example.learningprojectserver.notification.NotificationType;
import org.example.learningprojectserver.notification.dto.NotificationDTO;
import org.example.learningprojectserver.notification.publisher.NotificationEventPublisher;
import org.example.learningprojectserver.repository.TeacherRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.questionbank.QuestionBankService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Live in-class quiz. The teacher opens a session for one class and puts questions to it one at a time;
 * students answer from their phones and the teacher sees the answers as they arrive.
 *
 * Sessions live only in this process's memory (like the SSE emitters they push through): a restart ends
 * every quiz, and a second instance would not see them. Pushes go over the existing SSE channel
 * (LIVE_QUIZ_STATE to the class, LIVE_QUIZ_RESULTS to the teacher); the screens also poll, so a dropped
 * SSE connection only slows them down. Sessions idle for SESSION_TTL are dropped on the next access.
 */
@Service
@RequiredArgsConstructor
public class LiveQuizService {

    static final Duration SESSION_TTL = Duration.ofHours(3);
    private static final int MAX_ANSWER_LENGTH = 300;

    private final TeacherRepository teacherRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final QuestionBankService questionBankService;

    /** code -> session */
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    /** Not read-only: replacing a running quiz writes the closing question's statistics. */
    @Transactional
    public BasicResponse<LiveQuizResultsDTO> start(String teacherId, Long classRoomId) {
        purgeExpired();
        TeacherEntity teacher = teacherRepository.findByUserId(teacherId);
        if (teacher == null) {
            return new BasicResponse<>(false, "המורה לא נמצא");
        }
        ClassRoomEntity classRoom = teacher.getTeachingClassRooms().stream()
                .filter(c -> c.getId().equals(classRoomId))
                .findFirst().orElse(null);
        if (classRoom == null) {
            return new BasicResponse<>(false, "הכיתה לא נמצאה או שאינך מלמד בה");
        }
        if (classRoom.getStudents().isEmpty()) {
            return new BasicResponse<>(false, "אין תלמידים בכיתה זו");
        }

        findTeacherSession(teacherId).ifPresent(this::finish);

        Map<String, String> students = new LinkedHashMap<>();
        classRoom.getStudents().stream()
                .sorted(Comparator.comparing(StudentEntity::getUsername, Comparator.nullsLast(String::compareTo)))
                .forEach(s -> students.put(s.getUserId(), s.getUsername()));

        Session session = new Session(newCode(), teacherId, teacher.getUsername(), classRoom.getId(),
                classRoom.getName(), students);
        sessions.put(session.code, session);

        broadcastState(session);
        BasicResponse<LiveQuizResultsDTO> response = new BasicResponse<>(true,
                "החידון נפתח. קוד החידון: " + session.code);
        response.setData(results(session));
        return response;
    }

    public BasicResponse<LiveQuizResultsDTO> pushQuestion(String teacherId, String code, LiveQuestionRequest request) {
        Session session = teacherSession(teacherId, code);
        if (session == null) return new BasicResponse<>(false, "החידון לא נמצא או שהסתיים");
        if (request == null) return new BasicResponse<>(false, "לא הוזנה שאלה");

        Question question;
        if (request.getBankQuestionId() != null) {
            BankQuestionDTO bank = questionBankService.findUsableQuestion(teacherId, request.getBankQuestionId());
            if (bank == null) return new BasicResponse<>(false, "השאלה לא נמצאה במאגר שלך");
            question = new Question(bank.getId(), bank.getQuestionText(), bank.getOptions(), bank.getAnswer());
        } else {
            BankQuestionDTO typed = new BankQuestionDTO();
            typed.setQuestionText(request.getQuestionText());
            typed.setAnswer(request.getAnswer());
            typed.setOptions(request.getOptions());
            String error = QuestionBankService.validate(typed);
            if (error != null) return new BasicResponse<>(false, error);
            List<String> options = request.getOptions() == null ? List.of() : request.getOptions().stream()
                    .filter(Objects::nonNull).map(String::trim).filter(o -> !o.isEmpty()).distinct().toList();
            question = new Question(null, request.getQuestionText().trim(), options, request.getAnswer().trim());
        }

        synchronized (session) {
            closeCurrent(session);
            session.current = question;
            session.questionNumber++;
            session.questionOpen = true;
            session.answers.clear();
            session.touch();
        }
        broadcastState(session);
        BasicResponse<LiveQuizResultsDTO> response = new BasicResponse<>(true, "השאלה נשלחה לכיתה");
        response.setData(results(session));
        return response;
    }

    public BasicResponse<LiveQuizResultsDTO> closeQuestion(String teacherId, String code) {
        Session session = teacherSession(teacherId, code);
        if (session == null) return new BasicResponse<>(false, "החידון לא נמצא או שהסתיים");
        synchronized (session) {
            if (session.current == null || !session.questionOpen) {
                return new BasicResponse<>(false, "אין שאלה פתוחה");
            }
            closeCurrent(session);
            session.touch();
        }
        broadcastState(session);
        BasicResponse<LiveQuizResultsDTO> response = new BasicResponse<>(true, "השאלה נסגרה והתשובה הוצגה לתלמידים");
        response.setData(results(session));
        return response;
    }

    public BasicResponse<LiveQuizResultsDTO> end(String teacherId, String code) {
        Session session = teacherSession(teacherId, code);
        if (session == null) return new BasicResponse<>(false, "החידון לא נמצא או שהסתיים");
        LiveQuizResultsDTO last = finish(session);
        BasicResponse<LiveQuizResultsDTO> response = new BasicResponse<>(true, "החידון הסתיים");
        response.setData(last);
        return response;
    }

    /** The teacher's active quiz; code may be null to find it after a page reload. data is null when none. */
    public BasicResponse<LiveQuizResultsDTO> getResults(String teacherId, String code) {
        purgeExpired();
        Session session = (code == null || code.isBlank())
                ? findTeacherSession(teacherId).orElse(null)
                : teacherSession(teacherId, code);
        BasicResponse<LiveQuizResultsDTO> response = new BasicResponse<>(true, null);
        response.setData(session == null ? null : results(session));
        return response;
    }

    /** The quiz running for the student's class, from the student's side. data is null when none. */
    public BasicResponse<LiveQuizStateDTO> getCurrent(String studentId) {
        purgeExpired();
        Session session = findStudentSession(studentId).orElse(null);
        BasicResponse<LiveQuizStateDTO> response = new BasicResponse<>(true, null);
        response.setData(session == null ? null : state(session, studentId));
        return response;
    }

    public BasicResponse<LiveQuizStateDTO> answer(String studentId, String code, String answer) {
        Session session = code == null ? null : sessions.get(code.trim());
        if (session == null || !session.students.containsKey(studentId)) {
            return new BasicResponse<>(false, "החידון לא נמצא או שהסתיים");
        }
        if (answer == null || answer.isBlank()) {
            return new BasicResponse<>(false, "יש לבחור או להקליד תשובה");
        }
        if (answer.length() > MAX_ANSWER_LENGTH) {
            return new BasicResponse<>(false, "התשובה ארוכה מדי");
        }
        synchronized (session) {
            if (session.current == null || !session.questionOpen) {
                return new BasicResponse<>(false, "השאלה כבר נסגרה");
            }
            if (session.answers.containsKey(studentId)) {
                return new BasicResponse<>(false, "כבר ענית על שאלה זו");
            }
            boolean correct = isCorrect(answer, session.current.answer());
            session.answers.put(studentId, new Answer(answer.trim(), correct));
            int[] score = session.scoreboard.computeIfAbsent(studentId, k -> new int[2]);
            score[0]++;
            if (correct) score[1]++;
            session.touch();
        }
        notificationEventPublisher.publish(List.of(session.teacherId),
                new NotificationDTO<>(NotificationType.LIVE_QUIZ_RESULTS, results(session)));

        BasicResponse<LiveQuizStateDTO> response = new BasicResponse<>(true, "התשובה נקלטה");
        response.setData(state(session, studentId));
        return response;
    }

    static boolean isCorrect(String given, String expected) {
        return normalize(given).equals(normalize(expected));
    }

    static String normalize(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    // ---- internals ----

    private LiveQuizResultsDTO finish(Session session) {
        synchronized (session) {
            closeCurrent(session);
            session.ended = true;
        }
        sessions.remove(session.code);
        notificationEventPublisher.publish(new ArrayList<>(session.students.keySet()),
                new NotificationDTO<>(NotificationType.LIVE_QUIZ_STATE, LiveQuizStateDTO.ended(session.code)));
        return results(session);
    }

    /** Closes the open question (if any) and adds its answers to the bank question's statistics. */
    private void closeCurrent(Session session) {
        if (session.current == null || !session.questionOpen) return;
        session.questionOpen = false;
        if (session.current.bankQuestionId() != null) {
            int attempts = session.answers.size();
            int correct = (int) session.answers.values().stream().filter(Answer::correct).count();
            try {
                questionBankService.recordUsage(session.current.bankQuestionId(), attempts, correct);
            } catch (RuntimeException ignored) {
                // Statistics are best effort; a failure must not break the running quiz.
            }
        }
    }

    private void broadcastState(Session session) {
        notificationEventPublisher.publish(new ArrayList<>(session.students.keySet()),
                new NotificationDTO<>(NotificationType.LIVE_QUIZ_STATE, state(session, null)));
    }

    private LiveQuizStateDTO state(Session session, String studentId) {
        synchronized (session) {
            Question q = session.current;
            Answer mine = studentId == null ? null : session.answers.get(studentId);
            boolean revealed = q != null && !session.questionOpen;
            return new LiveQuizStateDTO(!session.ended, session.code, session.teacherName, session.className,
                    session.questionNumber,
                    q == null ? null : q.text(),
                    q == null ? List.of() : q.options(),
                    session.questionOpen,
                    revealed ? q.answer() : null,
                    mine == null ? null : mine.answer(),
                    mine == null || !revealed ? null : mine.correct());
        }
    }

    private LiveQuizResultsDTO results(Session session) {
        synchronized (session) {
            Question q = session.current;
            Map<String, Integer> counts = new LinkedHashMap<>();
            if (q != null) {
                for (String option : q.options()) counts.put(option, 0);
                for (Answer a : session.answers.values()) {
                    String key = q.options().stream().filter(o -> isCorrect(o, a.answer())).findFirst().orElse(a.answer());
                    counts.merge(key, 1, Integer::sum);
                }
            }
            List<LiveQuizResultsDTO.StudentAnswer> answers = new ArrayList<>();
            session.answers.forEach((id, a) -> answers.add(
                    new LiveQuizResultsDTO.StudentAnswer(session.students.get(id), a.answer(), a.correct())));
            List<LiveQuizResultsDTO.ScoreRow> scoreboard = new ArrayList<>();
            session.students.forEach((id, name) -> {
                int[] s = session.scoreboard.getOrDefault(id, new int[2]);
                scoreboard.add(new LiveQuizResultsDTO.ScoreRow(name, s[0], s[1]));
            });
            scoreboard.sort(Comparator.comparingInt(LiveQuizResultsDTO.ScoreRow::correct).reversed()
                    .thenComparing(LiveQuizResultsDTO.ScoreRow::studentName, Comparator.nullsLast(String::compareTo)));
            int correct = (int) session.answers.values().stream().filter(Answer::correct).count();
            return new LiveQuizResultsDTO(!session.ended, session.code, session.classRoomId, session.className,
                    session.questionNumber,
                    q == null ? null : q.text(),
                    q == null ? List.of() : q.options(),
                    q == null ? null : q.answer(),
                    session.questionOpen,
                    session.students.size(), session.answers.size(), correct,
                    counts, answers, scoreboard);
        }
    }

    private Session teacherSession(String teacherId, String code) {
        purgeExpired();
        Session session = code == null ? null : sessions.get(code.trim());
        return session != null && session.teacherId.equals(teacherId) ? session : null;
    }

    private Optional<Session> findTeacherSession(String teacherId) {
        return sessions.values().stream().filter(s -> s.teacherId.equals(teacherId)).findFirst();
    }

    private Optional<Session> findStudentSession(String studentId) {
        return sessions.values().stream()
                .filter(s -> s.students.containsKey(studentId))
                .max(Comparator.comparing(s -> s.lastActivity));
    }

    private void purgeExpired() {
        Instant cutoff = Instant.now().minus(SESSION_TTL);
        sessions.values().removeIf(s -> s.lastActivity.isBefore(cutoff));
    }

    private String newCode() {
        String code;
        do {
            code = String.format("%06d", random.nextInt(1_000_000));
        } while (sessions.containsKey(code));
        return code;
    }

    record Question(Long bankQuestionId, String text, List<String> options, String answer) {}

    record Answer(String answer, boolean correct) {}

    static final class Session {
        final String code;
        final String teacherId;
        final String teacherName;
        final Long classRoomId;
        final String className;
        /** student userId -> name, fixed when the session opens */
        final Map<String, String> students;
        /** answers to the current question: student userId -> answer */
        final Map<String, Answer> answers = new LinkedHashMap<>();
        /** student userId -> [answered, correct] over the whole session */
        final Map<String, int[]> scoreboard = new HashMap<>();
        Question current;
        int questionNumber;
        boolean questionOpen;
        boolean ended;
        volatile Instant lastActivity = Instant.now();

        Session(String code, String teacherId, String teacherName, Long classRoomId, String className,
                Map<String, String> students) {
            this.code = code;
            this.teacherId = teacherId;
            this.teacherName = teacherName;
            this.classRoomId = classRoomId;
            this.className = className;
            this.students = students;
        }

        void touch() {
            lastActivity = Instant.now();
        }
    }
}
