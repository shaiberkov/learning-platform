package org.example.learningprojectserver.service.realworld;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.RealWorldDTOs.SimulationExplorationDTO;
import org.example.learningprojectserver.entities.SimulationExplorationEntity;
import org.example.learningprojectserver.mappers.SimulationExplorationEntityToDTOMapper;
import org.example.learningprojectserver.repository.SimulationExplorationRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Records which interactive simulations a student opened, so the gallery can mark them as
 * explored. The simulations themselves run entirely in the browser.
 */
@Service
@RequiredArgsConstructor
public class SimulationService {

    /** Must match the keys in frontend/src/features/simulations/simulationCatalog.js. */
    public static final Set<String> SIMULATION_KEYS = Set.of("parabola", "line-slope", "triangle", "motion");

    private final SimulationExplorationRepository explorationRepository;
    private final SimulationExplorationEntityToDTOMapper mapper;

    public BasicResponse<SimulationExplorationDTO> recordExploration(String studentId, String simulationKey) {
        if (studentId == null) {
            return new BasicResponse<>(false, "משתמש לא מזוהה");
        }
        if (simulationKey == null || !SIMULATION_KEYS.contains(simulationKey)) {
            return new BasicResponse<>(false, "סימולציה לא מוכרת");
        }
        LocalDateTime now = LocalDateTime.now();
        SimulationExplorationEntity entity = explorationRepository
                .findByStudentUserIdAndSimulationKey(studentId, simulationKey)
                .orElseGet(() -> {
                    SimulationExplorationEntity e = new SimulationExplorationEntity();
                    e.setStudentUserId(studentId);
                    e.setSimulationKey(simulationKey);
                    e.setFirstExploredAt(now);
                    return e;
                });
        entity.setExploreCount(entity.getExploreCount() + 1);
        entity.setLastExploredAt(now);
        BasicResponse<SimulationExplorationDTO> response = new BasicResponse<>(true, null);
        response.setData(mapper.apply(explorationRepository.save(entity)));
        return response;
    }

    public BasicResponse<List<SimulationExplorationDTO>> getExplored(String studentId) {
        BasicResponse<List<SimulationExplorationDTO>> response = new BasicResponse<>(true, null);
        response.setData(explorationRepository.findByStudentUserIdOrderBySimulationKey(studentId).stream()
                .map(mapper).toList());
        return response;
    }
}
