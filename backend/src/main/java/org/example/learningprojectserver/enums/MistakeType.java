package org.example.learningprojectserver.enums;

/**
 * Why a student got a question wrong. Each type gets a different follow-up: a calculation slip gets
 * "check yourself", a conceptual mistake sends the student back to the lesson, a misread question
 * asks them to read it again.
 */
public enum MistakeType {
    /** A slip in the arithmetic: wrong sign, off by a little, swapped digits, misplaced decimal point. */
    CALCULATION,
    /** The method or the idea is wrong. */
    CONCEPTUAL,
    /** The question was misread: a number copied from it, the right values in the wrong order or format. */
    MISREAD,
    /** Not enough information to tell (blank or non-numeric answer); the AI can analyze it. */
    UNCLASSIFIED
}
