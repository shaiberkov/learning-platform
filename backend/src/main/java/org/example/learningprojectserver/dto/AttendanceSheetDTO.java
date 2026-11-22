package org.example.learningprojectserver.dto;

import java.util.List;

/** The students of a lesson's class and their status on one date (status null = not marked yet). */
public record AttendanceSheetDTO(Long lessonId, String date, String subject, String className,
                                 String startTime, String endTime, List<Row> students) {
    public record Row(String studentId, String name, String status) {}
}
