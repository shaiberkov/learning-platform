package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** A section (פרק) of a past exam. questionsToCount set: only the best N answers in the section count. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "past_exam_section")
public class PastExamSectionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private PastExamEntity exam;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "questions_to_count")
    private Integer questionsToCount;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PastExamQuestionEntity> questions = new ArrayList<>();
}
