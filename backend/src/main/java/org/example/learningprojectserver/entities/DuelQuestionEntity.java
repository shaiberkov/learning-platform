package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One question of a duel, with each player's answer to it. */
@Entity
@Table(name = "duel_question",
        uniqueConstraints = @UniqueConstraint(name = "uk_duel_question_position", columnNames = {"duel_id", "position"}))
@Getter
@Setter
@NoArgsConstructor
public class DuelQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "duel_id", nullable = false)
    private DuelEntity duel;

    private int position;

    @Column(columnDefinition = "TEXT")
    private String questionText;

    private String answer;

    private String challengerAnswer;
    private Boolean challengerCorrect;
    private LocalDateTime challengerAnsweredAt;

    private String opponentAnswer;
    private Boolean opponentCorrect;
    private LocalDateTime opponentAnsweredAt;
}
