package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * The student's explicit opt-in ([s10]) to let a repeated distress pattern alert the school
 * staff member who acts as counselor (the school manager). Kept with timestamps and the
 * version of the privacy text the student agreed to.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "student_wellbeing_consent")
public class StudentWellbeingConsentEntity {
    @Id
    private String studentUserId;

    private boolean consented;

    private LocalDateTime consentedAt;

    private LocalDateTime revokedAt;

    @Column(length = 20)
    private String policyVersion;

    public StudentWellbeingConsentEntity(String studentUserId) {
        this.studentUserId = studentUserId;
    }
}
