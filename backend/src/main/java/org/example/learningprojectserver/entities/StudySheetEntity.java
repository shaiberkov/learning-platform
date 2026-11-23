package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A personal summary sheet built for a student before a test: the test's topics, the relevant
 * formulas and the student's own mistakes with a short explanation each. {@code content} is the
 * sheet as JSON (see {@code StudySummaryDTOs.SheetContent}), stored so the sheet can be reopened.
 */
@Entity
@Table(name = "study_sheet", indexes = @Index(name = "idx_study_sheet_student", columnList = "studentUserId,createdAt"))
@Getter
@Setter
@NoArgsConstructor
public class StudySheetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    @Column(nullable = false)
    private String title;

    /** The teacher test the sheet prepares for, or null when the student picked topics. */
    private Long testId;

    /** The topics covered, comma separated, for the list view. */
    @Column(length = 1000)
    private String topics;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** False for the basic sheet built without the AI (no explanations). */
    @Column(nullable = false)
    private boolean aiGenerated;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
