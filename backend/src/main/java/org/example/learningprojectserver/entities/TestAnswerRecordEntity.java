package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One answered question of a submitted exam (practice or teacher test), with the time the
 * student spent on it. Written once per student and test by {@code ExamFeedbackService.recordExamAnswers}.
 * Feeds the post-exam feedback, the anonymous class comparison and the weekly teacher insights
 * (which also need the chosen wrong answer, to spot questions where most students chose the same one).
 */
@Entity
@Table(name = "test_answer_record",
        indexes = {
                @Index(name = "idx_answer_record_test_student", columnList = "testId,studentUserId"),
                @Index(name = "idx_answer_record_class_subtopic", columnList = "classRoomId,subTopic"),
                @Index(name = "idx_answer_record_teacher_time", columnList = "teacherUserId,createdAt")
        })
@Getter
@Setter
@NoArgsConstructor
public class TestAnswerRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long testId;
    @Column(nullable = false)
    private String studentUserId;
    /** The teacher who wrote the test; null for practice tests. */
    private String teacherUserId;
    private Long classRoomId;
    private Long questionId;
    /** PRACTICE_TEST or TEACHER_TEST. */
    private String source;

    private String subject;
    private String topic;
    private String subTopic;
    @Column(columnDefinition = "TEXT")
    private String questionText;
    /** What the student answered; null when the question was left blank. */
    @Column(length = 500)
    private String givenAnswer;
    @Column(length = 500)
    private String correctAnswer;
    private boolean correct;

    /** Seconds spent on the question; null until the client reports its timings. */
    private Integer secondsSpent;

    private LocalDateTime createdAt;
}
