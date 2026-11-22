package org.example.learningprojectserver.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** The graded attempt. Shows which questions were right, not the answer key. */
@Data
@NoArgsConstructor
public class ExamResultView {
    private int score;
    private int correctCount;
    private int totalQuestions;
    private boolean autoSubmitted;
    private List<QuestionResult> questions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResult {
        private Long questionId;
        private String text;
        private boolean correct;
        private int pointsEarned;
        private int points;
    }
}
