package org.example.learningprojectserver.enums;

/** Gradebook assignment categories; each carries a weight per teacher and class. */
public enum GradeCategory {
    EXAM("מבחנים", 60),
    HOMEWORK("שיעורי בית", 40),
    QUIZ("בחנים", 0),
    PROJECT("עבודות", 0);

    private final String label;
    private final int defaultWeight;

    GradeCategory(String label, int defaultWeight) {
        this.label = label;
        this.defaultWeight = defaultWeight;
    }

    public String getLabel() {
        return label;
    }

    public int getDefaultWeight() {
        return defaultWeight;
    }
}
