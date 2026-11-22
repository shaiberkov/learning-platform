package org.example.learningprojectserver.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.AdaptiveDTOs.*;
import org.example.learningprojectserver.entities.TeacherTestEntity;
import org.example.learningprojectserver.entities.TestAnswerRecordEntity;
import org.example.learningprojectserver.entities.TestEntity;
import org.example.learningprojectserver.entities.TestQuestionEntity;
import org.example.learningprojectserver.repository.TestAnswerRecordRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.adaptive.PracticeScoring;
import org.example.learningprojectserver.service.adaptive.SkillCatalog;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Records every answer of a submitted exam and builds the post-exam feedback: breakdown by
 * topic, time per question, an anonymous comparison with the class and a practice
 * recommendation. Recording an exam also feeds the mistakes notebook and the Elo mastery.
 */
@Service
@RequiredArgsConstructor
public class ExamFeedbackService {

    public static final String PRACTICE_TEST = "PRACTICE_TEST";
    public static final String TEACHER_TEST = "TEACHER_TEST";

    /** Class averages are shown only when at least this many other students contributed, so no one can be singled out. */
    static final int MIN_CLASSMATES_FOR_COMPARISON = 3;
    /** A sub-topic below this success rate is worth practising. */
    static final int RECOMMEND_BELOW_PERCENT = 80;
    private static final int MAX_SECONDS_PER_QUESTION = 6 * 60 * 60;

    private final TestAnswerRecordRepository testAnswerRecordRepository;
    private final ReviewService reviewService;
    private final MasteryService masteryService;

    /**
     * Stores the answers of a checked exam once per student and test, queues the wrong ones in
     * the mistakes notebook and moves the student's mastery in each sub-topic.
     */
    public void recordExamAnswers(String userId, TestEntity test, String source, Map<Long, String> userAnswers) {
        recordExamAnswers(userId, test, source, userAnswers, (question, given) -> {
            String expected = question.getAnswer() == null ? "" : question.getAnswer().trim();
            return given != null && given.equalsIgnoreCase(expected);
        });
    }

    /**
     * Same, with the exam's own grader (teacher exams have several question kinds). Runs in its own
     * transaction so a failure here cannot roll back the exam submission that called it.
     */
    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void recordExamAnswers(String userId, TestEntity test, String source, Map<Long, String> userAnswers,
                                  BiPredicate<TestQuestionEntity, String> isCorrect) {
        if (test == null || testAnswerRecordRepository.existsByTestIdAndStudentUserId(test.getId(), userId)) {
            return;
        }
        Long classRoomId = testAnswerRecordRepository.findClassRoomIdOfStudent(userId);
        LocalDateTime now = LocalDateTime.now();
        List<TestAnswerRecordEntity> records = new ArrayList<>();
        for (TestQuestionEntity question : test.getQuestions()) {
            String given = userAnswers == null ? null : userAnswers.get(question.getId());
            String expected = question.getAnswer() == null ? "" : question.getAnswer().trim();
            boolean correct = isCorrect.test(question, given);

            TestAnswerRecordEntity r = new TestAnswerRecordEntity();
            r.setTestId(test.getId());
            r.setStudentUserId(userId);
            r.setClassRoomId(classRoomId);
            r.setQuestionId(question.getId());
            r.setSource(source);
            if (test instanceof TeacherTestEntity teacherTest && teacherTest.getTeacher() != null) {
                r.setTeacherUserId(teacherTest.getTeacher().getUserId());
            }
            r.setSubject(question.getSubject());
            String subTopic = SkillCatalog.canonicalSubTopic(question.getSubTopic());
            r.setSubTopic(subTopic);
            r.setTopic(SkillCatalog.topicOf(subTopic, question.getTopic()));
            r.setQuestionText(question.getQuestionText());
            r.setGivenAnswer(given == null || given.length() <= 500 ? given : given.substring(0, 500));
            r.setCorrectAnswer(expected);
            r.setCorrect(correct);
            r.setCreatedAt(now);
            records.add(r);

            masteryService.recordAnswer(userId, question.getSubject(), question.getTopic(), question.getSubTopic(),
                    question.getDifficultyLevel(), PracticeScoring.eloOutcome(correct, 0), correct);
            if (!correct) {
                reviewService.addMistake(userId, question, source);
            }
        }
        testAnswerRecordRepository.saveAll(records);
    }

    /** Stores the seconds the student spent per question; only fills questions that have no time yet. */
    @Transactional
    public BasicResponse recordQuestionTimes(String userId, Long testId, Map<Long, Integer> secondsByQuestion) {
        List<TestAnswerRecordEntity> records = testAnswerRecordRepository.findByTestIdAndStudentUserIdOrderByIdAsc(testId, userId);
        if (records.isEmpty()) {
            return new BasicResponse(false, "לא נמצאו תשובות למבחן הזה");
        }
        if (secondsByQuestion != null) {
            for (TestAnswerRecordEntity r : records) {
                Integer seconds = secondsByQuestion.get(r.getQuestionId());
                if (seconds != null && r.getSecondsSpent() == null) {
                    r.setSecondsSpent(Math.max(0, Math.min(MAX_SECONDS_PER_QUESTION, seconds)));
                }
            }
            testAnswerRecordRepository.saveAll(records);
        }
        return new BasicResponse(true, null);
    }

