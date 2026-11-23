package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A virtual study room opened by a student, visible to the students of the same school.
 * Every member computes the shared Pomodoro phase from {@code timerStartedAt}; who is in the room
 * right now and the break chat are kept in memory by {@code StudyRoomService}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "study_room", indexes = {
        @Index(name = "idx_study_room_school_active", columnList = "schoolCode,active")
})
public class StudyRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String schoolCode;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false)
    private String createdByUserId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** Start of the first focus block; the 25 + 5 minute cycle repeats from here. */
    @Column(nullable = false)
    private LocalDateTime timerStartedAt;

    /** Last time a member was seen (written at most every few minutes); idle rooms are closed. */
    @Column(nullable = false)
    private LocalDateTime lastActivityAt;

    private boolean active;
}
