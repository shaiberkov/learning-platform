// [s06] Display rules of the mistake-type diagnosis. The server sends a MistakeType name
// (backend enums/MistakeType.java); each type gets its own label, chip and follow-up practice.

export const MISTAKE_TYPES = {
    CALCULATION: {
        label: "טעות חישוב (רשלנות)",
        chip: "chip--amber",
        action: "SELF_CHECK",
        actionLabel: "בדוק את עצמך",
        advice: "השיטה כנראה נכונה. פתרו שוב לאט, בדקו סימנים, העתקה וספרות, והציבו את התשובה בחזרה בשאלה.",
    },
    CONCEPTUAL: {
        label: "טעות מושגית",
        chip: "chip--danger",
        action: "LESSON",
        actionLabel: "חזרה לשיעור",
        advice: "כדאי לחזור לשיעור על הנושא ולהבין את הרעיון לפני שמתרגלים שוב.",
    },
    MISREAD: {
        label: "קריאה לא נכונה של השאלה",
        chip: "chip--mint",
        action: "SELF_CHECK",
        actionLabel: "קרא שוב ובדוק",
        advice: "קראו את השאלה שוב לאט, סמנו מה בדיוק שואלים ובאיזו צורה צריך לכתוב את התשובה.",
    },
    UNCLASSIFIED: {
        label: "עוד לא אובחן",
        chip: "",
        action: "AI",
        actionLabel: "אבחון עם AI",
        advice: "לא הצלחנו לזהות את סוג הטעות לפי התשובה. אפשר לבקש מה-AI לנתח אותה.",
    },
};

export const TYPE_ORDER = ["CALCULATION", "CONCEPTUAL", "MISREAD", "UNCLASSIFIED"];

export const mistakeType = (type) => MISTAKE_TYPES[type] || MISTAKE_TYPES.UNCLASSIFIED;

/** The counts per type as rows in a fixed order, with each type's share of the total (whole percent). */
export function summarizeCounts(counts = {}) {
    const total = TYPE_ORDER.reduce((sum, type) => sum + (Number(counts[type]) || 0), 0);
    return TYPE_ORDER.map((type) => {
        const count = Number(counts[type]) || 0;
        return { type, ...mistakeType(type), count, percent: total ? Math.round((count / total) * 100) : 0 };
    });
}

/** One sentence for the student about their most frequent mistake type. */
export function dominantAdvice(dominantType) {
    if (!dominantType || !MISTAKE_TYPES[dominantType] || dominantType === "UNCLASSIFIED") {
        return "עוד אין מספיק טעויות מאובחנות כדי לזהות דפוס.";
    }
    const t = MISTAKE_TYPES[dominantType];
    return `רוב הטעויות שלך הן ${t.label}. ${t.advice}`;
}

/** Score band of an "explain it to me" attempt: label and chip class. */
export function scoreBand(score) {
    if (score === null || score === undefined) return { label: "לא נבדק", chip: "" };
    if (score >= 80) return { label: "הבנה מלאה", chip: "chip--mint" };
    if (score >= 50) return { label: "הבנה חלקית", chip: "chip--amber" };
    return { label: "עדיין יש חורים", chip: "chip--danger" };
}
