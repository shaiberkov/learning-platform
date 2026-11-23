package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Teacher-edited hints and guided solution for one sub-topic. Questions are generated,
 * so hints are written per question type (sub-topic); the full solution ends with the
 * concrete answer of the question being solved.
 */
@Entity
@Table(name = "sub_topic_hint",
        uniqueConstraints = @UniqueConstraint(columnNames = {"subject", "subTopic"}))
@Getter
@Setter
@NoArgsConstructor
public class SubTopicHintEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String subject;
    private String topic;
    @Column(nullable = false)
    private String subTopic;

    @Column(length = 1000)
    private String hint1;
    @Column(length = 1000)
    private String hint2;
    /** Solution steps, one per line. */
    @Column(length = 4000)
    private String solutionSteps;

    private String updatedByTeacherId;
    private LocalDateTime updatedAt;
}
