package org.example.learningprojectserver.dto.exam;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Body of the create-exam endpoint. Times use the app's "dd/MM/yyyy HH:mm" format.
 * publishAt empty = publish now. template = save for cloning, not sent to students.
 * versions = split the students into versions A and B with different question order.
 */
@Data
@NoArgsConstructor
public class CreateExamRequest {
    private String title;
    private String subject;
    private String topic;
    private String startTime;
    private int timeLimitMinutes;
    private String publishAt;
    private List<String> studentIds = new ArrayList<>();
    private List<ExamQuestionInput> questions = new ArrayList<>();
    private boolean template;
    private boolean versions;
    private Boolean shuffleQuestions;
    private Boolean shuffleOptions;
}
