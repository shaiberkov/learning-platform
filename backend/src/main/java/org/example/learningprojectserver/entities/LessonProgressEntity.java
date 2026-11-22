package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Where a student is in the learn, practice, review flow of one lesson page. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lesson_progress", indexes = {
        @Index(name = "idx_lesson_progress_student", columnList = "studentUserId,lessonContentId")
})
public class LessonProgressEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    @Column(nullable = false)
    private Long lessonContentId;

    private boolean learned;
    private boolean practiced;
    private boolean reviewed;

    private LocalDateTime updatedAt;
}
