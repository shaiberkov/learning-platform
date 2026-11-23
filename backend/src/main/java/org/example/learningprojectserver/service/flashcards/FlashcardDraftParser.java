package org.example.learningprojectserver.service.flashcards;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.learningprojectserver.dto.FlashcardDTOs.FlashcardDraftDTO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the model's JSON into flashcard drafts, and, when no AI is available, splits a pasted summary
 * into cards from lines shaped like "term: explanation" (also " - ", " – " or " = ").
 */
public final class FlashcardDraftParser {

    static final int MAX_FRONT = 500;
    static final int MAX_BACK = 1000;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** "front: back", "front - back", "front – back", "front = back"; the separator needs a space after it. */
    private static final Pattern LINE_PAIR = Pattern.compile("^\\s*(?:[-*•\\d.)]+\\s+)?(.{2,200}?)\\s*(?::|\\s[-–—=])\\s+(.{2,})$");

    private FlashcardDraftParser() {}

    public static List<FlashcardDraftDTO> parseJson(String json, int maxCount) {
        List<FlashcardDraftDTO> result = new ArrayList<>();
        JsonNode root;
        try {
            root = MAPPER.readTree(stripCodeFence(json));
        } catch (IOException e) {
            return result;
        }
        if (root == null) {
            return result;
        }
        JsonNode items = root.isArray() ? root : root.path("cards");
        if (!items.isArray()) {
            return result;
        }
        for (JsonNode item : items) {
            if (result.size() >= maxCount) {
                break;
            }
            add(result, text(item, "front"), text(item, "back"));
        }
        return result;
    }

    /** The non-AI fallback: one card per "term: explanation" line. */
    public static List<FlashcardDraftDTO> splitLines(String summary, int maxCount) {
        List<FlashcardDraftDTO> result = new ArrayList<>();
        if (summary == null) {
            return result;
        }
        for (String line : summary.split("\\R")) {
            if (result.size() >= maxCount) {
                break;
            }
            Matcher m = LINE_PAIR.matcher(line);
            if (m.matches()) {
                add(result, m.group(1).trim(), m.group(2).trim());
            }
        }
        return result;
    }

    private static void add(List<FlashcardDraftDTO> result, String front, String back) {
        if (front.isBlank() || back.isBlank()) {
            return;
        }
        result.add(new FlashcardDraftDTO(truncate(front, MAX_FRONT), truncate(back, MAX_BACK)));
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String text(JsonNode item, String field) {
        JsonNode node = item.path(field);
        return node.isMissingNode() || node.isNull() ? "" : node.asText("").trim();
    }

    private static String stripCodeFence(String json) {
        String trimmed = json == null ? "" : json.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }
}
