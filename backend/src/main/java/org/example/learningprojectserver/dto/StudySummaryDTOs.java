package org.example.learningprojectserver.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Request and response shapes of the study summary endpoints (summary sheet and study podcast). */
public final class StudySummaryDTOs {
    private StudySummaryDTOs() {}

    /** A teacher test the student can prepare for. {@code startTime} is "yyyy-MM-dd HH:mm". */
    public record TestOption(Long testId, String title, String subject, String topic, String startTime) {
    }

    /** What the student can build a sheet or a podcast from. */
    public record Options(List<TestOption> tests, List<String> topics, long mistakeCount, boolean aiAvailable) {
    }

    /**
     * Pick an upcoming test ({@code testId}), topics, or both. {@code basic} builds the version that
     * needs no AI (offered when the AI is not configured).
     */
    public record GenerateRequest(Long testId, List<String> topics, Boolean basic) {
        public boolean isBasic() {
            return Boolean.TRUE.equals(basic);
        }
    }

    public record SheetTopic(String name, String summary) {
    }

    public record Formula(String name, String expression, String note) {
    }

    /** One of the student's own mistakes: the question, the right answer and why. */
    public record Mistake(String topic, String question, String correctAnswer, String explanation) {
    }

    public record SheetContent(String intro,
                               List<SheetTopic> topics,
                               List<Formula> formulas,
                               List<Mistake> mistakes,
                               List<String> tips) {
    }

    public record SheetSummary(Long id, String title, String topics, boolean aiGenerated, LocalDateTime createdAt) {
    }

    public record Sheet(Long id,
                        String title,
                        String topics,
                        Long testId,
                        boolean aiGenerated,
                        LocalDateTime createdAt,
                        SheetContent content) {
    }

    public record PodcastSummary(Long id,
                                 String title,
                                 String topics,
                                 boolean aiGenerated,
                                 boolean hasAudio,
                                 LocalDateTime createdAt) {
    }

    public record Podcast(Long id,
                          String title,
                          String topics,
                          Long testId,
                          boolean aiGenerated,
                          boolean hasAudio,
                          int estimatedMinutes,
                          LocalDateTime createdAt,
                          String script) {
    }
}
