package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A student's one try at a daily challenge. challengeDate is copied from the challenge for the streak query. */
@Entity
@Table(name = "daily_challenge_attempt",
        uniqueConstraints = @UniqueConstraint(name = "uk_daily_attempt_student",
                columnNames = {"challengeId", "studentUserId"}),
        indexes = @Index(name = "idx_daily_attempt_student_date", columnList = "studentUserId, challengeDate"))
@Getter
@Setter
@NoArgsConstructor
public class DailyChallengeAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long challengeId;

    @Column(nullable = false)
    private String studentUserId;

    @Column(nullable = false)
    private LocalDate challengeDate;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime answeredAt;

    private String answer;

    private boolean correct;
}
