package org.example.learningprojectserver.service.ai.provider;

public record AiResult(String text, int promptTokens, int completionTokens, String model) {
}
