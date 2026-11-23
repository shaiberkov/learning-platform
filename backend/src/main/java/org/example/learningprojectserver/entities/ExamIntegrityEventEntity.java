package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.learningprojectserver.enums.IntegrityEventType;

import java.time.LocalDateTime;

/** A tab-leave or focus signal sent by the exam page, stamped with server time. */
@Entity
@Table(name = "exam_integrity_event")
@Getter
@Setter
@NoArgsConstructor
public class ExamIntegrityEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private ExamAttemptEntity attempt;

    @Enumerated(EnumType.STRING)
    private IntegrityEventType type;

    private LocalDateTime occurredAt;
}
