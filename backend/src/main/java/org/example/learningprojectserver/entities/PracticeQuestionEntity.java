package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("PRACTICE")
@Getter
@Setter
@NoArgsConstructor
public class PracticeQuestionEntity extends QuestionEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_progress_id")
    private StudentProgressEntity progressEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_history_id")
    private StudentQuestionHistoryEntity questionHistory;

    /** Highest guidance step the student opened: 0 none, 1 hint 1, 2 hint 2, 3 full solution. */
    private Integer hintsUsed;

    /** Practice points earned when answered; null while unanswered. */
    private Integer pointsEarned;

}