package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A student's weekly reflection ([s10]), written on the Sunday that opens the week: what went well
 * last week, where they got stuck, and a goal for the week that starts on weekStart.
 * Visible only to the student.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "student_reflection",
        uniqueConstraints = @UniqueConstraint(name = "uk_reflection_student_week", columnNames = {"studentUserId", "weekStart"}))
public class StudentReflectionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    /** The Sunday (Israel time) that opens the week the goal is for. */
    @Column(nullable = false)
    private LocalDate weekStart;

    @Column(columnDefinition = "TEXT")
    private String wentWell;

    @Column(columnDefinition = "TEXT")
    private String stuckOn;

    @Column(columnDefinition = "TEXT")
    private String nextGoal;

    /** FREE, PRACTICE_QUESTIONS, XP or ACTIVE_DAYS (WellbeingPolicy.GoalType). */
    @Column(nullable = false, length = 30)
    private String goalType;

    /** The number to reach for a measurable goal; null for FREE. */
    private Integer goalTarget;

    /** Practice answers counted during the week since the goal was set (PRACTICE_QUESTIONS only). */
    private int goalProgress;

    /** The student's own mark; null until they mark it. */
    private Boolean achieved;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
