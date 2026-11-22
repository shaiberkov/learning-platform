package org.example.learningprojectserver.service.ai.questions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Turns the model's JSON into questions, dropping any item that misses a text or an answer. */
final class GeneratedQuestionParser {

    record GeneratedQuestion(String questionText, String answer, String explanation, String subTopic, String difficulty) {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GeneratedQuestionParser() {
    }

    static List<GeneratedQuestion> parse(String json, int maxCount) {
        List<GeneratedQuestion> result = new ArrayList<>();
        JsonNode root;
        try {
            root = MAPPER.readTree(stripCodeFence(json));
        } catch (IOException e) {
            return result;
        }
        JsonNode items = root.isArray() ? root : root.path("questions");
        if (!items.isArray()) {
            return result;
        }
        for (JsonNode item : items) {
            if (result.size() >= maxCount) {
                break;
            }
            String text = text(item, "questionText");
            String answer = text(item, "answer");
            if (text.isBlank() || answer.isBlank()) {
                continue;
            }
            result.add(new GeneratedQuestion(text, answer, text(item, "explanation"), text(item, "subTopic"),
                    text(item, "difficulty")));
        }
        return result;
    }

    private static String text(JsonNode item, String field) {
        JsonNode node = item.path(field);
        return node.isMissingNode() || node.isNull() ? "" : node.asText("").trim();
    }

    private static String stripCodeFence(String json) {
        String trimmed = json == null ? "" : json.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\s*", "").replaceFirst("\s*```$", "");
        }
        return trimmed;
    }
}
