package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One "how do I feel?" check-in before an exam ([s10]). At most one per student per day.
 * Never shown to teachers; a distress pattern reaches the school manager only with the
 * student's consent, and then without the entries themselves.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "student_mood_checkin", indexes = {
        @Index(name = "idx_mood_checkin_student_date", columnList = "studentUserId,checkinDate")
})
public class StudentMoodCheckinEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    @Column(nullable = false)
    private LocalDate checkinDate;

    /** GREAT, OK, NERVOUS, STRESSED or OVERWHELMED (WellbeingPolicy.Mood). */
    @Column(nullable = false, length = 20)
    private String mood;

    private boolean distress;

    /** The exam the check-in was made before, when there was one ("subject topic"). */
    private String examLabel;

    /** True when this check-in triggered the alert to the school manager. */
    private boolean escalated;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
