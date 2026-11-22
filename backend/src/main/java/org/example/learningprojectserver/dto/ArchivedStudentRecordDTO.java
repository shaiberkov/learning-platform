package org.example.learningprojectserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArchivedStudentRecordDTO {
    private String studentUserId;
    private String studentName;
    private String gradeName;
    private String className;
    private int testsCount;
    private Double averageScore;
    private String outcome;
    private String nextGradeName;
    private String nextClassName;
}
