package org.example.learningprojectserver.service.ai.provider;

import java.util.List;

/**
 * A provider-neutral completion request. {@code model} may be null to use the configured default.
 * {@code jsonOutput} asks the provider for a single JSON object.
 */
public record AiRequest(List<AiMessage> messages, String model, double temperature, boolean jsonOutput, int maxTokens) {

    public static AiRequest chat(List<AiMessage> messages) {
        return new AiRequest(messages, null, 0.4, false, 600);
    }

    public static AiRequest json(List<AiMessage> messages, int maxTokens) {
        return new AiRequest(messages, null, 0.2, true, maxTokens);
    }

    public AiRequest withModel(String newModel) {
        return new AiRequest(messages, newModel, temperature, jsonOutput, maxTokens);
    }

    public boolean hasImage() {
        return messages.stream().anyMatch(m -> m.imageDataUrl() != null);
    }
}
