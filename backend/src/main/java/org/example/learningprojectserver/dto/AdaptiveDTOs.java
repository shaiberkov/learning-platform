package org.example.learningprojectserver.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** API shapes of the adaptive-learning features: mistakes notebook, mastery, hints, exam feedback. */
public final class AdaptiveDTOs {

    private AdaptiveDTOs() {}

    /* ---------- Mistakes notebook (SM-2) ---------- */

    /** A due notebook item. The answer is not sent. */
    public record ReviewItemDTO(Long id, String subject, String topic, String subTopic, String questionText,
                                String source, int reviewCount, int lapses, LocalDate dueDate) {}

    public record ReviewSummaryDTO(long dueToday, long dueTomorrow, long total, long learned) {}

    public record ReviewAnswerDTO(boolean correct, String correctAnswer, LocalDate nextDueDate,
                                  int intervalDays, long remainingDue) {}

    /* ---------- Mastery and skill map (Elo) ---------- */

    /** color is RED, YELLOW, GREEN, or NONE when the student never answered in this sub-topic. */
    public record SkillNodeDTO(String subTopic, Integer rating, int percent, String color,
                               int attempts, int correctCount, int nextLevel) {}

    public record SkillTopicDTO(String subject, String topic, Integer averageRating, int percent, String color,
                                List<SkillNodeDTO> subTopics) {}

    public record MasteryUpdateDTO(int rating, int percent, String color, int nextLevel) {}

    public record TeacherClassDTO(Long id, String name, String gradeName) {}

    public record ClassSkillCellDTO(Integer rating, String color, int attempts) {}

    public record ClassStudentRowDTO(String userId, String name, Map<String, ClassSkillCellDTO> cells) {}

    public record ClassSubTopicDTO(String topic, String subTopic, Integer averageRating, String color, int studentsPracticed) {}

    public record ClassSkillMapDTO(Long classRoomId, String className, List<ClassSubTopicDTO> subTopics,
                                   List<ClassStudentRowDTO> students) {}

    /* ---------- Hints and guided solution ---------- */

    /** step 1 and 2 carry {@code text}; step 3 carries {@code solutionSteps} and {@code answer}. */
    public record HintStepDTO(int step, String text, List<String> solutionSteps, String answer,
                              int hintsUsed, int pointsAvailable) {}

    public record HintTemplateDTO(String subject, String topic, String subTopic, String hint1, String hint2,
                                  List<String> solutionSteps, boolean custom, String updatedAt) {}

    public record HintTemplateRequest(String subject, String subTopic, String hint1, String hint2,
                                      List<String> solutionSteps) {}

    public record HintCatalogSubTopicDTO(String subTopic, boolean custom) {}

    public record HintCatalogTopicDTO(String topic, List<HintCatalogSubTopicDTO> subTopics) {}

    /* ---------- Post-exam feedback ---------- */

    /** classAveragePercent is null when fewer than the minimum classmates answered this sub-topic. */
    public record TopicBreakdownDTO(String topic, String subTopic, int correct, int total, int percent,
                                    Integer averageSeconds, Integer classAveragePercent) {}

    public record QuestionFeedbackDTO(Long questionId, String subTopic, String questionText, String givenAnswer,
                                      String correctAnswer, boolean correct, Integer secondsSpent) {}

    public record RecommendationDTO(String subject, String topic, String subTopic, String message) {}

    public record ExamFeedbackDTO(Long testId, String source, int score, int correct, int total,
                                  Integer totalSeconds, Integer classAverageScore,
                                  List<TopicBreakdownDTO> topics, List<QuestionFeedbackDTO> questions,
                                  RecommendationDTO recommendation) {}
}
