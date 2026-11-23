package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.learningprojectserver.enums.PeerQuestionStatus;

import java.time.LocalDateTime;

/**
 * A multiple-choice question a student wrote for their classmates. Once the class teacher approves it,
 * it is copied into the teacher's question bank (bankQuestionId) and served in the class practice,
 * credited to the student who wrote it.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "peer_question", indexes = {
        @Index(name = "idx_peer_question_class_status", columnList = "class_room_id,status"),
        @Index(name = "idx_peer_question_author", columnList = "author_user_id")
})
public class PeerQuestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_room_id", nullable = false)
    private Long classRoomId;

    @Column(name = "school_code")
    private String schoolCode;

    @Column(name = "author_user_id", nullable = false)
    private String authorUserId;
    private String authorName;

    @Column(nullable = false, length = 2000)
    private String questionText;

    @Column(nullable = false, length = 500)
    private String correctAnswer;

    /** The wrong options, one per line. */
    @Column(nullable = false, length = 2000)
    private String distractors;

    @Column(length = 100)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PeerQuestionStatus status;

    private String reviewerTeacherId;

    @Column(length = 500)
    private String reviewNote;

    /** The bank_question row created on approval. */
    private Long bankQuestionId;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
