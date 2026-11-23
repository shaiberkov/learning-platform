package org.example.learningprojectserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Parallel classes of one grade level, side by side. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassComparisonDTO {
    private String gradeName;
    private List<AnalyticsBucketDTO> classes;
}
