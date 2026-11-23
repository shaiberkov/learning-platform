package org.example.learningprojectserver.dto;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** A timetable lesson of the teacher on a given date, with how many students are already marked. */
public record AttendanceLessonDTO(Long lessonId, String subject, String startTime, String endTime,
                                  Long classRoomId, String className, int studentCount, long markedCount) {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    /** Used by the JPQL constructor expression. */
    public AttendanceLessonDTO(Long lessonId, String subject, LocalTime startTime, LocalTime endTime,
                               Long classRoomId, String className, Integer studentCount) {
        this(lessonId, subject, startTime == null ? null : startTime.format(HH_MM),
                endTime == null ? null : endTime.format(HH_MM), classRoomId, className,
                studentCount == null ? 0 : studentCount, 0);
    }

    public AttendanceLessonDTO withMarkedCount(long marked) {
        return new AttendanceLessonDTO(lessonId, subject, startTime, endTime, classRoomId, className, studentCount, marked);
    }
}
