package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** A shared goal for a whole class: the class's combined XP between two dates. No per-student ranking. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "class_challenge")
public class ClassChallengeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String teacherUserId;

    @Column(nullable = false)
    private Long classRoomId;

    @Column(nullable = false)
    private String title;

    private int targetXp;

    private LocalDate startDate;
    private LocalDate endDate;
}
