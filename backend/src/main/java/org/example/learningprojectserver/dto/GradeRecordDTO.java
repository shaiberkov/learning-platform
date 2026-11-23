package org.example.learningprojectserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One graded teacher-test result with the context the school dashboard groups by. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GradeRecordDTO {
    private String studentUserId;
    private String studentName;
    private int score;
    private String subject;
    private String topic;
    /** Test start time as stored on the test, "dd/MM/yyyy HH:mm". */
    private String testStartTime;
    private String teacherUserId;
    private String teacherName;
    private String className;
    private String gradeName;
}
