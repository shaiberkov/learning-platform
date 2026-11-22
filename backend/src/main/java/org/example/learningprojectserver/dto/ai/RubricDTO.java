package org.example.learningprojectserver.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An open question. {@code rubric} is null in what students receive. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RubricDTO {
    private Long id;
    private String title;
    private String questionText;
    private String rubric;
    private int maxScore;
    private boolean published;
}
