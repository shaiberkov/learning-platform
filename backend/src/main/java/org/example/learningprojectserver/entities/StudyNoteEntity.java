package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A personal note, highlight or bookmark a student attached to a question, a lesson, a
 * mistakes-notebook item or a flashcard. The target's title and topic are copied so the notebook
 * can be filtered by topic without joining the target tables.
 */
@Entity
@Table(name = "study_note",
        indexes = {
                @Index(name = "idx_study_note_student_topic", columnList = "studentUserId,topic"),
                @Index(name = "idx_study_note_student_target", columnList = "studentUserId,targetType,targetId")
        })
@Getter
@Setter
@NoArgsConstructor
public class StudyNoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    /** NOTE, HIGHLIGHT or BOOKMARK. */
    @Column(nullable = false, length = 16)
    private String kind;

    /** QUESTION, LESSON, REVIEW_ITEM or FLASHCARD. */
    @Column(nullable = false, length = 16)
    private String targetType;

    private Long targetId;

    @Column(length = 500)
    private String targetTitle;

    private String subject;
    private String topic;
    private String subTopic;

    /** The highlighted passage (for a HIGHLIGHT), copied from the target. */
    @Column(length = 1000)
    private String quote;

    @Column(length = 2000)
    private String body;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
