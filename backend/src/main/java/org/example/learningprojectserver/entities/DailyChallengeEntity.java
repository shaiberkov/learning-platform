package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** The one daily question of a grade level in a school. Created by the first student who opens it that day. */
@Entity
@Table(name = "daily_challenge",
        uniqueConstraints = @UniqueConstraint(name = "uk_daily_challenge_scope",
                columnNames = {"schoolCode", "gradeName", "challengeDate"}))
@Getter
@Setter
@NoArgsConstructor
public class DailyChallengeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String schoolCode;

    @Column(nullable = false, length = 64)
    private String gradeName;

    @Column(nullable = false)
    private LocalDate challengeDate;

    private String subject;
    private String topic;
    private String subTopic;
    private int level;

    @Column(columnDefinition = "TEXT")
    private String questionText;

    private String answer;

    private LocalDateTime createdAt;
}
