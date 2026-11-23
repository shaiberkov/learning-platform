package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A question a teacher wrote, imported or approved from an AI draft into the question bank.
 * Kept apart from QuestionEntity, which holds the questions the generators create on the fly.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "bank_question", indexes = {
        @Index(name = "idx_bank_question_owner", columnList = "owner_teacher_id"),
        @Index(name = "idx_bank_question_school", columnList = "school_code")
})
public class BankQuestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String questionText;

    @Column(nullable = false, length = 500)
    private String answer;

    /** Multiple-choice options, one per line. Null or empty for an open question. */
    @Column(length = 2000)
    private String options;

    private String subject;
    private String topic;
    private String subTopic;
    private String level;
    private String grade;

    @Column(name = "owner_teacher_id", nullable = false)
    private String ownerTeacherId;
    private String ownerName;

    @Column(name = "school_code", nullable = false)
    private String schoolCode;

    /** Visible to the other teachers of the same school. */
    private boolean shared;

    /** How many times it was put to a class (live quiz). */
    private int timesAsked;
    /** Student answers recorded for it, and how many of them were correct. */
    private int attempts;
    private int correctAttempts;

    /** Worked explanation (AI-generated questions carry one). */
    @Column(columnDefinition = "TEXT")
    private String explanation;

    /** AI (approved from an AI draft), IMPORT or MANUAL; null on rows written before this column. */
    @Column(length = 16)
    private String source;

    private LocalDateTime createdAt;
}
