package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.learningprojectserver.enums.GradeCategory;

import java.time.LocalDate;

/** A column the teacher added to a class gradebook (homework, quiz, an exam outside the system...). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "gradebook_assignment", indexes = {
        @Index(name = "idx_gradebook_assignment_teacher_class", columnList = "teacher_id, class_room_id")
})
public class GradebookAssignmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", nullable = false)
    private String teacherId;

    @Column(name = "class_room_id", nullable = false)
    private Long classRoomId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GradeCategory category;

    private double maxScore = 100;

    private LocalDate assignedDate;
}
