package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A question in a teacher's question bank. Text and answer live in {@link QuestionEntity}
 * (so the bank question has the same shape as generated ones); this table adds the explanation
 * and who owns it.
 */
@Entity
@DiscriminatorValue("BANK")
@Getter
@Setter
@NoArgsConstructor
public class BankQuestionEntity extends QuestionEntity {

    @Column(columnDefinition = "TEXT")
    private String explanation;

    private String difficulty;
    private String gradeLevel;

    @Column(nullable = false)
    private String ownerTeacherUserId;

    private String schoolCode;

    /** AI (approved from a draft) or MANUAL. */
    @Column(length = 16)
    private String source;

    private LocalDateTime createdAt;
}
