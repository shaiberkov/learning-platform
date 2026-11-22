package org.example.learningprojectserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BankQuestionDTO {
    private Long id;
    private String questionText;
    private String answer;
    private List<String> options = new ArrayList<>();
    private String subject;
    private String topic;
    private String level;
    private String grade;
    private String ownerName;
    private boolean shared;
    /** True when the requesting teacher owns it (may edit, delete, share). */
    private boolean mine;
    private int timesAsked;
    private int attempts;
    private int correctAttempts;
    /** Percent of correct answers, null before any answer. */
    private Double successRate;
}
