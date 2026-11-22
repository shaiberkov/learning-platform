package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A learning page written by a teacher (text, optional video, worked examples), linked to the
 * practice of the same subject/topic/sub-topic. Not to be confused with LessonEntity,
 * which is a slot in the weekly schedule.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lesson_content", indexes = {
        @Index(name = "idx_lesson_content_teacher", columnList = "teacherUserId")
})
public class LessonContentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String teacherUserId;

    @Column(nullable = false)
    private String title;

    private String subject;
    private String topic;
    private String subTopic;

    @Column(columnDefinition = "TEXT")
    private String body;

    /** Embed URL (YouTube or Vimeo only), normalised on save. */
    private String videoEmbedUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "lesson_content_example", joinColumns = @JoinColumn(name = "lesson_content_id"))
    @Column(name = "example_text", columnDefinition = "TEXT")
    @OrderColumn(name = "example_order")
    private List<String> examples = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
