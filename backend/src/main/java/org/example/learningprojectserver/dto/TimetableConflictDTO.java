package org.example.learningprojectserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimetableConflictDTO {
    /** TEACHER, CLASS or ROOM */
    private String type;
    private String message;
    private Long conflictingLessonId;
}
