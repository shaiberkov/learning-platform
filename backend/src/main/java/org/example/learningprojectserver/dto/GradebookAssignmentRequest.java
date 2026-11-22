package org.example.learningprojectserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GradebookAssignmentRequest {
    private Long classRoomId;
    private String title;
    /** GradeCategory name: EXAM, HOMEWORK, QUIZ, PROJECT */
    private String category;
    private Double maxScore;
    /** yyyy-MM-dd, optional */
    private String date;
}
