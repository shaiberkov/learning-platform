// Pure helpers for the duel and daily challenge screens ([s03]).

/** TanStack Query key of today's challenge, shared by the page and the dashboard tile. */
export const DAILY_CHALLENGE_QUERY_KEY = ["daily-challenge", "today"];

export const DUEL_MODE_LABELS = {
    COMPETITIVE: "אחד על אחד",
    COOP: "שיתופי נגד המערכת",
};

/** The duel from the viewer's side: who is "me" and who is the other player. */
export function duelSides(duel, userId) {
    const iAmChallenger = duel.challengerId === userId;
    return {
        iAmChallenger,
        myName: iAmChallenger ? duel.challengerName : duel.opponentName,
        theirName: iAmChallenger ? duel.opponentName : duel.challengerName,
        myAnswered: iAmChallenger ? duel.challengerAnswered : duel.opponentAnswered,
        myCorrect: iAmChallenger ? duel.challengerCorrect : duel.opponentCorrect,
        theirAnswered: iAmChallenger ? duel.opponentAnswered : duel.challengerAnswered,
        theirCorrect: iAmChallenger ? duel.opponentCorrect : duel.challengerCorrect,
    };
}

/** Short Hebrew status of a duel for the list. */
export function duelStatusLabel(duel, userId) {
    switch (duel.status) {
        case "PENDING":
            return duel.challengerId === userId ? "ממתין לאישור" : "הוזמנת";
        case "ACTIVE":
            return "פעיל";
        case "FINISHED":
            return duelResult(duel, userId).label;
        case "DECLINED":
            return "בוטל";
        default:
            return "";
    }
}

/** The result of a finished duel from the viewer's side: label and tone (mint, danger or neutral). */
export function duelResult(duel, userId) {
    const outcome = duel.outcome;
    if (duel.mode === "COOP") {
        if (outcome === "TEAM") return { label: "ניצחתם את המערכת!", tone: "mint" };
        if (outcome === "SYSTEM") return { label: "המערכת ניצחה הפעם", tone: "danger" };
        return { label: "תיקו מול המערכת", tone: "neutral" };
    }
    if (outcome === "DRAW" || !outcome) return { label: "תיקו", tone: "neutral" };
    const iWon = (outcome === "CHALLENGER") === (duel.challengerId === userId);
    return iWon ? { label: "ניצחת!", tone: "mint" } : { label: "הפסדת הפעם", tone: "danger" };
}

/** The question the viewer should answer next: the first unanswered one, or null when all are answered. */
export function nextQuestion(questions) {
    return (questions || []).find((q) => q.myAnswer == null) ?? null;
}

/** Seconds as m:ss. Negative values show as 0:00. */
export function formatClock(seconds) {
    const s = Math.max(0, Math.floor(seconds || 0));
    return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;
}

/** Hebrew text for a streak count. */
export function streakLabel(streak) {
    if (!streak) return "עוד אין רצף";
    if (streak === 1) return "יום אחד ברצף";
    return `${streak} ימים ברצף`;
}
