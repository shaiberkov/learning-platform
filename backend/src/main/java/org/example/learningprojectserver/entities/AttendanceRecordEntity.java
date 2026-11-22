package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.learningprojectserver.enums.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One student's attendance in one lesson (a timetable slot) on one date. Class, school and names are
 * copied in so reports stay readable if the student later changes class.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "attendance_record",
        uniqueConstraints = @UniqueConstraint(name = "uk_attendance_lesson_date_student",
                columnNames = {"lesson_id", "lesson_date", "student_user_id"}),
        indexes = {
                @Index(name = "idx_attendance_school_date", columnList = "school_code, lesson_date"),
                @Index(name = "idx_attendance_student_date", columnList = "student_user_id, lesson_date")
        })
public class AttendanceRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private LessonEntity lesson;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate lessonDate;

    @Column(name = "student_user_id", nullable = false)
    private String studentUserId;
    private String studentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    private String subject;
    private Long classRoomId;
    private String classRoomName;

    @Column(name = "school_code")
    private String schoolCode;

    private String markedByTeacherId;
    private LocalDateTime markedAt;
}
