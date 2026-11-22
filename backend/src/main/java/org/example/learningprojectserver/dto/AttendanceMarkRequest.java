package org.example.learningprojectserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One line of an attendance submission. status: PRESENT, ABSENT, LATE or EXCUSED. */
@Getter
@Setter
@NoArgsConstructor
public class AttendanceMarkRequest {
    private String studentId;
    private String status;
}
