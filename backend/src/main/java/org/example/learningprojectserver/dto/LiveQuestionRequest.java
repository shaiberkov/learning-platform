package org.example.learningprojectserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** A question the teacher puts to the class: from the bank (bankQuestionId) or typed on the spot. */
@Getter
@Setter
@NoArgsConstructor
public class LiveQuestionRequest {
    private Long bankQuestionId;
    private String questionText;
    private List<String> options = new ArrayList<>();
    private String answer;
}
