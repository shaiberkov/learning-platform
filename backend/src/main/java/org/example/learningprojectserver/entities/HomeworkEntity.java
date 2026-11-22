package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Homework given by a teacher to the students of one or more classes. Separate from tests. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "homework", indexes = {
        @Index(name = "idx_homework_teacher", columnList = "teacherUserId"),
        @Index(name = "idx_homework_due", columnList = "dueDate")
})
public class HomeworkEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String teacherUserId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String subject;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    private LocalDateTime createdAt;

    /** Set once the automatic "due soon" reminder went out. */
    private boolean reminderSent;

    /** The students the homework was given to, fixed when it is created. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "homework_student", joinColumns = @JoinColumn(name = "homework_id"))
    @Column(name = "student_user_id")
    private List<String> studentUserIds = new ArrayList<>();
}
