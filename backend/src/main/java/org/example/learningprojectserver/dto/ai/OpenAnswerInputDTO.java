package org.example.learningprojectserver.dto.ai;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An answer to submit (student), to add by hand (teacher), or a review decision (teacher). */
@Getter
@Setter
@NoArgsConstructor
public class OpenAnswerInputDTO {
    private Long rubricId;
    private String studentName;
    private String answerText;
    private Integer finalScore;
    private String teacherComment;
}
