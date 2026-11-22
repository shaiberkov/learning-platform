package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Testing accommodations of one student. The teacher sets the extra time; the student turns
 * read-aloud and the dyslexia-friendly font on or off.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "student_accommodation", indexes = {
        @Index(name = "idx_accommodation_student", columnList = "studentUserId", unique = true)
})
public class StudentAccommodationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    /** 0-100: added to every exam's time limit. */
    private int extraTimePercent;

    private boolean readAloud;
    private boolean dyslexiaFont;

    public StudentAccommodationEntity(String studentUserId) {
        this.studentUserId = studentUserId;
    }
}
