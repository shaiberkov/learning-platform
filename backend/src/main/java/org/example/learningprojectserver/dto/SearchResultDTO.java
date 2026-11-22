package org.example.learningprojectserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One hit of the global search (Ctrl+K). type: STUDENT, TEACHER, CLASS, EXAM or SCHOOL. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO {
    private String type;
    private String key;
    private String title;
    private String subtitle;
}
