package org.example.learningprojectserver.dto;

import java.util.List;
import java.util.Map;

/** Attendance per student over a date range. scope is SCHOOL (manager) or TEACHER (own lessons only). */
public record AttendanceReportDTO(String from, String to, String scope, List<String> classNames,
                                  List<Row> students, Map<String, Long> totals) {
    public record Row(String studentId, String name, String className, long present, long absent, long late,
                      long excused, long total, double absenceRate, boolean unusual) {}
}
