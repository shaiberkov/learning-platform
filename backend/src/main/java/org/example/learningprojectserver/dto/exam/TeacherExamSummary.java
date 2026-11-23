package org.example.learningprojectserver.dto.exam;

import lombok.Data;
import lombok.NoArgsConstructor;

/** One row of the teacher's exam list. */
@Data
@NoArgsConstructor
public class TeacherExamSummary {
    private Long testId;
    private String title;
    private String subject;
    private String topic;
    private String startTime;
    private int timeLimitMinutes;
    private boolean published;
    private String publishAt;
    private boolean template;
    private String versionLabel;
    private String examGroupId;
    private Long sourceTestId;
    private int questionCount;
    private int studentCount;
    private long submittedCount;
}
