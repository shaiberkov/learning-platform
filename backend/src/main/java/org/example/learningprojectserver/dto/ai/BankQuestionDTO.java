package org.example.learningprojectserver.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankQuestionDTO {
    private Long id;
    private String subject;
    private String topic;
    private String subTopic;
    private String difficulty;
    private String gradeLevel;
    private String questionText;
    private String answer;
    private String explanation;
    private String source;
    private LocalDateTime createdAt;
}
