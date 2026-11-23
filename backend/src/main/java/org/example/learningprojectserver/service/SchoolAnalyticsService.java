package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.AnalyticsBucketDTO;
import org.example.learningprojectserver.dto.ClassComparisonDTO;
import org.example.learningprojectserver.dto.GradeRecordDTO;
import org.example.learningprojectserver.dto.SchoolAnalyticsDTO;
import org.example.learningprojectserver.entities.SchoolEntity;
import org.example.learningprojectserver.repository.SchoolAnalyticsRepository;
import org.example.learningprojectserver.repository.SchoolRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.utils.GradeLevels;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** School dashboard numbers: averages of graded teacher tests by grade, subject, teacher, month and class. */
@Service
@RequiredArgsConstructor
public class SchoolAnalyticsService {

    static final String NO_VALUE = "ללא";
    private static final DateTimeFormatter TEST_START_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final SchoolRepository schoolRepository;
    private final SchoolAnalyticsRepository schoolAnalyticsRepository;

    public BasicResponse getSchoolAnalytics(String schoolCode) {
        SchoolEntity school = schoolRepository.findBySchoolCode(schoolCode);
        if (school == null) {
            return new BasicResponse(false, "בית הספר לא נמצא");
        }
        List<GradeRecordDTO> records = schoolAnalyticsRepository.findGradeRecordsBySchoolCode(schoolCode);
        SchoolAnalyticsDTO analytics = buildAnalytics(records);
        analytics.setSchoolName(school.getSchoolName());

        BasicResponse<SchoolAnalyticsDTO> response = new BasicResponse<>(true,
                records.isEmpty() ? "עדיין אין ציוני מבחנים בבית הספר" : null);
        response.setData(analytics);
        return response;
    }

    static SchoolAnalyticsDTO buildAnalytics(List<GradeRecordDTO> records) {
        SchoolAnalyticsDTO dto = new SchoolAnalyticsDTO();
        dto.setResultsCount(records.size());
        dto.setOverallAverage(round(records.stream().mapToInt(GradeRecordDTO::getScore).average().orElse(0)));

        dto.setByGrade(sortByGradeLevel(bucket(records, r -> orNone(r.getGradeName()))));
        dto.setBySubject(sortByLabel(bucket(records, r -> orNone(r.getSubject()))));
        dto.setByTeacher(sortByLabel(bucket(records, r -> orNone(r.getTeacherName()))));

        List<GradeRecordDTO> dated = records.stream().filter(r -> monthOf(r) != null).toList();
        dto.setTrend(sortByLabel(bucket(dated, SchoolAnalyticsService::monthOf)));

        Map<String, List<GradeRecordDTO>> byGrade = records.stream()
                .filter(r -> r.getGradeName() != null && r.getClassName() != null)
                .collect(Collectors.groupingBy(GradeRecordDTO::getGradeName));
        List<ClassComparisonDTO> comparison = new ArrayList<>();
        byGrade.forEach((grade, gradeRecords) ->
                comparison.add(new ClassComparisonDTO(grade, sortByLabel(bucket(gradeRecords, GradeRecordDTO::getClassName)))));
        comparison.sort(Comparator.comparingInt((ClassComparisonDTO c) -> GradeLevels.level(c.getGradeName()))
                .thenComparing(ClassComparisonDTO::getGradeName));
        dto.setClassComparison(comparison);
        return dto;
    }

    static List<AnalyticsBucketDTO> bucket(List<GradeRecordDTO> records, Function<GradeRecordDTO, String> key) {
        Map<String, List<GradeRecordDTO>> groups = records.stream().collect(Collectors.groupingBy(key));
        List<AnalyticsBucketDTO> buckets = new ArrayList<>();
        groups.forEach((label, group) -> buckets.add(new AnalyticsBucketDTO(
                label,
                round(group.stream().mapToInt(GradeRecordDTO::getScore).average().orElse(0)),
                group.size())));
        return buckets;
    }

    /** "yyyy-MM" of the test start time, or null when the stored time cannot be read. */
    static String monthOf(GradeRecordDTO record) {
        String start = record.getTestStartTime();
        if (start == null || start.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(start.trim(), TEST_START_FORMAT).format(MONTH_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static List<AnalyticsBucketDTO> sortByLabel(List<AnalyticsBucketDTO> buckets) {
        buckets.sort(Comparator.comparing(AnalyticsBucketDTO::getLabel));
        return buckets;
    }

    private static List<AnalyticsBucketDTO> sortByGradeLevel(List<AnalyticsBucketDTO> buckets) {
        buckets.sort(Comparator.comparingInt((AnalyticsBucketDTO b) -> GradeLevels.level(b.getLabel()))
                .thenComparing(AnalyticsBucketDTO::getLabel));
        return buckets;
    }

    private static String orNone(String value) {
        return value == null || value.isBlank() ? NO_VALUE : value;
    }

    static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
