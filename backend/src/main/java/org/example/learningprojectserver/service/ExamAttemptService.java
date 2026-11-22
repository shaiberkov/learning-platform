package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.exam.AttemptMonitorView;
import org.example.learningprojectserver.dto.exam.ExamAttemptView;
import org.example.learningprojectserver.dto.exam.ExamQuestionView;
import org.example.learningprojectserver.dto.exam.ExamResultView;
import org.example.learningprojectserver.entities.*;
import org.example.learningprojectserver.enums.ExamAttemptStatus;
import org.example.learningprojectserver.enums.IntegrityEventType;
import org.example.learningprojectserver.repository.*;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.exam.ExamGrader;
import org.example.learningprojectserver.service.exam.ExamQuestionCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Runs a student's exam attempt with the server as the clock.
 * Start records the time and the deadline (scheduled start + time limit); answers are autosaved;
 * a submission after the deadline (plus a short network grace) is rejected and the attempt is graded
 * from what was saved; {@code jobs/ExamSchedulerJob} grades attempts nobody submitted.
 */
@Service
@RequiredArgsConstructor
public class ExamAttemptService {

    public static final Duration SUBMIT_GRACE = Duration.ofSeconds(30);
    public static final Duration LATE_START_LIMIT = Duration.ofMinutes(15);
    static final DateTimeFormatter START_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static final DateTimeFormatter STAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final SecureRandom SEEDS = new SecureRandom();

    private final TeacherTestRepository teacherTestRepository;
    private final TeacherTestResultRepository teacherTestResultRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAttemptAnswerRepository examAttemptAnswerRepository;
    private final ExamIntegrityEventRepository examIntegrityEventRepository;
    private final StudentRepository studentRepository;
    private final ExamQuestionCodec examQuestionCodec;
    private final ExamGrader examGrader;
    private final CacheManager cacheManager;
    private final ExamFeedbackService examFeedbackService;
    private final EngagementEvents engagementEvents;

    private static final Logger log = LoggerFactory.getLogger(ExamAttemptService.class);

    @Transactional
    public BasicResponse<ExamAttemptView> startAttempt(String userId, Long testId) {
        TeacherTestEntity test = teacherTestRepository.findTeacherTestByTestId(testId);
        if (test == null) {
            return new BasicResponse<>(false, "מבחן לא קיים");
        }
        if (test.isTemplateExam()) {
            return new BasicResponse<>(false, "זוהי תבנית מבחן ולא מבחן פעיל");
        }
        if (!test.isPublishedNow()) {
            return new BasicResponse<>(false, "המבחן עדיין לא פורסם");
        }
        if (!teacherTestRepository.isStudentAssigned(testId, userId)) {
            return new BasicResponse<>(false, "המבחן לא שובץ עבורך");
        }

        LocalDateTime now = LocalDateTime.now();
        ExamAttemptEntity attempt = examAttemptRepository.findByTestAndStudent(testId, userId);
        if (attempt != null) {
            if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
                return new BasicResponse<>(false, "כבר הגשת את המבחן הזה");
            }
            if (isPastGrace(attempt, now)) {
                finalizeAttempt(attempt, true, now);
                return new BasicResponse<>(false, "הזמן למבחן הסתיים והמבחן הוגש אוטומטית לפי התשובות שנשמרו");
            }
            return ok(buildView(attempt, now), "המבחן ממשיך מהנקודה שבה עצרת");
        }

        if (teacherTestResultRepository.existsForTestAndStudent(testId, userId)) {
            return new BasicResponse<>(false, "כבר הגשת את המבחן הזה");
        }

        LocalDateTime start;
        try {
            start = LocalDateTime.parse(test.getStartTime(), START_FORMAT);
        } catch (DateTimeParseException | NullPointerException e) {
            return new BasicResponse<>(false, "מועד המבחן אינו תקין");
        }
        if (now.isBefore(start)) {
            return new BasicResponse<>(false, "הזמן טרם הגיע להתחלת המבחן");
        }
        LocalDateTime deadline = start.plusMinutes(test.getTimeLimitMinutes());

        attempt = new ExamAttemptEntity();
        attempt.setTest(test);
        attempt.setStudentUserId(userId);
        attempt.setStartedAt(now);
        attempt.setSeed(SEEDS.nextLong());
        attempt.setStatus(ExamAttemptStatus.IN_PROGRESS);

