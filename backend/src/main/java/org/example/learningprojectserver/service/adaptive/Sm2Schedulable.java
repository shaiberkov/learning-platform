package org.example.learningprojectserver.service.adaptive;

import java.time.LocalDate;

/**
 * Anything scheduled with SM-2: mistakes-notebook items ({@code ReviewItemEntity}) and a student's
 * flashcards ({@code FlashcardReviewEntity}) share the same fields and the same {@link Sm2Scheduler}.
 */
public interface Sm2Schedulable {
    Long getId();
    double getEasiness();
    void setEasiness(double easiness);
    int getRepetitions();
    void setRepetitions(int repetitions);
    int getIntervalDays();
    void setIntervalDays(int intervalDays);
    int getLapses();
    void setLapses(int lapses);
    int getReviewCount();
    void setReviewCount(int reviewCount);
    LocalDate getDueDate();
    void setDueDate(LocalDate dueDate);
}
