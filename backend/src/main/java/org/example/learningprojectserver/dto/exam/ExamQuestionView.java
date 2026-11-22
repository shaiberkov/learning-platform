package org.example.learningprojectserver.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A question as the student sees it during an attempt. Carries no answer key.
 * Option ids are the original indexes; the lists are already in this attempt's shuffled order.
 */
@Data
@NoArgsConstructor
public class ExamQuestionView {
    private Long id;
    private String kind;
    private String text;
    private int points;
    private List<Option> options;
    private List<Option> matchLeft;
    private List<Option> matchRight;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private int id;
        private String text;
    }
}
