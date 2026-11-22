package org.example.learningprojectserver.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A student answer. Students receive it without the AI fields, and with a score only once approved. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenAnswerDTO {
    private Long id;
    private Long rubricId;
    private String rubricTitle;
    private int maxScore;
    private String studentUserId;
    private String studentName;
    private String answerText;
    private Integer aiScore;
    private String aiReasoning;
    private Integer finalScore;
    private String teacherComment;
    private String status;
    private LocalDateTime submittedAt;
}
