package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A student answer to an open question. SUBMITTED, then SUGGESTED once the AI proposed a score,
 * then APPROVED once the teacher accepted or changed it. Students see only the approved score.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ai_open_answer", indexes = {
        @Index(name = "idx_ai_answer_rubric", columnList = "rubric_id"),
        @Index(name = "idx_ai_answer_student", columnList = "studentUserId")
})
public class AiOpenAnswerEntity {

    public enum Status { SUBMITTED, SUGGESTED, APPROVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rubric_id")
    private AiRubricEntity rubric;

    /** Null when the teacher typed in an answer written on paper. */
    private String studentUserId;

    private String studentName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answerText;

    private Integer aiScore;

    @Column(columnDefinition = "TEXT")
    private String aiReasoning;

    private Integer finalScore;

    @Column(columnDefinition = "TEXT")
    private String teacherComment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;
}
