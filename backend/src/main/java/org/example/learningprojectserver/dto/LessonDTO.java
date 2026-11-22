package org.example.learningprojectserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
public class LessonDTO {
    private static final String TIME_PATTERN = "^([01][0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9])?$";
    @NotBlank(message = "יש לבחור מקצוע")
    private String subject;
    @NotNull(message = "יש לבחור יום בשבוע")
    private DayOfWeek dayOfWeek;
    @NotBlank(message = "יש להזין שעת התחלה")
    @Pattern(regexp = TIME_PATTERN, message = "שעת ההתחלה צריכה להיות בפורמט HH:mm")
    private String startTime;
    @NotBlank(message = "יש להזין שעת סיום")
    @Pattern(regexp = TIME_PATTERN, message = "שעת הסיום צריכה להיות בפורמט HH:mm")
    private String endTime;
    @NotBlank(message = "יש לבחור כיתה")
    private String classRoomName;
    private String teacherName;

    public LessonDTO(String subject, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, String classRoomName, String teacherName) {
        this.subject = subject;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        this.endTime = endTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        this.classRoomName = classRoomName;
        this.teacherName = teacherName;
    }

}
