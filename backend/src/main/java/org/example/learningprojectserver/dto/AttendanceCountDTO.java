package org.example.learningprojectserver.dto;

import org.example.learningprojectserver.enums.AttendanceStatus;

/** Number of records per student and status, for the absence report. */
public record AttendanceCountDTO(String studentUserId, String studentName, String classRoomName,
                                 AttendanceStatus status, Long count) {}
