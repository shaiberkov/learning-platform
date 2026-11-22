package org.example.learningprojectserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChildSummaryDTO {
    private String userId;
    private String username;
    private String className;
    private String gradeName;
    private String schoolName;
}
