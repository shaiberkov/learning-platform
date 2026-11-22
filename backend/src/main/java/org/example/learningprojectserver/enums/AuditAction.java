package org.example.learningprojectserver.enums;

/** Sensitive actions written to the audit log, each with the Hebrew label the UI shows. */
public enum AuditAction {
    USER_PROMOTED_TO_MANAGER("מינוי משתמש למנהל בית ספר"),
    MANAGER_ASSIGNED_TO_SCHOOL("שיוך מנהל לבית ספר"),
    MANAGER_REMOVED_FROM_SCHOOL("הסרת מנהל מבית ספר"),
    USER_ASSIGNED_AS_TEACHER("שיוך משתמש כמורה"),
    TEACHER_REMOVED_FROM_SCHOOL("הסרת מורה מבית הספר"),
    TEACHER_ASSIGNED_TO_CLASSES("שיבוץ מורה לכיתות"),
    TEACHER_REMOVED_FROM_CLASS("הסרת מורה מכיתה"),
    STUDENT_ASSIGNED_TO_CLASS("שיבוץ תלמיד לכיתה"),
    STUDENT_CLASS_CHANGED("העברת תלמיד כיתה"),
    SCHOOL_GRADES_ADDED("הוספת שכבות"),
    SCHOOL_GRADES_REMOVED("הסרת שכבות"),
    LESSON_ADDED("הוספת שיעור למערכת השעות"),
    TEACHER_TEST_SCORE_RECORDED("רישום ציון במבחן"),
    IMPERSONATION_STARTED("תחילת התחזות"),
    IMPERSONATION_STOPPED("סיום התחזות"),
    FEATURE_FLAG_CHANGED("שינוי הגדרת פיצ'ר לבית ספר");

    private final String label;

    AuditAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
