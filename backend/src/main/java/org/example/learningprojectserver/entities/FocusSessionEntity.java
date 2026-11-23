package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One focus-mode session of a student: when, for how long, on what, and whether it ran to the end. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "focus_session", indexes = {
        @Index(name = "idx_focus_session_student_time", columnList = "studentUserId,startedAt")
})
public class FocusSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    @Column(length = 80)
    private String task;

    private int plannedMinutes;

    private int actualMinutes;

    /** True when the timer reached zero, false when the student stopped early. */
    private boolean completed;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime endedAt;
}
