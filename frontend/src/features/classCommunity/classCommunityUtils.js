// Pure helpers for the class forum and the student-authored questions (no React, no requests).

export const MIN_DISTRACTORS = 2;
export const MAX_DISTRACTORS = 5;
/** Characters typed before the "similar questions" lookup runs. */
export const MIN_SIMILAR_QUERY = 4;

export const PEER_STATUS = {
    PENDING: { label: "ממתינה לאישור המורה", chip: "chip--amber" },
    APPROVED: { label: "אושרה ✓ בתרגול הכיתה", chip: "chip--mint" },
    REJECTED: { label: "הוחזרה לתיקון", chip: "chip--danger" },
};

/** Trimmed, non-empty, de-duplicated (case-insensitive) wrong options. */
export function cleanDistractors(distractors = []) {
    const out = [];
    for (const raw of distractors) {
        const value = (raw ?? "").replace(/\n/g, " ").trim();
        if (value && !out.some((o) => o.toLowerCase() === value.toLowerCase())) out.push(value);
    }
    return out;
}

/** Same rules as PeerQuestionService.validate on the server; returns a Hebrew message or null. */
export function validatePeerQuestion(form) {
    const question = (form?.questionText ?? "").trim();
    const correct = (form?.correctAnswer ?? "").trim();
    if (!question) return "יש לכתוב את נוסח השאלה";
    if (!correct) return "יש לכתוב את התשובה הנכונה";
    const distractors = cleanDistractors(form?.distractors);
    if (distractors.some((d) => d.toLowerCase() === correct.toLowerCase())) {
        return "תשובה שגויה לא יכולה להיות זהה לתשובה הנכונה";
    }
    if (distractors.length < MIN_DISTRACTORS) {
        return `יש לכתוב לפחות ${MIN_DISTRACTORS} תשובות שגויות שונות (מסיחים)`;
    }
    if (distractors.length > MAX_DISTRACTORS) return `אפשר לכתוב עד ${MAX_DISTRACTORS} תשובות שגויות`;
    return null;
}

/** The request body for /peer-questions/submit. */
export function toSubmitPayload(form) {
    return {
        questionText: (form.questionText ?? "").trim(),
        correctAnswer: (form.correctAnswer ?? "").trim(),
        distractors: cleanDistractors(form.distractors),
        topic: (form.topic ?? "").trim() || null,
        explanation: (form.explanation ?? "").trim() || null,
    };
}

/** The text sent to the similar-threads lookup, or "" when too short to be worth asking. */
export function similarQuery(title, exerciseText) {
    const text = [title, exerciseText].map((s) => (s ?? "").trim()).filter(Boolean).join(" ");
    return text.length >= MIN_SIMILAR_QUERY ? text : "";
}

/** Percent of correct classmate answers, or null before any answer. */
export function successRate(attempts, correctAttempts) {
    if (!attempts) return null;
    return Math.round((100 * (correctAttempts ?? 0)) / attempts);
}

/** "dd/mm/yyyy hh:mm" in Hebrew locale, "" for a missing value. */
export function formatWhen(value) {
    if (!value) return "";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "";
    return date.toLocaleString("he-IL", { dateStyle: "short", timeStyle: "short" });
}
