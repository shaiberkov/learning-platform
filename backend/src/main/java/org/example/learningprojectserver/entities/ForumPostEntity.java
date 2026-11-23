package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** An answer in a class forum thread, by a classmate or the teacher. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "class_forum_post", indexes = {
        @Index(name = "idx_forum_post_thread", columnList = "thread_id")
})
public class ForumPostEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thread_id", nullable = false)
    private Long threadId;

    @Column(nullable = false)
    private String authorUserId;
    private String authorName;

    /** STUDENT or TEACHER (the role when written). */
    @Column(length = 16)
    private String authorRole;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    /** Marked as a correct answer by the class teacher. */
    private boolean approved;
    private String approvedByName;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
