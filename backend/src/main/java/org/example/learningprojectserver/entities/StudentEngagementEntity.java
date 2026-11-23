package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** XP total and daily streak of one student. Created on the student's first activity. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "student_engagement", indexes = {
        @Index(name = "idx_engagement_student", columnList = "studentUserId", unique = true)
})
public class StudentEngagementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    private int totalXp;
    private int currentStreak;
    private int longestStreak;
    private LocalDate lastActiveDate;

    public StudentEngagementEntity(String studentUserId) {
        this.studentUserId = studentUserId;
    }
}
