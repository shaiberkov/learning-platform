// [s04] Pure helpers for the flashcards and the study notebook screens.

export const NOTE_KINDS = {
    NOTE: "הערה",
    HIGHLIGHT: "הדגשה",
    BOOKMARK: "סימנייה",
};

export const TARGET_LABELS = {
    QUESTION: "שאלה",
    LESSON: "שיעור",
    REVIEW_ITEM: "מחברת הטעויות",
    FLASHCARD: "כרטיסייה",
};

export const SOURCE_LABELS = {
    MANUAL: "ידנית",
    AI_TEXT: "AI מסיכום",
    AI_IMAGE: "AI מתמונה",
};

/** Whole days from today to an ISO date (yyyy-MM-dd); negative when overdue. */
export function daysUntil(isoDate, today = new Date()) {
    if (!isoDate) return null;
    const [y, m, d] = isoDate.split("-").map(Number);
    const due = Date.UTC(y, m - 1, d);
    const now = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());
    return Math.round((due - now) / 86_400_000);
}

/** "לחזרה היום", "מחר", "בעוד 6 ימים" for a card's next review. */
export function dueLabel(isoDate, today = new Date()) {
    const days = daysUntil(isoDate, today);
    if (days === null) return "";
    if (days <= 0) return "לחזרה היום";
    if (days === 1) return "מחר";
    return `בעוד ${days} ימים`;
}

/**
 * Groups notes by the question or lesson they belong to, keeping the server's order (newest first)
 * for both the groups and the notes inside each group.
 */
export function groupNotesByTarget(notes) {
    const groups = new Map();
    for (const note of notes || []) {
        const key = `${note.targetType}:${note.targetId}`;
        if (!groups.has(key)) {
            groups.set(key, {
                key,
                targetType: note.targetType,
                targetId: note.targetId,
                title: note.targetTitle || TARGET_LABELS[note.targetType] || "",
                subject: note.subject,
                topic: note.topic,
                subTopic: note.subTopic,
                notes: [],
            });
        }
        groups.get(key).notes.push(note);
    }
    return [...groups.values()];
}

/** Where a note's target lives in the app, or null when there is no page for it. */
export function targetLink(group) {
    if (group.targetType === "LESSON" && group.targetId) return `/lessons/${group.targetId}`;
    if (group.targetType === "QUESTION" && group.subject && group.topic && group.subTopic) {
        return `/exercises/${group.subject}/${group.topic}/${group.subTopic}`;
    }
    if (group.targetType === "REVIEW_ITEM") return "/mistakes-notebook";
    if (group.targetType === "FLASHCARD") return "/flashcards";
    return null;
}

/** Keeps only drafts with both sides filled, trimmed, ready for the save request. */
export function cleanDrafts(drafts) {
    return (drafts || [])
        .map((d) => ({ front: (d.front || "").trim(), back: (d.back || "").trim() }))
        .filter((d) => d.front && d.back);
}
