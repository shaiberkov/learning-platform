package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A question a student asked in the class forum. The class teacher moderates it and approves answers. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "class_forum_thread", indexes = {
        @Index(name = "idx_forum_thread_class", columnList = "class_room_id")
})
public class ForumThreadEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_room_id", nullable = false)
    private Long classRoomId;

    @Column(name = "school_code")
    private String schoolCode;

    @Column(nullable = false)
    private String authorUserId;
    private String authorName;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(length = 100)
    private String topic;

    /** The exercise the student is stuck on, as they copied it. */
    @Column(length = 1000)
    private String exerciseText;

    private int answerCount;

    /** At least one answer was approved by the teacher. */
    private boolean resolved;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastActivityAt;
}
