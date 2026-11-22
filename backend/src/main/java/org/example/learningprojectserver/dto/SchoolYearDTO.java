package org.example.learningprojectserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SchoolYearDTO {
    private Long id;
    private String name;
    /** yyyy-MM-dd */
    private String startDate;
    private String endDate;
    private boolean active;
    private boolean closed;
    private String closedAt;
    private long archivedStudents;
    private List<SemesterDTO> semesters = new ArrayList<>();
}
