package org.example.learningprojectserver.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** The numbers behind a weekly insight. Stored as JSON in {@code teacher_insight.statsJson}. */
@Getter
@Setter
@NoArgsConstructor
public class InsightStatsDTO {

    private int totalAnswers;
    private int studentCount;
    private List<TopicStat> topics = new ArrayList<>();
    private List<StrugglingStudent> strugglingStudents = new ArrayList<>();
    private List<SuspiciousQuestion> suspiciousQuestions = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicStat {
        private String topic;
        private int answered;
        private int accuracyPct;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrugglingStudent {
        private String studentUserId;
        private String studentName;
        /** Pseudonym used in the AI summary ("S1"); the AI never sees names or ids. */
        private String label;
        private String topic;
        private int answered;
        private int accuracyPct;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuspiciousQuestion {
        private Long questionId;
        private String questionText;
        private String topic;
        private String correctAnswer;
        private int answered;
        private String commonWrongAnswer;
        private int commonWrongCount;
        private int commonWrongPct;
    }
}
