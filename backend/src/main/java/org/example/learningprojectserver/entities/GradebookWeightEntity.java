package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.learningprojectserver.enums.GradeCategory;

/** The weight (percent) a teacher gives a category in one class. Missing rows use the category default. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "gradebook_weight", uniqueConstraints = {
        @UniqueConstraint(name = "uk_gradebook_weight", columnNames = {"teacher_id", "class_room_id", "category"})
})
public class GradebookWeightEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", nullable = false)
    private String teacherId;

    @Column(name = "class_room_id", nullable = false)
    private Long classRoomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GradeCategory category;

    private int weight;
}
