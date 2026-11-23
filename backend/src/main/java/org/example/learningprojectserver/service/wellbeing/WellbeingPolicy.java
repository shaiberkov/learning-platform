package org.example.learningprojectserver.service.wellbeing;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * The rules of the reflection journal and the pre-exam check-in ([s10]), kept free of Spring
 * and the database so they can be tested on their own.
 */
public final class WellbeingPolicy {

    private WellbeingPolicy() {}

    /** A distress alert needs this many distress check-ins ... */
    public static final int DISTRESS_THRESHOLD = 3;
    /** ... among the student's latest this many check-ins. */
    public static final int DISTRESS_WINDOW = 5;
    /** After an alert, no new alert for the same student for this many days. */
    public static final int ALERT_COOLDOWN_DAYS = 14;
    /** A teacher test within this many days counts as an upcoming exam for the check-in. */
    public static final int EXAM_HORIZON_DAYS = 3;
    /** Version of the privacy text shown next to the consent checkbox. */
    public static final String PRIVACY_POLICY_VERSION = "2026-11";

    public enum GoalType {
        FREE, PRACTICE_QUESTIONS, XP, ACTIVE_DAYS;

        public boolean measurable() {
            return this != FREE;
        }

        public static GoalType parse(String value) {
            if (value == null) return null;
            try {
                return GoalType.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public enum Mood {
        GREAT(false), OK(false), NERVOUS(false), STRESSED(true), OVERWHELMED(true);

        private final boolean distress;

        Mood(boolean distress) {
            this.distress = distress;
        }

        public boolean distress() {
            return distress;
        }

        public static Mood parse(String value) {
            if (value == null) return null;
            try {
                return Mood.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /** ACHIEVED, IN_PROGRESS (week still running), NOT_ACHIEVED, or UNMARKED (free goal, week over, no mark). */
    public enum GoalStatus { ACHIEVED, IN_PROGRESS, NOT_ACHIEVED, UNMARKED }

    /** The Sunday that opens the week of the given day. */
    public static LocalDate weekStart(LocalDate day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    /**
     * Status of a weekly goal. A measurable goal is achieved once progress reaches the target,
     * whatever the student marked; a free goal follows the student's own mark.
     */
    public static GoalStatus goalStatus(GoalType type, Integer target, int progress, Boolean selfMark, boolean weekOver) {
        if (type != null && type.measurable() && target != null && target > 0) {
            if (progress >= target) return GoalStatus.ACHIEVED;
            return weekOver ? GoalStatus.NOT_ACHIEVED : GoalStatus.IN_PROGRESS;
        }
        if (Boolean.TRUE.equals(selfMark)) return GoalStatus.ACHIEVED;
        if (Boolean.FALSE.equals(selfMark)) return weekOver ? GoalStatus.NOT_ACHIEVED : GoalStatus.IN_PROGRESS;
        return weekOver ? GoalStatus.UNMARKED : GoalStatus.IN_PROGRESS;
    }

    /** Distress check-ins among the latest DISTRESS_WINDOW (newest first). */
    public static int distressCount(List<Boolean> latestFirst) {
        return (int) latestFirst.stream().limit(DISTRESS_WINDOW).filter(Boolean::booleanValue).count();
    }

    public static boolean distressPattern(List<Boolean> latestFirst) {
        return distressCount(latestFirst) >= DISTRESS_THRESHOLD;
    }

    /** Alert only with consent, on a pattern, and not again within the cooldown. */
    public static boolean shouldAlert(boolean pattern, boolean consented, boolean alertedRecently) {
        return pattern && consented && !alertedRecently;
    }

    /** What the screen offers after a check-in: a breathing exercise for stress, a tip otherwise. */
    public static Suggestion suggestionFor(Mood mood, int seed) {
        if (mood == Mood.GREAT || mood == Mood.OK) {
            String[] tips = CALM_TIPS;
            return new Suggestion("TIP", "שומרים על הקצב", tips[Math.floorMod(seed, tips.length)]);
        }
        String[] tips = STRESS_TIPS;
        String intro = mood == Mood.NERVOUS ? "קצת התרגשות לפני מבחן זה טבעי." : "לחץ לפני מבחן מוכר לכולנו, ואפשר להרגיע אותו.";
        return new Suggestion("BREATHING", "תרגיל נשימה קצר",
                intro + " נסה/י נשימת קופסה: שאיפה 4 שניות, החזקה 4, נשיפה 4, החזקה 4. " + tips[Math.floorMod(seed, tips.length)]);
    }

    public record Suggestion(String kind, String title, String text) {}

    private static final String[] CALM_TIPS = {
            "לפני המבחן עבור/י על סיכום קצר אחד ולא על כל החומר.",
            "שינה טובה בלילה שלפני המבחן שווה יותר משעה נוספת של לימוד.",
            "במבחן, התחל/י מהשאלות שאת/ה בטוח/ה בהן כדי לצבור ביטחון.",
    };

    private static final String[] STRESS_TIPS = {
            "כתוב/י על דף מה מדאיג אותך: מחשבה שנכתבה תופסת פחות מקום בראש.",
            "חלק/י את החזרה לקטעים של 20 דקות עם הפסקה קצרה ביניהם.",
            "אם נתקעת בשאלה במבחן, דלג/י עליה וחזור/י אליה בסוף.",
            "דבר/י עם מישהו שסומכים עליו: חבר, הורה, מורה או יועצת.",
    };
}
