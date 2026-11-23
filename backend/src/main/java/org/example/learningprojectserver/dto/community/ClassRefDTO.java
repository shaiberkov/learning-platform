package org.example.learningprojectserver.dto.community;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A class the caller belongs to (student) or teaches (teacher). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassRefDTO {
    private Long id;
    private String name;
    private String schoolCode;
}
