package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A question the AI proposed to a teacher. It waits in the teacher's approval queue
 * (PENDING); approving copies it into the question bank ({@link BankQuestionEntity}).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ai_question_draft", indexes = {
        @Index(name = "idx_ai_draft_teacher_status", columnList = "teacherUserId,status")
})
public class AiQuestionDraftEntity {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String teacherUserId;

    private String schoolCode;

    /** The teacher's request, e.g. "15 שאלות על משוואות ריבועיות, בינוני, כיתה ט". */
    @Column(columnDefinition = "TEXT")
    private String prompt;

    private String subject;
    private String topic;
    private String subTopic;
    private String difficulty;
    private String gradeLevel;

    @Column(columnDefinition = "TEXT")
    private String questionText;

    private String answer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    /** Set when approved. */
    private Long bankQuestionId;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
