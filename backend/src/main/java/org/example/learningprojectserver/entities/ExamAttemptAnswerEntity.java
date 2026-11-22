package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The graded answer to one question in one attempt; the raw data for item analysis. */
@Entity
@Table(name = "exam_attempt_answer")
@Getter
@Setter
@NoArgsConstructor
public class ExamAttemptAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private ExamAttemptEntity attempt;

    private Long questionId;

    @Column(columnDefinition = "TEXT")
    private String answer;

    private boolean correct;
    private int pointsEarned;
}
