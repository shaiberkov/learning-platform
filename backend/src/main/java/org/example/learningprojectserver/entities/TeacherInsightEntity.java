package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A weekly insight report for one teacher: computed statistics plus an AI-written summary. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "teacher_insight", indexes = {
        @Index(name = "idx_teacher_insight_teacher", columnList = "teacherUserId,createdAt")
})
public class TeacherInsightEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String teacherUserId;

    private String schoolCode;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    /** {@code InsightStatsDTO} as JSON. */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String statsJson;

    @Column(columnDefinition = "TEXT")
    private String summary;

    /** False when the summary could not be written by the AI (no key, quota, error) or there was no data. */
    private boolean aiGenerated;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
