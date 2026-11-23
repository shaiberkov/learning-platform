package org.example.learningprojectserver.service.attendance;

/**
 * When an absence is unusual enough to tell the school manager. Pure rules, tested alone.
 * A student: every STUDENT_THRESHOLD-th absence within WINDOW_DAYS (3, 6, 9 and so on), checked only when
 * a save newly marks the student absent, so saving the same sheet again does not alert twice.
 * A lesson: when at least CLASS_RATIO of a class of at least MIN_CLASS_SIZE is absent, checked only when
 * a save crosses that line.
 */
public final class AbsenceAlertPolicy {

    public static final int STUDENT_THRESHOLD = 3;
    public static final int WINDOW_DAYS = 30;
    public static final double CLASS_RATIO = 0.5;
    public static final int MIN_CLASS_SIZE = 4;

    private AbsenceAlertPolicy() {}

    public static boolean studentAlert(long absencesInWindow) {
        return absencesInWindow >= STUDENT_THRESHOLD && absencesInWindow % STUDENT_THRESHOLD == 0;
    }

    public static boolean classAlert(int absentBefore, int absentAfter, int classSize) {
        if (classSize < MIN_CLASS_SIZE) return false;
        return absentAfter >= CLASS_RATIO * classSize && absentBefore < CLASS_RATIO * classSize;
    }

    /** Highlighted in the report: STUDENT_THRESHOLD absences or more, or a fifth of the marked lessons. */
    public static boolean unusualInReport(long absent, long total) {
        return absent >= STUDENT_THRESHOLD || (total >= 5 && absent * 5 >= total);
    }
}
