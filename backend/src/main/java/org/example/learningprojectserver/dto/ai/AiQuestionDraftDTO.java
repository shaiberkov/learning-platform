package org.example.learningprojectserver.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiQuestionDraftDTO {
    private Long id;
    private String subject;
    private String topic;
    private String subTopic;
    private String difficulty;
    private String gradeLevel;
    private String questionText;
    private String answer;
    private String explanation;
    private String status;
    private Long bankQuestionId;
}
