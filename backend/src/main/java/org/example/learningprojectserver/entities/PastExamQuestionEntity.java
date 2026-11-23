package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One question of a past exam. answer is the exact expected answer (parts separated by commas get
 * partial credit, see ExamSimGrader); rubric is the grading guide (מחוון) shown after the exam.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "past_exam_question")
public class PastExamQuestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private PastExamSectionEntity section;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false, length = 512)
    private String answer;

    @Column(nullable = false)
    private int points;

    @Column(columnDefinition = "TEXT")
    private String rubric;

    private String topic;

    @Column(name = "sub_topic")
    private String subTopic;

    /** Generator difficulty 1-5, used by the score prediction. */
    @Column(nullable = false)
    private int level;
}