        if (now.isAfter(start.plus(LATE_START_LIMIT)) || !now.isBefore(deadline)) {
            attempt.setDeadline(now);
            examAttemptRepository.save(attempt);
            finalizeAttempt(attempt, true, now);
            return new BasicResponse<>(false, "המבחן לא יכול להתחיל אחרי 15 דקות מתחילת הזמן לכן ציון 0");
        }

        attempt.setDeadline(deadline);
        examAttemptRepository.save(attempt);
        return ok(buildView(attempt, now), "המבחן התחיל בהצלחה");
    }

    /** Autosave. Returns the seconds left by the server's clock. */
    @Transactional
    public BasicResponse<Long> saveAnswers(String userId, Long attemptId, Map<Long, String> answers) {
        ExamAttemptEntity attempt = examAttemptRepository.findById(attemptId).orElse(null);
        String error = checkOwnedAndOpen(attempt, userId);
        if (error != null) {
            return new BasicResponse<>(false, error);
        }
        LocalDateTime now = LocalDateTime.now();
        if (isPastGrace(attempt, now)) {
            finalizeAttempt(attempt, true, now);
            return new BasicResponse<>(false, "הזמן הסתיים, המבחן הוגש אוטומטית");
        }
        mergeAnswers(attempt, answers);
        return ok(secondsLeft(attempt, now), null);
    }

    @Transactional
    public BasicResponse<ExamResultView> submitAttempt(String userId, Long attemptId, Map<Long, String> answers) {
        ExamAttemptEntity attempt = examAttemptRepository.findById(attemptId).orElse(null);
        String error = checkOwnedAndOpen(attempt, userId);
        if (error != null) {
            return new BasicResponse<>(false, error);
        }
        LocalDateTime now = LocalDateTime.now();
        if (isPastGrace(attempt, now)) {
            ExamResultView result = finalizeAttempt(attempt, true, now);
            BasicResponse<ExamResultView> late = new BasicResponse<>(false,
                    "ההגשה נדחתה כי הזמן הסתיים. המבחן נבדק לפי התשובות שנשמרו עד תום הזמן");
            late.setData(result);
            return late;
        }
        mergeAnswers(attempt, answers);
        return ok(finalizeAttempt(attempt, false, now), "המבחן הוגש בהצלחה");
    }

    @Transactional
    public BasicResponse<Integer> recordIntegrityEvent(String userId, Long attemptId, String type) {
        ExamAttemptEntity attempt = examAttemptRepository.findById(attemptId).orElse(null);
        String error = checkOwnedAndOpen(attempt, userId);
        if (error != null) {
            return new BasicResponse<>(false, error);
        }
        IntegrityEventType eventType;
        try {
            eventType = IntegrityEventType.valueOf(type);
        } catch (IllegalArgumentException | NullPointerException e) {
            return new BasicResponse<>(false, "סוג אירוע לא מוכר");
        }
        ExamIntegrityEventEntity event = new ExamIntegrityEventEntity();
        event.setAttempt(attempt);
        event.setType(eventType);
        event.setOccurredAt(LocalDateTime.now());
        examIntegrityEventRepository.save(event);
        if (eventType == IntegrityEventType.TAB_HIDDEN) {
            attempt.setTabLeaveCount(attempt.getTabLeaveCount() + 1);
        }
        return ok(attempt.getTabLeaveCount(), null);
    }

    /** Ids of attempts whose deadline (plus grace) passed without a submission. */
    public List<Long> findExpiredAttemptIds() {
        return examAttemptRepository.findIdsByStatusAndDeadlineBefore(
                ExamAttemptStatus.IN_PROGRESS, LocalDateTime.now().minus(SUBMIT_GRACE));
    }

    /** Grades one expired attempt from its saved answers. Called by the scheduler, one transaction each. */
    @Transactional
    public void autoSubmitExpired(Long attemptId) {
        ExamAttemptEntity attempt = examAttemptRepository.findById(attemptId).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (attempt != null && attempt.getStatus() == ExamAttemptStatus.IN_PROGRESS && isPastGrace(attempt, now)) {
            finalizeAttempt(attempt, true, now);
        }
    }

    /** Every assigned student's attempt with the tab-leave log, for the teacher's monitor. */
    @Transactional(readOnly = true)
    public BasicResponse<List<AttemptMonitorView>> getAttemptsForTeacher(String teacherId, Long testId) {
        TeacherTestEntity test = teacherTestRepository.findTeacherTestByTestId(testId);
        if (test == null || test.getTeacher() == null || !test.getTeacher().getUserId().equals(teacherId)) {
            return new BasicResponse<>(false, "המבחן לא נמצא או שאינו שלך");
        }
        Map<Long, List<ExamIntegrityEventEntity>> eventsByAttempt = examIntegrityEventRepository.findByTestId(testId)
                .stream().collect(Collectors.groupingBy(e -> e.getAttempt().getId()));
        Map<String, ExamAttemptEntity> attemptByStudent = examAttemptRepository.findByTestId(testId).stream()
                .collect(Collectors.toMap(ExamAttemptEntity::getStudentUserId, a -> a, (a, b) -> a));

        List<AttemptMonitorView> rows = new ArrayList<>();
        for (StudentEntity student : test.getStudents()) {
            AttemptMonitorView row = new AttemptMonitorView();
            row.setStudentId(student.getUserId());
            row.setStudentName(student.getUsername());
            ExamAttemptEntity a = attemptByStudent.get(student.getUserId());
            if (a == null) {
                row.setStatus("NOT_STARTED");
            } else {
                row.setAttemptId(a.getId());
                row.setStatus(a.getStatus().name());
                row.setStartedAt(format(a.getStartedAt()));
                row.setDeadline(format(a.getDeadline()));
                row.setSubmittedAt(format(a.getSubmittedAt()));
                row.setScore(a.getScore());
                row.setTabLeaveCount(a.getTabLeaveCount());
                for (ExamIntegrityEventEntity e : eventsByAttempt.getOrDefault(a.getId(), List.of())) {
                    row.getEvents().add(new AttemptMonitorView.Event(e.getType().name(), format(e.getOccurredAt())));
                }
            }
            rows.add(row);
        }
        return ok(rows, null);
    }

    // ---- internals ----

    private ExamAttemptView buildView(ExamAttemptEntity attempt, LocalDateTime now) {
        TeacherTestEntity test = attempt.getTest();
        boolean shuffleQuestions = !Boolean.FALSE.equals(test.getShuffleQuestions());
        boolean shuffleOptions = !Boolean.FALSE.equals(test.getShuffleOptions());

        List<TestQuestionEntity> ordered = orderedQuestions(test);
        if (shuffleQuestions) {
            Collections.shuffle(ordered, new Random(attempt.getSeed()));
        }
        List<ExamQuestionView> questions = new ArrayList<>();
        for (TestQuestionEntity q : ordered) {
            questions.add(examQuestionCodec.toView(q, attempt.getSeed(), shuffleOptions));
        }

        ExamAttemptView view = new ExamAttemptView();
        view.setAttemptId(attempt.getId());
        view.setTestId(test.getId());
        view.setTitle(test.getTitle() != null ? test.getTitle() : test.getSubject());
        view.setSubject(test.getSubject());
        view.setTopic(test.getTopic());
        view.setVersionLabel(test.getVersionLabel());
        view.setRemainingSeconds(secondsLeft(attempt, now));
        view.setDeadline(format(attempt.getDeadline()));
        view.setQuestions(questions);
        view.setSavedAnswers(ExamQuestionCodec.readAnswers(attempt.getSavedAnswersJson()));
        return view;
    }

    static List<TestQuestionEntity> orderedQuestions(TestEntity test) {
        List<TestQuestionEntity> list = new ArrayList<>(test.getQuestions());
        list.sort(Comparator.comparing((TestQuestionEntity q) -> q.getPosition() == null ? Integer.MAX_VALUE : q.getPosition())
                .thenComparing(TestQuestionEntity::getId));
        return list;
    }

    private void mergeAnswers(ExamAttemptEntity attempt, Map<Long, String> answers) {
        if (answers == null || answers.isEmpty()) {
            return;
        }
        Set<Long> questionIds = attempt.getTest().getQuestions().stream()
                .map(QuestionEntity::getId).collect(Collectors.toSet());
        Map<Long, String> saved = ExamQuestionCodec.readAnswers(attempt.getSavedAnswersJson());
        answers.forEach((qid, value) -> {
            if (questionIds.contains(qid)) {
                saved.put(qid, value == null ? "" : value);
            }
        });
        attempt.setSavedAnswersJson(ExamQuestionCodec.write(saved));
    }

    private ExamResultView finalizeAttempt(ExamAttemptEntity attempt, boolean auto, LocalDateTime now) {
        TeacherTestEntity test = attempt.getTest();
        Map<Long, String> answers = ExamQuestionCodec.readAnswers(attempt.getSavedAnswersJson());

        int totalPoints = 0;
        int earned = 0;
        int correctCount = 0;
        List<ExamResultView.QuestionResult> results = new ArrayList<>();
        List<ExamAttemptAnswerEntity> rows = new ArrayList<>();
        for (TestQuestionEntity q : orderedQuestions(test)) {
            String answer = answers.get(q.getId());
            boolean correct = examGrader.isCorrect(q, answer);
            int points = q.getPointsOrDefault();
            totalPoints += points;
            if (correct) {
                earned += points;
                correctCount++;
            }
            ExamAttemptAnswerEntity row = new ExamAttemptAnswerEntity();
            row.setAttempt(attempt);
            row.setQuestionId(q.getId());
            row.setAnswer(answer);
            row.setCorrect(correct);
            row.setPointsEarned(correct ? points : 0);
            rows.add(row);
            results.add(new ExamResultView.QuestionResult(q.getId(), q.getDisplayText(), correct, correct ? points : 0, points));
        }
        examAttemptAnswerRepository.saveAll(rows);

        int score = totalPoints == 0 ? 0 : (int) Math.round(earned * 100.0 / totalPoints);
        attempt.setStatus(auto ? ExamAttemptStatus.AUTO_SUBMITTED : ExamAttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(now);
        attempt.setScore(score);
        attempt.setCorrectCount(correctCount);
        examAttemptRepository.save(attempt);

        saveLegacyResult(attempt, test, score, correctCount, results.size(), now);
        recordForFeedback(attempt.getStudentUserId(), test, answers);
        engagementEvents.testCompleted(attempt.getStudentUserId(), score, true);

        ExamResultView view = new ExamResultView();
        view.setScore(score);
        view.setCorrectCount(correctCount);
        view.setTotalQuestions(results.size());
        view.setAutoSubmitted(auto);
        view.setQuestions(results);
        return view;
    }

    /**
     * Post-exam feedback, mistakes notebook, mastery and teacher insights read test_answer_record.
     * Failures are logged and never fail the submission.
     */
    private void recordForFeedback(String userId, TeacherTestEntity test, Map<Long, String> answers) {
        try {
            examFeedbackService.recordExamAnswers(userId, test, ExamFeedbackService.TEACHER_TEST, answers,
                    examGrader::isCorrect);
        } catch (RuntimeException e) {
            log.warn("Could not record exam answers of test {} for {}: {}", test.getId(), userId, e.getMessage());
        }
    }

    /** Keeps the student's tests table and statistics, which read TeacherTestResultEntity, working. */
    private void saveLegacyResult(ExamAttemptEntity attempt, TeacherTestEntity test, int score,
                                  int correctCount, int questionCount, LocalDateTime now) {
        String userId = attempt.getStudentUserId();
        if (!teacherTestResultRepository.existsForTestAndStudent(test.getId(), userId)) {
            StudentEntity student = studentRepository.findStudentByStudentId(userId);
            TeacherTestResultEntity result = new TeacherTestResultEntity();
            result.setStudent(student);
            result.setTeacher(test.getTeacher());
            result.setTest(test);
            result.setScore(score);
            result.setCorrectAnswersCount(correctCount);
            result.setIncorrectAnswersCount(questionCount - correctCount);
            result.setFinishTime(now.format(DateTimeFormatter.ofPattern("HH:mm")));
            teacherTestResultRepository.save(result);
        }
        Cache cache = cacheManager.getCache("studentTestsStatus");
        if (cache != null) {
            cache.evict(userId);
        }
    }

    private String checkOwnedAndOpen(ExamAttemptEntity attempt, String userId) {
        if (attempt == null || !attempt.getStudentUserId().equals(userId)) {
            return "ניסיון המבחן לא נמצא";
        }
        if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
            return "המבחן כבר הוגש";
        }
        return null;
    }

    private static boolean isPastGrace(ExamAttemptEntity attempt, LocalDateTime now) {
        return now.isAfter(attempt.getDeadline().plus(SUBMIT_GRACE));
    }

    private static long secondsLeft(ExamAttemptEntity attempt, LocalDateTime now) {
        return Math.max(0, Duration.between(now, attempt.getDeadline()).getSeconds());
    }

    private static String format(LocalDateTime t) {
        return t == null ? null : t.format(STAMP_FORMAT);
    }

    private static <T> BasicResponse<T> ok(T data, String message) {
        BasicResponse<T> response = new BasicResponse<>(true, message);
        response.setData(data);
        return response;
    }
}
