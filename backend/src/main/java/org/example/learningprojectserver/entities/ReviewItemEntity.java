package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.learningprojectserver.service.adaptive.Sm2Schedulable;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One entry in a student's "mistakes notebook": a question the student got wrong,
 * scheduled for review with SM-2. The question text and answer are copied so the item
 * survives changes to the original question row.
 */
@Entity
@Table(name = "review_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"studentUserId", "questionId"}),
        indexes = @Index(name = "idx_review_student_due", columnList = "studentUserId,dueDate"))
@Getter
@Setter
@NoArgsConstructor
public class ReviewItemEntity implements Sm2Schedulable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    @Column(nullable = false)
    private Long questionId;

    private String subject;
    private String topic;
    private String subTopic;
    @Column(length = 1000)
    private String questionText;
    @Column(length = 500)
    private String answer;

    /** PRACTICE, PRACTICE_TEST or TEACHER_TEST. */
    private String source;

    /** SM-2 easiness factor, starts at 2.5 and never drops below 1.3. */
    private double easiness = 2.5;
    /** Consecutive successful reviews. */
    private int repetitions;
    private int intervalDays;
    private int lapses;
    private int reviewCount;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDateTime createdAt;
    private LocalDateTime lastReviewedAt;
}
