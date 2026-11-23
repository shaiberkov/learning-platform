package org.example.learningprojectserver.dto.examsim;

import java.util.List;
import java.util.Map;

/** API shapes of the past-exam simulation (מבחני סימולציה) and the score prediction. */
public final class ExamSimDTOs {

    private ExamSimDTOs() {}

    /** One exam of the bank with the caller's own history on it. */
    public record PastExamSummaryDTO(Long id, String title, String subject, Integer examYear, String sessionLabel,
                                     String description, int durationMinutes, int sectionCount, int questionCount,
                                     int attemptsCount, Integer bestScore, Integer lastScore, Long lastAttemptId,
                                     Long inProgressAttemptId) {}

    /** A question as the student sees it during the exam; the answer and rubric are not sent. */
    public record SimQuestionView(Long id, int number, String text, int points, String answerHint, String kind) {}

    public record SimSectionView(String title, String instructions, Integer questionsToCount,
                                 List<SimQuestionView> questions) {}

    public record SimAttemptView(Long attemptId, Long examId, String title, int durationMinutes,
                                 long remainingSeconds, String deadline, List<SimSectionView> sections,
                                 Map<Long, String> savedAnswers, Integer predictedScore, Integer predictedMargin) {}

    /** status: CORRECT, PARTIAL or WRONG. counted is false when a "best N of the section" rule dropped it. */
    public record SimQuestionResult(Long questionId, int number, String text, String yourAnswer,
                                    String expectedAnswer, double pointsEarned, int points, String rubric,
                                    String status, boolean counted) {}

    public record SimSectionResult(String title, Integer questionsToCount, double earned, double possible,
                                   List<SimQuestionResult> questions) {}

    public record SimResultView(Long attemptId, Long examId, String title, int score, boolean autoSubmitted,
                                String submittedAt, Integer predictedScore, Integer predictedMargin,
                                List<SimSectionResult> sections) {}

    /**
     * Expected score and its margin (about one standard deviation), with the Hebrew sentence the screen shows.
     * masteryCoverage: percent of the exam's points whose sub-topic the student has practised.
     */
    public record ScorePredictionDTO(Long examId, int expectedScore, int margin, int low, int high,
                                     int masteryCoverage, int pastAttempts, String message, String basis) {}
}
