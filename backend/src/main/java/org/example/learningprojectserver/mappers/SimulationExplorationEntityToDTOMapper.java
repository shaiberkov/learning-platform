package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.RealWorldDTOs.SimulationExplorationDTO;
import org.example.learningprojectserver.entities.SimulationExplorationEntity;
import org.springframework.stereotype.Component;

@Component
public class SimulationExplorationEntityToDTOMapper implements Mapper<SimulationExplorationEntity, SimulationExplorationDTO> {
    @Override
    public SimulationExplorationDTO apply(SimulationExplorationEntity e) {
        return new SimulationExplorationDTO(e.getSimulationKey(), e.getExploreCount(),
                e.getLastExploredAt() == null ? null : e.getLastExploredAt().toString());
    }
}
