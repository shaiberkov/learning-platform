package org.example.learningprojectserver.enums;

/**
 * Features a system admin can turn on or off per school (SchoolFeatureFlagEntity).
 * A school with no stored row for a feature gets its default.
 */
public enum Feature {
    AI_CHAT("צ'אט AI לתלמידים", true),
    PRACTICE_TESTS("מבחני תרגול", true),
    TEACHER_TESTS("מבחנים של מורים", true),
    MESSAGES("שליחת הודעות", true);

    private final String label;
    private final boolean defaultEnabled;

    Feature(String label, boolean defaultEnabled) {
        this.label = label;
        this.defaultEnabled = defaultEnabled;
    }

    public String getLabel() {
        return label;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }
}
