package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.SimulationExplorationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SimulationExplorationRepository extends JpaRepository<SimulationExplorationEntity, Long> {

    List<SimulationExplorationEntity> findByStudentUserIdOrderBySimulationKey(String studentUserId);

    Optional<SimulationExplorationEntity> findByStudentUserIdAndSimulationKey(String studentUserId, String simulationKey);
}