    public BasicResponse getFeedback(String userId, Long testId) {
        List<TestAnswerRecordEntity> records = testAnswerRecordRepository.findByTestIdAndStudentUserIdOrderByIdAsc(testId, userId);
        if (records.isEmpty()) {
            return new BasicResponse(false, "לא נמצאו תשובות למבחן הזה");
        }
        int total = records.size();
        int correct = (int) records.stream().filter(TestAnswerRecordEntity::isCorrect).count();
        int score = (int) (((double) correct / total) * 100);
        String source = records.get(0).getSource();
        Long classRoomId = records.get(0).getClassRoomId();

        boolean anyTime = records.stream().anyMatch(r -> r.getSecondsSpent() != null);
        Integer totalSeconds = anyTime
                ? records.stream().map(TestAnswerRecordEntity::getSecondsSpent).filter(Objects::nonNull).mapToInt(Integer::intValue).sum()
                : null;

        // Group by sub-topic, keeping exam order.
        Map<String, List<TestAnswerRecordEntity>> bySubTopic = records.stream()
                .collect(Collectors.groupingBy(TestAnswerRecordEntity::getSubTopic, LinkedHashMap::new, Collectors.toList()));
        Map<String, Integer> classPercent = classAccuracy(classRoomId, userId, bySubTopic.keySet());

        List<TopicBreakdownDTO> topics = new ArrayList<>();
        for (Map.Entry<String, List<TestAnswerRecordEntity>> e : bySubTopic.entrySet()) {
            List<TestAnswerRecordEntity> rs = e.getValue();
            int c = (int) rs.stream().filter(TestAnswerRecordEntity::isCorrect).count();
            OptionalDouble avgSeconds = rs.stream().map(TestAnswerRecordEntity::getSecondsSpent)
                    .filter(Objects::nonNull).mapToInt(Integer::intValue).average();
            topics.add(new TopicBreakdownDTO(rs.get(0).getTopic(), e.getKey(), c, rs.size(),
                    (int) Math.round(100.0 * c / rs.size()),
                    avgSeconds.isPresent() ? (int) Math.round(avgSeconds.getAsDouble()) : null,
                    classPercent.get(e.getKey())));
        }

        List<QuestionFeedbackDTO> questions = records.stream()
                .map(r -> new QuestionFeedbackDTO(r.getQuestionId(), r.getSubTopic(), r.getQuestionText(),
                        r.getGivenAnswer(), r.getCorrectAnswer(), r.isCorrect(), r.getSecondsSpent()))
                .toList();

        Integer classAverageScore = null;
        if (TEACHER_TEST.equals(source)) {
            List<Object[]> stats = testAnswerRecordRepository.teacherTestScoreStats(testId);
            if (!stats.isEmpty() && stats.get(0)[0] != null) {
                long count = ((Number) stats.get(0)[1]).longValue();
                // The count includes the student; require enough others to keep the average anonymous.
                if (count - 1 >= MIN_CLASSMATES_FOR_COMPARISON) {
                    classAverageScore = (int) Math.round(((Number) stats.get(0)[0]).doubleValue());
                }
            }
        }

        BasicResponse response = new BasicResponse(true, null);
        response.setData(new ExamFeedbackDTO(testId, source, score, correct, total, totalSeconds, classAverageScore,
                topics, questions, recommend(records.get(0).getSubject(), topics)));
        return response;
    }

    private Map<String, Integer> classAccuracy(Long classRoomId, String userId, Collection<String> subTopics) {
        Map<String, Integer> result = new HashMap<>();
        if (classRoomId == null || subTopics.isEmpty()) {
            return result;
        }
        for (Object[] row : testAnswerRecordRepository.classAccuracyBySubTopic(classRoomId, userId, subTopics)) {
            long correct = ((Number) row[1]).longValue();
            long total = ((Number) row[2]).longValue();
            long students = ((Number) row[3]).longValue();
            if (total > 0 && students >= MIN_CLASSMATES_FOR_COMPARISON) {
                result.put((String) row[0], (int) Math.round(100.0 * correct / total));
            }
        }
        return result;
    }

    /** The weakest sub-topic of the exam, or encouragement when everything went well. */
    static RecommendationDTO recommend(String subject, List<TopicBreakdownDTO> topics) {
        Optional<TopicBreakdownDTO> weakest = topics.stream()
                .filter(t -> t.percent() < RECOMMEND_BELOW_PERCENT)
                .min(Comparator.comparingInt(TopicBreakdownDTO::percent)
                        .thenComparing(t -> -t.total()));
        if (weakest.isPresent()) {
            TopicBreakdownDTO t = weakest.get();
            String message = "מומלץ לתרגל את \"" + t.subTopic() + "\": ענית נכון על " + t.correct() + " מתוך "
                    + t.total() + " שאלות בנושא הזה.";
            return new RecommendationDTO(subject, t.topic(), t.subTopic(), message);
        }
        TopicBreakdownDTO first = topics.get(0);
        return new RecommendationDTO(subject, first.topic(), first.subTopic(),
                "כל הכבוד! שלטת בכל הנושאים במבחן. אפשר להמשיך לתרגל את \"" + first.subTopic()
                        + "\" ברמה גבוהה יותר.");
    }
}
