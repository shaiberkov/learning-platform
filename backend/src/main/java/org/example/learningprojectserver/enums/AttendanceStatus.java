package org.example.learningprojectserver.enums;

public enum AttendanceStatus {
    PRESENT("נוכח/ת"),
    ABSENT("נעדר/ת"),
    LATE("איחור"),
    EXCUSED("היעדרות מוצדקת");

    private final String label;

    AttendanceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
