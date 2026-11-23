package org.example.learningprojectserver.dto.exam;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One authored question as the teacher's exam builder sends it.
 * Which fields are used depends on {@code kind}:
 * MULTI_SELECT: options + correctOptions (indexes into options);
 * NUMERIC: numericAnswer + tolerance;
 * MATCHING: matchLeft + matchRight + matchKey (for each left index, the matching right index);
 * ORDERING: options in the correct order;
 * SHORT_OPEN: acceptedAnswers (text or LaTeX).
 */
@Data
@NoArgsConstructor
public class ExamQuestionInput {
    private String kind;
    private String text;
    private Integer points;

    private List<String> options;
    private List<Integer> correctOptions;

    private Double numericAnswer;
    private Double tolerance;

    private List<String> matchLeft;
    private List<String> matchRight;
    private List<Integer> matchKey;

    private List<String> acceptedAnswers;
}
