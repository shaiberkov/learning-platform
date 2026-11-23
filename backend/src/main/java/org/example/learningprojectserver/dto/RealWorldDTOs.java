package org.example.learningprojectserver.dto;

import java.util.List;

/** API shapes of the "why does it matter to me?" cards and the simulations gallery. */
public final class RealWorldDTOs {

    private RealWorldDTOs() {}

    /** The real-world card of one topic; customized is true when the school's teacher rewrote it. */
    public record RealWorldTopicDTO(String topic,
                                    List<String> subTopics,
                                    String everydayExample,
                                    String careerLink,
                                    boolean customized,
                                    String updatedAt) {}

    /** A teacher's text for one topic. */
    public record RealWorldOverrideRequest(String topic, String everydayExample, String careerLink) {}

    /** How often the student opened one simulation. */
    public record SimulationExplorationDTO(String simulationKey, int exploreCount, String lastExploredAt) {}
}
