// Pure helpers for the exam simulation and the "what do I need?" calculator screens.

/** "mm:ss" (or "h:mm:ss" from an hour up) for a number of seconds. */
export function formatClock(totalSeconds) {
    const s = Math.max(0, Math.floor(Number(totalSeconds) || 0));
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = String(s % 60).padStart(2, "0");
    return h > 0 ? `${h}:${String(m).padStart(2, "0")}:${sec}` : `${m}:${sec}`;
}

/** Answered and total questions over all sections. */
export function countAnswered(sections, answers) {
    let total = 0;
    let answered = 0;
    for (const section of sections || []) {
        for (const q of section.questions || []) {
            total += 1;
            const value = answers?.[q.id];
            if (typeof value === "string" && value.trim() !== "") answered += 1;
        }
    }
    return { answered, total };
}

/**
 * The target typed by the student: a number from 0 to 100, or an error in Hebrew.
 * Accepts a comma as the decimal separator.
 */
export function parseTarget(raw) {
    const text = String(raw ?? "").trim().replace(",", ".");
    if (text === "") return { error: "יש להזין ציון יעד" };
    if (!/^\d+(\.\d+)?$/.test(text)) return { error: "היעד חייב להיות מספר" };
    const value = Number(text);
    if (value < 0 || value > 100) return { error: "היעד חייב להיות בין 0 ל-100" };
    return { value };
}

/** Theme tone and Hebrew label for a calculator status from the server. */
export function targetStatusInfo(status) {
    switch (status) {
        case "POSSIBLE":
            return { tone: "accent", label: "אפשרי" };
        case "ALREADY_SECURED":
        case "NO_REMAINING_MET":
            return { tone: "mint", label: "היעד מובטח" };
        case "IMPOSSIBLE":
        case "NO_REMAINING_MISSED":
            return { tone: "danger", label: "לא בר השגה" };
        default:
            return { tone: "neutral", label: "אין נתונים" };
    }
}

/** How hard a required average is, for the chip next to it. */
export function difficultyLabel(requiredPercent) {
    if (requiredPercent === null || requiredPercent === undefined) return null;
    if (requiredPercent <= 0) return "מובטח";
    if (requiredPercent < 70) return "בהישג יד";
    if (requiredPercent < 90) return "דורש מאמץ";
    return "מאתגר מאוד";
}

/** A score shown without a trailing ".0". */
export function formatScore(value) {
    if (value === null || value === undefined || Number.isNaN(Number(value))) return "—";
    const n = Number(value);
    return Number.isInteger(n) ? String(n) : n.toFixed(1);
}
