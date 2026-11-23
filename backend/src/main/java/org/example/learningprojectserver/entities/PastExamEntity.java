package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * An exam in the past-exams bank, sat as a full simulation: real duration, the same sections and a
 * point rubric per question. The sample exams are generated (PastExamSeeder), not copied from real exams.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "past_exam")
public class PastExamEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String subject;

    @Column(name = "exam_year")
    private Integer examYear;

    @Column(name = "session_label", length = 64)
    private String sessionLabel;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PastExamSectionEntity> sections = new ArrayList<>();
}
