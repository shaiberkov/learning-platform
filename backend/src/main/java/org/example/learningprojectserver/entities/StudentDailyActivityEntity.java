package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** XP a student earned on one day. Feeds the personal weekly progress and class challenges. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "student_daily_activity", indexes = {
        @Index(name = "idx_daily_activity_student_date", columnList = "studentUserId,activityDate")
})
public class StudentDailyActivityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    @Column(nullable = false)
    private LocalDate activityDate;

    private int xp;

    public StudentDailyActivityEntity(String studentUserId, LocalDate activityDate) {
        this.studentUserId = studentUserId;
        this.activityDate = activityDate;
    }
}
