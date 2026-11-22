package org.example.learningprojectserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One line of the promotion plan: students of fromGrade move to toGrade.
 * toGrade empty or null: the grade graduates (its students leave the school).
 * toGrade equal to fromGrade: the grade stays as it is.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GradePromotionDTO {
    private String fromGrade;
    private String toGrade;
    private int studentCount;
}
