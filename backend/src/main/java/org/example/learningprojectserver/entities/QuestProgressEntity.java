package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One student's place in one story quest (the quests themselves are defined in code, service/quest/QuestCatalog).
 * The pending question and its answer are kept here, so the answer never reaches the browser and
 * a quest survives a restart or a new device.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "student_quest_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_quest_progress_student_quest", columnNames = {"studentUserId", "questCode"})
}, indexes = {
        @Index(name = "idx_quest_progress_student", columnList = "studentUserId")
})
public class QuestProgressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    @Column(nullable = false, length = 40)
    private String questCode;

    /** Index of the current chapter; equals the chapter count once the quest is completed. */
    private int chapterIndex;

    /** Correct answers in the current chapter. */
    private int chapterCorrect;

    /** Wrong tries on the pending question. */
    private int wrongAttempts;

    private int totalCorrect;

    @Column(length = 500)
    private String pendingQuestion;

    private String pendingAnswer;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;
}
