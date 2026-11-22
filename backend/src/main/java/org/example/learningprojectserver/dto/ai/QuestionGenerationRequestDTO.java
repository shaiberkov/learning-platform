package org.example.learningprojectserver.dto.ai;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A teacher's request: free text plus optional structured hints the UI fills in. */
@Getter
@Setter
@NoArgsConstructor
public class QuestionGenerationRequestDTO {
    private String prompt;
    private String subject;
    private String topic;
    private String difficulty;
    private String gradeLevel;
    private Integer count;
}
