package org.example.learningprojectserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SchoolAnalyticsDTO {
    private String schoolName;
    private int resultsCount;
    private double overallAverage;
    private List<AnalyticsBucketDTO> byGrade = new ArrayList<>();
    private List<AnalyticsBucketDTO> bySubject = new ArrayList<>();
    private List<AnalyticsBucketDTO> byTeacher = new ArrayList<>();
    /** Monthly averages, label "yyyy-MM", oldest first. */
    private List<AnalyticsBucketDTO> trend = new ArrayList<>();
    private List<ClassComparisonDTO> classComparison = new ArrayList<>();
}
