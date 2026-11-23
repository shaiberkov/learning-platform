package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A project the student added to their personal portfolio. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "portfolio_project", indexes = {
        @Index(name = "idx_portfolio_project_student", columnList = "studentUserId")
})
public class PortfolioProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 1000)
    private String description;

    /** The grade the student was in when adding it. */
    @Column(length = 20)
    private String gradeName;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
