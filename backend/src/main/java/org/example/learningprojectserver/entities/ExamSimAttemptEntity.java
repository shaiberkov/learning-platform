package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.learningprojectserver.enums.ExamAttemptStatus;

import java.time.LocalDateTime;

/**
 * A student's sitting of a past exam simulation. The server owns the clock (deadline set at start);
 * an attempt left open past its deadline is graded from its saved answers the next time it is read.
 * The prediction shown before the start is kept so the result screen can compare.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "exam_sim_attempt", indexes = {
        @Index(name = "idx_exam_sim_attempt_student", columnList = "student_user_id")
})
public class ExamSimAttemptEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private PastExamEntity exam;

    @Column(name = "student_user_id", nullable = false)
    private String studentUserId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamAttemptStatus status;

    @Column(name = "saved_answers_json", columnDefinition = "TEXT")
    private String savedAnswersJson;

    private Integer score;

    @Column(name = "predicted_score")
    private Integer predictedScore;

    @Column(name = "predicted_margin")
    private Integer predictedMargin;
}
