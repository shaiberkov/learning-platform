package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A teacher's own "why does it matter to me?" text for one practice topic, shared by the whole
 * school. Topics nobody edited fall back to the built-in texts in RealWorldCatalog.
 */
@Entity
@Table(name = "real_world_override",
        uniqueConstraints = @UniqueConstraint(name = "uk_real_world_override_school_topic",
                columnNames = {"school_code", "topic"}))
@Getter
@Setter
@NoArgsConstructor
public class RealWorldOverrideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_code", nullable = false, length = 64)
    private String schoolCode;

    @Column(nullable = false, length = 120)
    private String topic;

    @Column(name = "everyday_example", nullable = false, length = 1000)
    private String everydayExample;

    @Column(name = "career_link", nullable = false, length = 1000)
    private String careerLink;

    @Column(name = "updated_by_teacher_id", length = 255)
    private String updatedByTeacherId;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
