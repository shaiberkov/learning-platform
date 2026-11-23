package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.learningprojectserver.enums.DuelMode;
import org.example.learningprojectserver.enums.DuelStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A 1-on-1 duel of a few generated questions on one sub-topic. The per-player counters are kept on the row
 * so the polling screens read one row instead of counting the questions.
 */
@Entity
@Table(name = "duel", indexes = {
        @Index(name = "idx_duel_challenger", columnList = "challengerId"),
        @Index(name = "idx_duel_opponent", columnList = "opponentId")
})
@Getter
@Setter
@NoArgsConstructor
public class DuelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String challengerId;
    private String challengerName;

    @Column(nullable = false)
    private String opponentId;
    private String opponentName;

    private String schoolCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DuelMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DuelStatus status;

    private String subject;
    private String topic;
    private String subTopic;

    /** Generator level (1-5) of the questions, chosen from the weaker player's mastery. */
    private int level;

    /** COOP only: the score the pair has to beat (out of both players' answers together). */
    private Integer systemScore;

    private int challengerAnswered;
    private int challengerCorrect;
    private int opponentAnswered;
    private int opponentCorrect;

    private LocalDateTime challengerFinishedAt;
    private LocalDateTime opponentFinishedAt;

    /** CHALLENGER, OPPONENT or DRAW (competitive); TEAM, SYSTEM or DRAW (coop). Null until finished. */
    @Column(length = 16)
    private String outcome;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @OneToMany(mappedBy = "duel", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<DuelQuestionEntity> questions = new ArrayList<>();
}
