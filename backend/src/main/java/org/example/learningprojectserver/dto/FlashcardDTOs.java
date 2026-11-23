package org.example.learningprojectserver.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** API shapes of the flashcards: the student's deck, AI drafts, reviews and the class's shared cards. */
public final class FlashcardDTOs {

    private FlashcardDTOs() {}

    /**
     * A card as one student sees it. {@code mine} is false for a classmate's shared card;
     * {@code inDeck} says whether this student reviews it, and then {@code dueDate} is their next review.
     */
    public record FlashcardDTO(Long id, String front, String back, String subject, String topic, String source,
                               boolean shared, boolean mine, boolean inDeck, LocalDate dueDate,
                               int reviewCount, int lapses) {}

    public record FlashcardDraftDTO(String front, String back) {}

    /** Cards proposed by the AI (or by the line-splitting fallback), not saved yet. */
    public record FlashcardDraftsDTO(List<FlashcardDraftDTO> cards, boolean aiGenerated, String notice) {}

    public record FlashcardReviewResultDTO(boolean knew, LocalDate nextDueDate, int intervalDays, long remainingDue) {}

    public record FlashcardSummaryDTO(long dueToday, long total) {}

    /** Either {@code text} (a pasted summary) or {@code imageDataUrl} (a photo of a book page). */
    public record FlashcardGenerateRequest(@Size(max = 4000, message = "הסיכום ארוך מדי (עד 4000 תווים)") String text,
                                           String imageDataUrl, String subject, String topic, Integer count) {}

    public record FlashcardInput(@NotBlank(message = "חסר צד השאלה בכרטיסייה")
                                 @Size(max = 500, message = "צד השאלה ארוך מדי (עד 500 תווים)") String front,
                                 @NotBlank(message = "חסר צד התשובה בכרטיסייה")
                                 @Size(max = 1000, message = "צד התשובה ארוך מדי (עד 1000 תווים)") String back) {}

    /** Saves one or more cards to the student's deck; {@code source} is MANUAL, AI_TEXT or AI_IMAGE. */
    public record FlashcardSaveRequest(@NotEmpty(message = "אין כרטיסיות לשמירה")
                                       @Size(max = 30, message = "אפשר לשמור עד 30 כרטיסיות בכל פעם")
                                       List<@Valid FlashcardInput> cards,
                                       @Size(max = 255) String subject, @Size(max = 255) String topic,
                                       String source, boolean share) {}
}
