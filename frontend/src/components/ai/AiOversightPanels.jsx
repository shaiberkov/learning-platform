import { useEffect, useState } from "react";
import { AI_SAFETY_FLAGGED, AI_USAGE } from "../../constants/pages.constants.js";
import { staffGet } from "./aiApi.js";

const FEATURE_LABEL = {
    TUTOR: "מורה-עזר (צ'אט)",
    TUTOR_PHOTO: "מורה-עזר (צילום)",
    TRANSCRIBE: "המרת קול לטקסט",
    QUESTION_GEN: "יצירת שאלות",
    GRADING: "בדיקה לפי מחוון",
    INSIGHTS: "סיכומים שבועיים",
};

const FLAG_LABEL = {
    DISTRESS: "מצוקה",
    INAPPROPRIATE: "תוכן לא הולם",
    INJECTION: "ניסיון לעקוף את הכללים",
    OUTPUT_INAPPROPRIATE: "תשובת AI שנחסמה",
};

/** AI requests, tokens and estimated cost for the caller's school, per feature. */
export function AiUsagePanel() {
    const [days, setDays] = useState(30);
    const [rows, setRows] = useState([]);
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;
        staffGet(AI_USAGE, { days }).then((res) => {
            if (cancelled) return;
            setRows(res.success ? res.data : []);
            setError(res.success ? "" : res.errorCode);
        });
        return () => {
            cancelled = true;
        };
    }, [days]);

    const total = rows.reduce((sum, r) => sum + (r.costUsd || 0), 0);
    const requests = rows.reduce((sum, r) => sum + (r.requests || 0), 0);

    return (
        <section className="glass p-5 space-y-3" dir="rtl">
            <div className="flex items-center justify-between gap-2">
                <div>
                    <span className="eyebrow">AI · שימוש ועלות</span>
                    <h3 className="title text-lg mt-1">שימוש ב-AI בבית הספר</h3>
                </div>
                <select className="field max-w-[9rem]" value={days} onChange={(e) => setDays(Number(e.target.value))}>
                    <option value={7}>7 ימים</option>
                    <option value={30}>30 ימים</option>
                    <option value={90}>90 ימים</option>
                </select>
            </div>
            {error && <p className="notice notice--danger">{error}</p>}
            <p className="num text-sm">
                {requests} בקשות · עלות משוערת ${total.toFixed(2)}
            </p>
            {rows.length > 0 && (
                <div className="overflow-x-auto">
                    <table className="data-table">
                        <thead>
                        <tr><th>שימוש</th><th>בקשות</th><th>טוקנים (קלט/פלט)</th><th>עלות ($)</th></tr>
                        </thead>
                        <tbody>
                        {rows.map((r) => (
                            <tr key={r.feature}>
                                <td>{FEATURE_LABEL[r.feature] || r.feature}</td>
                                <td className="num">{r.requests}</td>
                                <td className="num">{r.promptTokens} / {r.completionTokens}</td>
                                <td className="num">{(r.costUsd || 0).toFixed(3)}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </section>
    );
}

/** Tutor messages the safety filter flagged in the caller's school (the only conversations staff can see). */
export function FlaggedMessagesPanel() {
    const [rows, setRows] = useState([]);
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;
        staffGet(AI_SAFETY_FLAGGED).then((res) => {
            if (cancelled) return;
            setRows(res.success ? res.data : []);
            setError(res.success ? "" : res.errorCode);
        });
        return () => {
            cancelled = true;
        };
    }, []);

    return (
        <section className="glass p-5 space-y-3" dir="rtl">
            <span className="eyebrow">AI · בטיחות</span>
            <h3 className="title text-lg">הודעות שסומנו על ידי מערכת הבטיחות</h3>
            <p className="faint text-xs">מוצגות רק הודעות שסומנו. שאר השיחות פרטיות לתלמיד ונמחקות אוטומטית.</p>
            {error && <p className="notice notice--danger">{error}</p>}
            {!error && rows.length === 0 && <p className="faint text-sm">אין הודעות מסומנות.</p>}
            {rows.map((m) => (
                <article key={m.id} className="row-card !block space-y-1 text-sm">
                    <div className="flex flex-wrap gap-2 items-center">
                        <span className={`chip ${m.flagReason === "DISTRESS" ? "chip--danger" : "chip--amber"}`}>
                            {FLAG_LABEL[m.flagReason] || m.flagReason}
                        </span>
                        <span className="num faint">ת"ז {m.userId}</span>
                        <span className="num faint">{m.createdAt?.replace("T", " ").slice(0, 16)}</span>
                    </div>
                    <p className="whitespace-pre-wrap">{m.content}</p>
                </article>
            ))}
        </section>
    );
}
