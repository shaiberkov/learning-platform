package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "message_entity", indexes = {
        @Index(name = "idx_message_batch", columnList = "batchId"),
        @Index(name = "idx_message_thread", columnList = "threadId"),
        @Index(name = "idx_message_pending_schedule", columnList = "pending,scheduledFor")
})
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private UserEntity receiver;

    // Read receipt: set the first time the receiver opens the message.
    private LocalDateTime readAt;

    // All copies created by one send share a batchId (a broadcast to a class is one batch).
    @Column(length = 36)
    private String batchId;

    // The batchId of the message that started the conversation; replies keep it.
    @Column(length = 36)
    private String threadId;

    private Long parentMessageId;

    private Long attachmentId;

    // Scheduled send: pending stays true until the dispatch job delivers it at scheduledFor.
    private LocalDateTime scheduledFor;

    private Boolean pending;

}
