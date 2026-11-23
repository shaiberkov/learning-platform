package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One student's use of one interactive simulation (the gallery marks it as explored). */
@Entity
@Table(name = "simulation_exploration",
        uniqueConstraints = @UniqueConstraint(name = "uk_simulation_exploration_student_key",
                columnNames = {"student_user_id", "simulation_key"}))
@Getter
@Setter
@NoArgsConstructor
public class SimulationExplorationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_user_id", nullable = false, length = 255)
    private String studentUserId;

    @Column(name = "simulation_key", nullable = false, length = 64)
    private String simulationKey;

    @Column(name = "explore_count", nullable = false)
    private int exploreCount;

    @Column(name = "first_explored_at", nullable = false)
    private LocalDateTime firstExploredAt;

    @Column(name = "last_explored_at", nullable = false)
    private LocalDateTime lastExploredAt;
}
