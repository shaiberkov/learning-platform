package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One day's study task for one student ahead of one teacher test. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "study_plan_task", indexes = {
        @Index(name = "idx_study_plan_student_test", columnList = "studentUserId,testId")
})
public class StudyPlanTaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    @Column(nullable = false)
    private Long testId;

    @Column(nullable = false)
    private LocalDate taskDate;

    private String subject;
    private String topic;
    private int minutes;

    /** LEARN, PRACTICE or REVIEW. */
    private String taskType;

    private String description;

    private boolean completed;
    private LocalDateTime completedAt;
}
