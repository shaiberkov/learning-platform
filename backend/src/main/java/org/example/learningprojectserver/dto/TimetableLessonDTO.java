package org.example.learningprojectserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * A lesson in the timetable builder. Also the request body for check/save:
 * id is null for a new lesson and set when an existing lesson is moved or edited.
 */
@Getter
@Setter
@NoArgsConstructor
public class TimetableLessonDTO {
    private Long id;
    private String subject;
    private DayOfWeek dayOfWeek;
    private String startTime;
    private String endTime;
    private String classRoomName;
    private String teacherId;
    private String teacherName;
    private String room;

    public TimetableLessonDTO(Long id, String subject, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                              String classRoomName, String teacherId, String teacherName, String room) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        this.id = id;
        this.subject = subject;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime == null ? null : startTime.format(fmt);
        this.endTime = endTime == null ? null : endTime.format(fmt);
        this.classRoomName = classRoomName;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.room = room;
    }
}
