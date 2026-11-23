package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.RealWorldDTOs.RealWorldOverrideRequest;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.CurrentUser;
import org.example.learningprojectserver.service.realworld.RealWorldService;
import org.example.learningprojectserver.service.realworld.SimulationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.example.learningprojectserver.constants.ControllerConstants.RealWorld.*;

/**
 * "Why does it matter to me?" cards and the simulations gallery. The acting user always comes
 * from the access token; no endpoint takes a user id.
 */
@RestController
@RequestMapping(REAL_WORLD_BASE_PATH)
@RequiredArgsConstructor
public class RealWorldController {

    private final RealWorldService realWorldService;
    private final SimulationService simulationService;

    @GetMapping(TOPICS)
    public BasicResponse getTopics() {
        return realWorldService.getTopics(CurrentUser.userId(), CurrentUser.role());
    }

    @GetMapping(TOPIC)
    public BasicResponse getTopic(@RequestParam String topic) {
        return realWorldService.getTopic(CurrentUser.userId(), CurrentUser.role(), topic);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping(SAVE_OVERRIDE)
    public BasicResponse saveOverride(@RequestBody RealWorldOverrideRequest request) {
        return realWorldService.saveOverride(CurrentUser.userId(), request);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping(RESET_OVERRIDE)
    public BasicResponse resetOverride(@RequestParam String topic) {
        return realWorldService.resetOverride(CurrentUser.userId(), topic);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping(SIMULATIONS_EXPLORED)
    public BasicResponse getExploredSimulations() {
        return simulationService.getExplored(CurrentUser.userId());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping(RECORD_SIMULATION)
    public BasicResponse recordSimulation(@RequestParam String simulationKey) {
        return simulationService.recordExploration(CurrentUser.userId(), simulationKey);
    }
}
