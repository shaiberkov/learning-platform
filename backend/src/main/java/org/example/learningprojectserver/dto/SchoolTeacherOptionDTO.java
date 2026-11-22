package org.example.learningprojectserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** A teacher as the timetable builder needs it: id, name, subjects and assigned classes. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchoolTeacherOptionDTO {
    private String userId;
    private String username;
    private List<String> subjects;
    private List<String> classes;
}
