package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Elo mastery rating of one student in one sub-topic. */
@Entity
@Table(name = "topic_mastery",
        uniqueConstraints = @UniqueConstraint(columnNames = {"studentUserId", "subject", "subTopic"}),
        indexes = @Index(name = "idx_mastery_student", columnList = "studentUserId"))
@Getter
@Setter
@NoArgsConstructor
public class TopicMasteryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    private String subject;
    private String topic;
    @Column(nullable = false)
    private String subTopic;

    private double rating;
    private int attempts;
    private int correctCount;

    private LocalDateTime updatedAt;
}
