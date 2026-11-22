package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A student's answer to a homework. One per student; resubmitting overwrites it. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "homework_submission", indexes = {
        @Index(name = "idx_submission_homework_student", columnList = "homework_id,studentUserId")
})
public class HomeworkSubmissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "homework_id")
    private HomeworkEntity homework;

    @Column(nullable = false)
    private String studentUserId;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime submittedAt;

    /** True when submitted after the due date. */
    private boolean late;
}
