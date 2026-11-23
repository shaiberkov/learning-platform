package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A badge a student earned for a real achievement (streak, mastery of a sub-topic, perfect test...). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "student_badge", indexes = {
        @Index(name = "idx_badge_student", columnList = "studentUserId")
})
public class StudentBadgeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    /** Stable code, e.g. STREAK_7 or MASTERED:<sub-topic>. Unique per student. */
    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String title;

    private String description;

    private LocalDateTime earnedAt;
}
