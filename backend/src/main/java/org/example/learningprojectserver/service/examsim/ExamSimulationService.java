package org.example.learningprojectserver.service.examsim;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.examsim.ExamSimDTOs.*;
import org.example.learningprojectserver.entities.*;
import org.example.learningprojectserver.enums.ExamAttemptStatus;
import org.example.learningprojectserver.mappers.PastExamQuestionEntityToSimQuestionViewMapper;
import org.example.learningprojectserver.repository.ExamSimAttemptRepository;
import org.example.learningprojectserver.repository.PastExamRepository;
import org.example.learningprojectserver.repository.TopicMasteryRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.ExamAttemptService;
import org.example.learningprojectserver.service.MasteryService;
import org.example.learningprojectserver.service.adaptive.SkillCatalog;
import org.example.learningprojectserver.service.exam.ExamQuestionCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Past-exam simulations (סימולציית בגרות): the student sits a bank exam under full conditions.
 * The server owns the clock (deadline = start + the exam's duration, the same grace as teacher exams),
 * answers are autosaved, and grading follows the rubric (ExamSimGrader). An attempt left open past its
 * deadline is graded from its saved answers (or discarded when it has none) the next time the student
 * lists exams or starts one.
 * Answers also move the student's mastery, which feeds the score prediction (ScorePredictor).
 * Nothing here is cached.
 */
@Service
@RequiredArgsConstructor
public class ExamSimulationService {

    static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Logger log = LoggerFactory.getLogger(ExamSimulationService.class);

    private final PastExamRepository pastExamRepository;
    private final ExamSimAttemptRepository attemptRepository;
    private final TopicMasteryRepository topicMasteryRepository;
    private final MasteryService masteryService;
    private final PastExamQuestionEntityToSimQuestionViewMapper questionMapper;

    @Transactional
    public BasicResponse<List<PastExamSummaryDTO>> listExams(String userId) {
        LocalDateTime now = LocalDateTime.now();
        List<ExamSimAttemptEntity> attempts = new ArrayList<>(attemptRepository.findByStudent(userId));
        attempts.removeIf(a -> a.getStatus() == ExamAttemptStatus.IN_PROGRESS && isPastGrace(a, now) && !expire(a, now));
        Map<Long, List<ExamSimAttemptEntity>> byExam = new HashMap<>();
        for (ExamSimAttemptEntity a : attempts) byExam.computeIfAbsent(a.getExam().getId(), k -> new ArrayList<>()).add(a);

        List<PastExamSummaryDTO> out = new ArrayList<>();
        for (PastExamEntity exam : pastExamRepository.findAllByOrderByExamYearDescIdAsc()) {
            List<ExamSimAttemptEntity> mine = byExam.getOrDefault(exam.getId(), List.of()); // newest first
            List<ExamSimAttemptEntity> done = mine.stream().filter(a -> a.getScore() != null).toList();
            Integer best = done.stream().map(ExamSimAttemptEntity::getScore).max(Integer::compare).orElse(null);
            Integer last = done.isEmpty() ? null : done.get(0).getScore();
            Long lastId = done.isEmpty() ? null : done.get(0).getId();
            Long open = mine.stream().filter(a -> a.getStatus() == ExamAttemptStatus.IN_PROGRESS)
                    .map(ExamSimAttemptEntity::getId).findFirst().orElse(null);
            int questions = exam.getSections().stream().mapToInt(s -> s.getQuestions().size()).sum();
            out.add(new PastExamSummaryDTO(exam.getId(), exam.getTitle(), exam.getSubject(), exam.getExamYear(),
                    exam.getSessionLabel(), exam.getDescription(), exam.getDurationMinutes(), exam.getSections().size(),
                    questions, done.size(), best, last, lastId, open));
        }
        return ok(out, out.isEmpty() ? "מאגר הבחינות עדיין ריק" : null);
    }

    @Transactional(readOnly = true)
    public BasicResponse<ScorePredictionDTO> predict(String userId, Long examId) {
        PastExamEntity exam = examId == null ? null : pastExamRepository.findById(examId).orElse(null);
        if (exam == null) return new BasicResponse<>(false, "הבחינה לא נמצאה");
        return ok(prediction(userId, exam), null);
    }

    @Transactional
    public BasicResponse<SimAttemptView> start(String userId, Long examId) {
        PastExamEntity exam = examId == null ? null : pastExamRepository.findById(examId).orElse(null);
        if (exam == null) return new BasicResponse<>(false, "הבחינה לא נמצאה");
        LocalDateTime now = LocalDateTime.now();
        String note = null;
        for (ExamSimAttemptEntity open : attemptRepository.findByStudentExamAndStatus(userId, examId, ExamAttemptStatus.IN_PROGRESS)) {
            if (!isPastGrace(open, now)) return ok(view(open, now), "הסימולציה ממשיכה מהנקודה שבה עצרת");
            if (expire(open, now)) note = "הזמן לסימולציה הקודמת הסתיים והיא נבדקה לפי התשובות שנשמרו. ";
        }
        ScorePredictionDTO prediction = prediction(userId, exam);
        ExamSimAttemptEntity attempt = new ExamSimAttemptEntity();
        attempt.setExam(exam);
        attempt.setStudentUserId(userId);
        attempt.setStartedAt(now);
        attempt.setDeadline(now.plusMinutes(exam.getDurationMinutes()));
        attempt.setStatus(ExamAttemptStatus.IN_PROGRESS);
        attempt.setPredictedScore(prediction.expectedScore());
        attempt.setPredictedMargin(prediction.margin());
        attemptRepository.save(attempt);
        return ok(view(attempt, now), (note == null ? "" : note) + "הסימולציה התחילה. בהצלחה!");
    }

    /** Autosave. Returns the seconds left by the server's clock. */
    @Transactional
    public BasicResponse<Long> save(String userId, Long attemptId, Map<Long, String> answers) {
        ExamSimAttemptEntity attempt = attemptId == null ? null : attemptRepository.findById(attemptId).orElse(null);
        String error = checkOwnedAndOpen(attempt, userId);
        if (error != null) return new BasicResponse<>(false, error);
        LocalDateTime now = LocalDateTime.now();
        if (isPastGrace(attempt, now)) {
            finalizeAttempt(attempt, true, now);
            return new BasicResponse<>(false, "הזמן הסתיים, הסימולציה הוגשה אוטומטית");
        }
        mergeAnswers(attempt, answers);
        return ok(secondsLeft(attempt, now), null);
    }

    @Transactional
    public BasicResponse<SimResultView> submit(String userId, Long attemptId, Map<Long, String> answers) {
        ExamSimAttemptEntity attempt = attemptId == null ? null : attemptRepository.findById(attemptId).orElse(null);
        String error = checkOwnedAndOpen(attempt, userId);
        if (error != null) return new BasicResponse<>(false, error);
        LocalDateTime now = LocalDateTime.now();
        if (isPastGrace(attempt, now)) {
            BasicResponse<SimResultView> late = new BasicResponse<>(false,
                    "ההגשה נדחתה כי הזמן הסתיים. הסימולציה נבדקה לפי התשובות שנשמרו עד תום הזמן");
            late.setData(finalizeAttempt(attempt, true, now));
            return late;
        }
        mergeAnswers(attempt, answers);
        return ok(finalizeAttempt(attempt, false, now), "הסימולציה הוגשה ונבדקה לפי המחוון");
    }

    @Transactional
    public BasicResponse<SimResultView> result(String userId, Long attemptId) {
        ExamSimAttemptEntity attempt = attemptId == null ? null : attemptRepository.findById(attemptId).orElse(null);
        if (attempt == null || !attempt.getStudentUserId().equals(userId)) {
            return new BasicResponse<>(false, "הסימולציה לא נמצאה");
        }
        LocalDateTime now = LocalDateTime.now();
        if (attempt.getStatus() == ExamAttemptStatus.IN_PROGRESS) {
            if (!isPastGrace(attempt, now)) return new BasicResponse<>(false, "הסימולציה עדיין פתוחה");
            return ok(finalizeAttempt(attempt, true, now), null);
        }
        return ok(grade(attempt), null);
    }

    // ---- internals ----

    /**
     * An attempt whose time ran out: graded from its saved answers, or deleted when the student never
     * answered anything (an abandoned simulation would otherwise count as a 0). Returns whether it was kept.
     */
    private boolean expire(ExamSimAttemptEntity attempt, LocalDateTime now) {
        boolean answered = ExamQuestionCodec.readAnswers(attempt.getSavedAnswersJson()).values().stream()
                .anyMatch(v -> v != null && !v.isBlank());
        if (!answered) {
            attemptRepository.delete(attempt);
            return false;
        }
        finalizeAttempt(attempt, true, now);
        return true;
    }

    ScorePredictionDTO prediction(String userId, PastExamEntity exam) {
        Map<String, Double> ratings = new HashMap<>();
        for (TopicMasteryEntity m : topicMasteryRepository.findByStudentUserId(userId)) {
            ratings.put(m.getSubject() + "|" + m.getSubTopic(), m.getRating());
        }
        List<ScorePredictor.Section> sections = new ArrayList<>();
        for (PastExamSectionEntity s : exam.getSections()) {
            List<ScorePredictor.Question> qs = s.getQuestions().stream()
                    .map(q -> new ScorePredictor.Question(q.getPoints(), q.getLevel(),
                            ratings.get(subjectOf(exam) + "|" + SkillCatalog.canonicalSubTopic(q.getSubTopic()))))
                    .toList();
            sections.add(new ScorePredictor.Section(qs, s.getQuestionsToCount()));
        }
        List<Integer> past = attemptRepository.findByStudent(userId).stream()
                .map(ExamSimAttemptEntity::getScore).filter(Objects::nonNull).limit(10).toList();
        ScorePredictor.Prediction p = ScorePredictor.predict(sections, past);

        String message = "לפי הביצועים שלך, הציון הצפוי הוא " + p.expected() + " ± " + p.margin();
        StringBuilder basis = new StringBuilder("החישוב משלב את רמת השליטה שלך בנושאי הבחינה (")
                .append(p.coverage()).append("% מהחומר תורגל)");
        if (!past.isEmpty()) basis.append(" עם ").append(past.size()).append(" סימולציות קודמות");
        basis.append(". ככל שתתרגלו יותר, הטווח יצטמצם");
        return new ScorePredictionDTO(exam.getId(), p.expected(), p.margin(), Math.max(0, p.expected() - p.margin()),
                Math.min(100, p.expected() + p.margin()), p.coverage(), past.size(), message, basis.toString());
    }

    private SimAttemptView view(ExamSimAttemptEntity attempt, LocalDateTime now) {
        PastExamEntity exam = attempt.getExam();
        List<SimSectionView> sections = exam.getSections().stream()
                .map(s -> new SimSectionView(s.getTitle(), s.getInstructions(), s.getQuestionsToCount(),
                        s.getQuestions().stream().map(questionMapper).toList()))
                .toList();
        return new SimAttemptView(attempt.getId(), exam.getId(), exam.getTitle(), exam.getDurationMinutes(),
                secondsLeft(attempt, now), format(attempt.getDeadline()), sections,
                ExamQuestionCodec.readAnswers(attempt.getSavedAnswersJson()),
                attempt.getPredictedScore(), attempt.getPredictedMargin());
    }

    private void mergeAnswers(ExamSimAttemptEntity attempt, Map<Long, String> answers) {
        if (answers == null || answers.isEmpty()) return;
        Set<Long> ids = new HashSet<>();
        attempt.getExam().getSections().forEach(s -> s.getQuestions().forEach(q -> ids.add(q.getId())));
        Map<Long, String> saved = ExamQuestionCodec.readAnswers(attempt.getSavedAnswersJson());
        answers.forEach((id, value) -> {
            if (ids.contains(id)) saved.put(id, value == null ? "" : value.length() > 500 ? value.substring(0, 500) : value);
        });
        attempt.setSavedAnswersJson(ExamQuestionCodec.write(saved));
    }

    private SimResultView finalizeAttempt(ExamSimAttemptEntity attempt, boolean auto, LocalDateTime now) {
        attempt.setStatus(auto ? ExamAttemptStatus.AUTO_SUBMITTED : ExamAttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(now);
        SimResultView result = grade(attempt);
        attempt.setScore(result.score());
        attemptRepository.save(attempt);
        recordMastery(attempt);
        return result;
    }

    /** Grades the saved answers by the rubric. Deterministic, so a stored attempt is re-graded for display. */
    SimResultView grade(ExamSimAttemptEntity attempt) {
        PastExamEntity exam = attempt.getExam();
        Map<Long, String> answers = ExamQuestionCodec.readAnswers(attempt.getSavedAnswersJson());
        double earnedTotal = 0;
        double possibleTotal = 0;
        List<SimSectionResult> sections = new ArrayList<>();
        for (PastExamSectionEntity s : exam.getSections()) {
            List<PastExamQuestionEntity> qs = s.getQuestions();
            List<Double> earned = qs.stream()
                    .map(q -> ExamSimGrader.points(q.getAnswer(), answers.get(q.getId()), q.getPoints())).toList();
            Set<Integer> counted = ExamSimGrader.countedIndexes(earned, s.getQuestionsToCount());
            double sectionEarned = 0;
            List<SimQuestionResult> results = new ArrayList<>();
            for (int i = 0; i < qs.size(); i++) {
                PastExamQuestionEntity q = qs.get(i);
                double e = earned.get(i);
                if (counted.contains(i)) sectionEarned += e;
                String status = e >= q.getPoints() ? "CORRECT" : e > 0 ? "PARTIAL" : "WRONG";
                results.add(new SimQuestionResult(q.getId(), q.getPosition(), q.getText(), answers.get(q.getId()),
                        q.getAnswer(), e, q.getPoints(), q.getRubric(), status, counted.contains(i)));
            }
            double possible = ExamSimGrader.possiblePoints(qs.stream().map(PastExamQuestionEntity::getPoints).toList(),
                    s.getQuestionsToCount());
            earnedTotal += sectionEarned;
            possibleTotal += possible;
            sections.add(new SimSectionResult(s.getTitle(), s.getQuestionsToCount(),
                    Math.round(sectionEarned * 10) / 10.0, possible, results));
        }
        int score = possibleTotal <= 0 ? 0 : (int) Math.round(100 * earnedTotal / possibleTotal);
        return new SimResultView(attempt.getId(), exam.getId(), exam.getTitle(), score,
                attempt.getStatus() == ExamAttemptStatus.AUTO_SUBMITTED, format(attempt.getSubmittedAt()),
                attempt.getPredictedScore(), attempt.getPredictedMargin(), sections);
    }

    /** Answered questions move the student's mastery; a failure here never fails the submission. */
    private void recordMastery(ExamSimAttemptEntity attempt) {
        try {
            Map<Long, String> answers = ExamQuestionCodec.readAnswers(attempt.getSavedAnswersJson());
            String subject = subjectOf(attempt.getExam());
            for (PastExamSectionEntity s : attempt.getExam().getSections()) {
                for (PastExamQuestionEntity q : s.getQuestions()) {
                    String answer = answers.get(q.getId());
                    if (q.getSubTopic() == null || answer == null || answer.isBlank()) continue;
                    double outcome = ExamSimGrader.fraction(q.getAnswer(), answer);
                    masteryService.recordAnswer(attempt.getStudentUserId(), subject, q.getTopic(), q.getSubTopic(),
                            q.getLevel(), outcome, outcome >= 1);
                }
            }
        } catch (RuntimeException e) {
            log.warn("Could not record mastery for simulation {}: {}", attempt.getId(), e.getMessage());
        }
    }

    private static String subjectOf(PastExamEntity exam) {
        return exam.getSubject() == null ? SkillCatalog.MATH : exam.getSubject();
    }

    private static String checkOwnedAndOpen(ExamSimAttemptEntity attempt, String userId) {
        if (attempt == null || !attempt.getStudentUserId().equals(userId)) return "הסימולציה לא נמצאה";
        if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) return "הסימולציה כבר הוגשה";
        return null;
    }

    private static boolean isPastGrace(ExamSimAttemptEntity attempt, LocalDateTime now) {
        return now.isAfter(attempt.getDeadline().plus(ExamAttemptService.SUBMIT_GRACE));
    }

    private static long secondsLeft(ExamSimAttemptEntity attempt, LocalDateTime now) {
        return Math.max(0, Duration.between(now, attempt.getDeadline()).getSeconds());
    }

    private static String format(LocalDateTime t) {
        return t == null ? null : t.format(STAMP);
    }

    private static <T> BasicResponse<T> ok(T data, String message) {
        BasicResponse<T> response = new BasicResponse<>(true, message);
        response.setData(data);
        return response;
    }
}
