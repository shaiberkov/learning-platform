// [s09] Pure helpers for the quests and portfolio screens.

/** Percent of a quest's chapters already done (0-100). */
export function questPercent(chaptersDone, chapterCount) {
    if (!chapterCount || chapterCount <= 0) return 0;
    const done = Math.min(Math.max(chaptersDone || 0, 0), chapterCount);
    return Math.round((done / chapterCount) * 100);
}

const STATUS_LABELS = {
    NOT_STARTED: "עוד לא התחיל",
    IN_PROGRESS: "בדרך",
    COMPLETED: "הושלם",
};

export function questStatusLabel(status) {
    return STATUS_LABELS[status] || "";
}

const TONE_CLASSES = {
    accent: "bg-accent-soft",
    mint: "bg-mint-soft",
    amber: "bg-amber-soft",
};

/** Theme-token background class for a room tone from the server catalog. */
export function roomToneClass(tone) {
    return TONE_CLASSES[tone] || TONE_CLASSES.accent;
}

/**
 * Each school year with the change in average from the previous year that has an average.
 * delta is null for the first year with an average and for years without one.
 */
export function withYearDeltas(years) {
    let previous = null;
    return (years || []).map((year) => {
        const avg = typeof year.averageScore === "number" ? year.averageScore : null;
        const delta = avg !== null && previous !== null ? Math.round((avg - previous) * 10) / 10 : null;
        if (avg !== null) previous = avg;
        return { ...year, delta };
    });
}

/** A short Hebrew sentence about the trend from the first to the last year with an average. */
export function trendMessage(years) {
    const scored = (years || []).filter((y) => typeof y.averageScore === "number");
    if (scored.length < 2) return "";
    const first = scored[0];
    const last = scored[scored.length - 1];
    const diff = Math.round((last.averageScore - first.averageScore) * 10) / 10;
    if (diff > 0) return `עלייה של ${diff} נקודות בממוצע מכיתה ${first.gradeName} ועד היום`;
    if (diff < 0) return `ירידה של ${Math.abs(diff)} נקודות בממוצע מכיתה ${first.gradeName}. אפשר לחזור לעלות!`;
    return `ממוצע יציב מכיתה ${first.gradeName} ועד היום`;
}

/** dd/MM/yyyy from an ISO date or date-time ("2026-11-23" or "2026-11-23T10:00:00"). */
export function formatDate(iso) {
    const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso || "");
    return match ? `${match[3]}/${match[2]}/${match[1]}` : "";
}

/** The address of the public page for a share token. */
export function shareUrl(origin, token) {
    if (!token) return "";
    return `${String(origin || "").replace(/\/+$/, "")}/shared-portfolio/${encodeURIComponent(token)}`;
}

// Print only the portfolio: hide the app chrome and the controls.
export const PORTFOLIO_PRINT_CSS = `@media print {
  #navbar, #sidebar, footer, .skip-link, .no-print { display: none !important; }
  main { padding-top: 0 !important; }
  .glass { box-shadow: none !important; backdrop-filter: none !important; }
}`;
