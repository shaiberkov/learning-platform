package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.learningprojectserver.service.adaptive.Sm2Schedulable;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One flashcard in one student's deck, scheduled with SM-2 exactly like a mistakes-notebook item. */
@Entity
@Table(name = "flashcard_review",
        uniqueConstraints = @UniqueConstraint(name = "uk_flashcard_review_student_card",
                columnNames = {"studentUserId", "flashcardId"}),
        indexes = @Index(name = "idx_flashcard_review_student_due", columnList = "studentUserId,dueDate"))
@Getter
@Setter
@NoArgsConstructor
public class FlashcardReviewEntity implements Sm2Schedulable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    @Column(nullable = false)
    private Long flashcardId;

    private double easiness = 2.5;
    private int repetitions;
    private int intervalDays;
    private int lapses;
    private int reviewCount;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDateTime createdAt;
    private LocalDateTime lastReviewedAt;
}
