package org.example.learningprojectserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SemesterDTO {
    private Long id;
    private String name;
    /** yyyy-MM-dd */
    private String startDate;
    private String endDate;
}
