package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * [s06] One "explain it to me" (Feynman) attempt: the student's own explanation of an idea, written or
 * recorded, and how well it shows understanding (score 0-100 and the gaps found). Kept as history.
 */
@Entity
@Table(name = "feynman_explanation",
        indexes = @Index(name = "idx_feynman_student_created", columnList = "studentUserId,createdAt"))
@Getter
@Setter
@NoArgsConstructor
public class FeynmanExplanationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    private String schoolCode;

    @Column(nullable = false, length = 80)
    private String promptKey;

    private String topic;

    @Column(length = 500)
    private String promptText;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String explanationText;

    /** TEXT or VOICE. */
    @Column(nullable = false, length = 10)
    private String inputMode;

    private Integer score;

    /** The gaps found, one per line. */
    @Column(columnDefinition = "TEXT")
    private String gaps;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    /** AI, or KEYWORDS when the AI was not available and the key-concept check was used. */
    @Column(nullable = false, length = 16)
    private String evaluatedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
