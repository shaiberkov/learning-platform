package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.learningprojectserver.enums.ExamAttemptStatus;

import java.time.LocalDateTime;

/**
 * One student's sitting of a teacher exam. The server owns the clock:
 * the deadline is computed at start and every write is checked against it.
 * The seed makes the question and option order deterministic for this attempt.
 */
@Entity
@Table(name = "exam_attempt",
        uniqueConstraints = @UniqueConstraint(columnNames = {"test_id", "student_user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ExamAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id")
    private TeacherTestEntity test;

    @Column(name = "student_user_id")
    private String studentUserId;

    private LocalDateTime startedAt;
    private LocalDateTime deadline;
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    private ExamAttemptStatus status;

    private long seed;

    @Column(columnDefinition = "TEXT")
    private String savedAnswersJson;

    private Integer score;
    private Integer correctCount;
    private int tabLeaveCount;

    @Version
    private Long version;
}
