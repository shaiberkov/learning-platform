package org.example.learningprojectserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class ChildOverviewDTO {
    private ChildSummaryDTO child;
    /** Teacher tests the child took, newest first. */
    private List<GradeRecordDTO> testResults = new ArrayList<>();
    private Double teacherTestsAverage;
    private List<AnalyticsBucketDTO> averageBySubject = new ArrayList<>();
    private long assignedTestsCount;
    private long practiceTestsCount;
    private Double practiceTestsAverage;
    /** Practice skill level per sub-topic, as tracked by the practice engine. */
    private Map<String, Integer> skillLevels;
    /** Attendance is not recorded by the system yet: false until it is, and the UI says so. */
    private boolean attendanceAvailable;
}
