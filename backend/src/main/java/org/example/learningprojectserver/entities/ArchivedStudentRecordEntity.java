package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Snapshot of one student at the moment a school year was closed: where they were,
 * how they did in teacher tests that year, and where the promotion moved them.
 * Kept as plain values so it survives later class, grade or user changes.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "archived_student_record", indexes = {
        @Index(name = "idx_archive_year", columnList = "school_year_id")
})
public class ArchivedStudentRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_year_id", nullable = false)
    private SchoolYearEntity schoolYear;

    private String studentUserId;
    private String studentName;
    private String gradeName;
    private String className;
    private int testsCount;
    private Double averageScore;

    /** PROMOTED, GRADUATED or STAYED */
    private String outcome;
    private String nextGradeName;
    private String nextClassName;
}
