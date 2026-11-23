package org.example.learningprojectserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** API shapes of the personal study notebook: notes, highlights and bookmarks linked to a question or lesson. */
public final class StudyNoteDTOs {

    private StudyNoteDTOs() {}

    public record StudyNoteDTO(Long id, String kind, String targetType, Long targetId, String targetTitle,
                               String subject, String topic, String subTopic, String quote, String body,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {}

    /** kind: NOTE, HIGHLIGHT or BOOKMARK; targetType: QUESTION, LESSON, REVIEW_ITEM or FLASHCARD. */
    public record StudyNoteRequest(@NotBlank(message = "חסר סוג ההערה") String kind,
                                   @NotBlank(message = "חסר למה ההערה שייכת") String targetType,
                                   Long targetId,
                                   @Size(max = 500, message = "הכותרת ארוכה מדי") String targetTitle,
                                   @Size(max = 255) String subject,
                                   @Size(max = 255) String topic,
                                   @Size(max = 255) String subTopic,
                                   @Size(max = 1000, message = "הקטע המודגש ארוך מדי (עד 1000 תווים)") String quote,
                                   @Size(max = 2000, message = "ההערה ארוכה מדי (עד 2000 תווים)") String body) {}
}
