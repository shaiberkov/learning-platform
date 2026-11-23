package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** An open question with the teacher's grading rubric. Published rubrics are visible to the school's students. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ai_rubric", indexes = {
        @Index(name = "idx_ai_rubric_teacher", columnList = "teacherUserId"),
        @Index(name = "idx_ai_rubric_school", columnList = "schoolCode,published")
})
public class AiRubricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String teacherUserId;

    private String schoolCode;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;

    /** Criteria and points, in the teacher's words. Never shown to students. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String rubric;

    private int maxScore;

    private boolean published;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
