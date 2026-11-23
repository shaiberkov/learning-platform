package org.example.learningprojectserver.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** [s06] API shapes of "explain it to me" (Feynman) and the mistake-type diagnosis. */
public final class DiagnosisDTOs {

    private DiagnosisDTOs() {}

    /* ---------- Mistake-type diagnosis ---------- */

    /** A notebook item with its diagnosis. errorType is a MistakeType name; source is RULE or AI. The answer is not sent. */
    public record MistakeItemDTO(Long id, String subject, String topic, String subTopic, String questionText,
                                 String givenAnswer, String errorType, String errorTypeSource, String errorNote,
                                 LocalDateTime createdAt) {}

    /** counts: MistakeType name → number of notebook items (items from before the diagnosis count as UNCLASSIFIED). */
    public record MistakeDiagnosisDTO(Map<String, Long> counts, long total, String dominantType,
                                      List<MistakeItemDTO> items) {}

    public record SelfCheckResultDTO(boolean correct, String correctAnswer) {}

    public record ClassMistakeRowDTO(String userId, String name, Map<String, Long> counts) {}

    public record ClassMistakeSummaryDTO(Long classRoomId, Map<String, Long> totals, List<ClassMistakeRowDTO> students) {}

    /* ---------- Explain it to me (Feynman) ---------- */

    public record FeynmanPromptDTO(String key, String subject, String topic, String prompt) {}

    public record FeynmanSubmitRequest(String promptKey, String text, String inputMode) {}

    public record FeynmanExplanationDTO(Long id, String studentUserId, String studentName, String promptKey,
                                        String topic, String promptText, String explanationText, String inputMode,
                                        Integer score, List<String> gaps, String feedback, String evaluatedBy,
                                        LocalDateTime createdAt) {}
}
