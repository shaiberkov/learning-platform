package org.example.learningprojectserver.enums;

/**
 * How an exam question is answered and graded.
 * TEXT is the legacy generated question (exact text match on {@code answer}).
 */
public enum QuestionKind {
    TEXT,
    MULTI_SELECT,
    NUMERIC,
    MATCHING,
    ORDERING,
    SHORT_OPEN;

    /** Null or blank means TEXT; an unknown value returns null. */
    public static QuestionKind of(String value) {
        if (value == null || value.isBlank()) {
            return TEXT;
        }
        try {
            return QuestionKind.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
