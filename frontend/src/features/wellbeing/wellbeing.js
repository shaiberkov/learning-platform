// [s10] Pure helpers for the reflection journal and the pre-exam check-in.

export const MOODS = [
    { value: "GREAT", label: "מצוין" },
    { value: "OK", label: "בסדר" },
    { value: "NERVOUS", label: "קצת מתוח/ה" },
    { value: "STRESSED", label: "לחוץ/ה" },
    { value: "OVERWHELMED", label: "מוצף/ת" },
];

export const GOAL_TYPES = [
    { value: "FREE", label: "יעד חופשי", hint: "את/ה מסמנ/ת בסוף השבוע אם הושג", unit: null },
    { value: "PRACTICE_QUESTIONS", label: "מספר שאלות תרגול", hint: "נספר אוטומטית מהתרגול", unit: "שאלות" },
    { value: "XP", label: "נקודות ניסיון (XP)", hint: "נספר אוטומטית מכל הפעילות", unit: "XP" },
    { value: "ACTIVE_DAYS", label: "ימי למידה בשבוע", hint: "ימים שבהם היתה פעילות", unit: "ימים" },
];

export const STATUS_LABEL = {
    ACHIEVED: { text: "היעד הושג", tone: "chip--mint" },
    IN_PROGRESS: { text: "בתהליך", tone: "" },
    NOT_ACHIEVED: { text: "לא הושג הפעם", tone: "chip--amber" },
    UNMARKED: { text: "לא סומן", tone: "" },
};

export function moodLabel(value) {
    return MOODS.find((m) => m.value === value)?.label ?? value;
}

export function goalType(value) {
    return GOAL_TYPES.find((g) => g.value === value) ?? GOAL_TYPES[0];
}

/** Percent of a measurable goal reached, 0-100; null for a free goal or a missing target. */
export function goalPercent(progress, target) {
    if (!target || target <= 0) return null;
    return Math.max(0, Math.min(100, Math.round(((progress ?? 0) / target) * 100)));
}

/** Box breathing: inhale, hold, exhale, hold, PHASE_SECONDS each. */
export const PHASE_SECONDS = 4;
export const BREATHING_PHASES = [
    { key: "inhale", label: "שאיפה", scale: 1 },
    { key: "hold-in", label: "החזקה", scale: 1 },
    { key: "exhale", label: "נשיפה", scale: 0.55 },
    { key: "hold-out", label: "החזקה", scale: 0.55 },
];

/**
 * Where a box-breathing exercise stands after elapsedSeconds: the phase, the seconds left in it
 * and the completed cycles.
 */
export function breathingPhase(elapsedSeconds) {
    const t = Math.max(0, Math.floor(elapsedSeconds));
    const cycleLength = PHASE_SECONDS * BREATHING_PHASES.length;
    const inCycle = t % cycleLength;
    const index = Math.floor(inCycle / PHASE_SECONDS);
    return {
        phase: BREATHING_PHASES[index],
        secondsLeft: PHASE_SECONDS - (inCycle % PHASE_SECONDS),
        cycles: Math.floor(t / cycleLength),
    };
}

/** "dd/MM" for an ISO date (yyyy-MM-dd) without time-zone shifts. */
export function shortDate(iso) {
    if (!iso) return "";
    const [, m, d] = String(iso).split("-");
    return d && m ? `${d}/${m}` : String(iso);
}
