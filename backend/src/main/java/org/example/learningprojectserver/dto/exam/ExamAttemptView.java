package org.example.learningprojectserver.dto.exam;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** What the exam page needs to run an attempt. The deadline is the server's. */
@Data
@NoArgsConstructor
public class ExamAttemptView {
    private Long attemptId;
    private Long testId;
    private String title;
    private String subject;
    private String topic;
    private String versionLabel;
    private long remainingSeconds;
    private String deadline;
    private List<ExamQuestionView> questions;
    private Map<Long, String> savedAnswers;
}
