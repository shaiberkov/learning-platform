package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A tutor chat message. Kept for {@code ai.retention.days} and then deleted by
 * {@code jobs/AiRetentionJob}. The student sees their own messages; school staff see only the
 * ones the safety filter flagged.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ai_conversation_message", indexes = {
        @Index(name = "idx_ai_conv_user_question", columnList = "userId,questionId"),
        @Index(name = "idx_ai_conv_created", columnList = "createdAt")
})
public class AiConversationMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    private String schoolCode;

    /** The practice question the chat is about; null for a free chat (for example a photographed exercise). */
    private Long questionId;

    @Column(nullable = false, length = 16)
    private String role;

    @Column(columnDefinition = "TEXT")
    private String content;

    private boolean flagged;

    @Column(length = 40)
    private String flagReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
