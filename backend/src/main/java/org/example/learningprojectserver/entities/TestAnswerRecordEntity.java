package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One answered question of a submitted exam (practice or teacher test), with the time the
 * student spent on it. Feeds the post-exam feedback and the anonymous class comparison.
 */
@Entity
@Table(name = "test_answer_record",
        indexes = {
                @Index(name = "idx_answer_record_test_student", columnList = "testId,studentUserId"),
                @Index(name = "idx_answer_record_class_subtopic", columnList = "classRoomId,subTopic")
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
    private Long classRoomId;
    private Long questionId;
    /** PRACTICE_TEST or TEACHER_TEST. */
    private String source;

    private String subject;
    private String topic;
    private String subTopic;
    @Column(length = 1000)
    private String questionText;
    @Column(length = 500)
    private String givenAnswer;
    @Column(length = 500)
    private String correctAnswer;
    private boolean correct;

    /** Seconds spent on the question; null until the client reports its timings. */
    private Integer secondsSpent;

    private LocalDateTime createdAt;
}
