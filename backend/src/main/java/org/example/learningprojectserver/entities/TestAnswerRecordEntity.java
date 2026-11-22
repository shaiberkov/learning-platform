package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * What a student actually answered on one question of a teacher test. {@code TestResultEntity}
 * keeps only right/wrong lists; the weekly teacher insights also need the chosen wrong answer
 * (to spot questions where most students chose the same wrong answer).
 * Written by {@code TestAnswerRecorder} when a teacher test is checked.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "test_answer_record", indexes = {
        @Index(name = "idx_answer_record_teacher_time", columnList = "teacherUserId,createdAt")
})
public class TestAnswerRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long testId;
    private Long questionId;

    @Column(nullable = false)
    private String teacherUserId;

    @Column(nullable = false)
    private String studentUserId;

    private String topic;
    private String subTopic;

    @Column(columnDefinition = "TEXT")
    private String questionText;

    private String correctAnswer;

    /** Empty when the student left the question blank. */
    private String answer;

    private boolean correct;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
