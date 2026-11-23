package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One student's score on one gradebook assignment. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "gradebook_grade", uniqueConstraints = {
        @UniqueConstraint(name = "uk_gradebook_grade_assignment_student", columnNames = {"assignment_id", "student_user_id"})
})
public class GradebookGradeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private GradebookAssignmentEntity assignment;

    @Column(name = "student_user_id", nullable = false)
    private String studentUserId;

    @Column(nullable = false)
    private double score;
}
