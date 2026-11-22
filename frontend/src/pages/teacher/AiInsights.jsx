import { useEffect, useState } from "react";
import { FaLightbulb } from "react-icons/fa";
import FormShell from "../../components/ui/FormShell.jsx";
import { AiUsagePanel, FlaggedMessagesPanel } from "../../components/ai/AiOversightPanels.jsx";
import { staffGet, staffPost } from "../../components/ai/aiApi.js";
import { AI_INSIGHTS_GENERATE, AI_INSIGHTS_LATEST } from "../../constants/pages.constants.js";

const formatDate = (value) => (value ? value.slice(0, 10).split("-").reverse().join("/") : "");

// Teacher: the weekly AI summary (written every Sunday morning, or on demand) with the numbers behind it.
export default function AiInsights() {
    const [insight, setInsight] = useState(null);
    const [message, setMessage] = useState("");
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        let cancelled = false;
        staffGet(AI_INSIGHTS_LATEST).then((res) => {
            if (cancelled) return;
            setInsight(res.success ? res.data : null);
            setMessage(res.errorCode || "");
        });
        return () => {
            cancelled = true;
        };
    }, []);

    const generate = async () => {
        setBusy(true);
        const res = await staffPost(AI_INSIGHTS_GENERATE);
        setBusy(false);
        if (res.success) {
            setInsight(res.data);
            setMessage("");
        } else {
            setMessage(res.errorCode);
        }
    };

    const stats = insight?.stats;

    return (
        <div className="space-y-5">
            <FormShell eyebrow="AI · תובנות שבועיות" title="מה קרה בכיתה השבוע" icon={<FaLightbulb />} width="wide">
                <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="faint text-sm">
                        {insight
                            ? `תקופה: ${formatDate(insight.periodStart)} – ${formatDate(insight.periodEnd)}`
                            : "הסיכום נוצר אוטומטית בכל יום ראשון בבוקר."}
                    </p>
                    <button className="btn btn-primary btn-sm" disabled={busy} onClick={generate}>
                        {busy ? "מחשב..." : "צור עכשיו"}
                    </button>
                </div>
                {message && <p className="notice">{message}</p>}

                {insight && (
                    <>
                        <article className="glass--sunk rounded-2xl p-4 whitespace-pre-wrap leading-relaxed text-sm">
                            {!insight.aiGenerated && <span className="chip chip--amber mb-2 inline-block">ללא סיכום AI</span>}
                            <div>{insight.summary}</div>
                            {insight.aiGenerated && (
                                <p className="faint text-xs mt-3">
                                    בסיכום התלמידים מופיעים בכינוי (S1, S2...). הטבלה למטה מקשרת כינוי לשם.
                                </p>
                            )}
                        </article>

                        <section className="space-y-2">
                            <h3 className="title text-lg">תלמידים שמתקשים לפי נושא</h3>
                            {stats.strugglingStudents.length === 0 ? (
                                <p className="faint text-sm">אין תלמידים מתחת ל-50% בנושא כלשהו.</p>
                            ) : (
                                <div className="overflow-x-auto">
                                    <table className="data-table">
                                        <thead>
                                        <tr><th>כינוי</th><th>תלמיד/ה</th><th>נושא</th><th>תשובות</th><th>הצלחה</th></tr>
                                        </thead>
                                        <tbody>
                                        {stats.strugglingStudents.map((s) => (
                                            <tr key={`${s.studentUserId}-${s.topic}`}>
                                                <td className="num">{s.label}</td>
                                                <td>{s.studentName}</td>
                                                <td>{s.topic}</td>
                                                <td className="num">{s.answered}</td>
                                                <td className="num">{s.accuracyPct}%</td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </section>

                        <section className="space-y-2">
                            <h3 className="title text-lg">שאלות חשודות</h3>
                            <p className="faint text-xs">שאלות שרוב התלמידים בחרו בהן באותה תשובה שגויה – כדאי לבדוק את מפתח התשובה או טעות משותפת.</p>
                            {stats.suspiciousQuestions.length === 0 ? (
                                <p className="faint text-sm">לא נמצאו שאלות כאלה.</p>
                            ) : (
                                <div className="overflow-x-auto">
                                    <table className="data-table">
                                        <thead>
                                        <tr><th>שאלה</th><th>תשובה במפתח</th><th>התשובה השגויה הנפוצה</th><th>שיעור</th></tr>
                                        </thead>
                                        <tbody>
                                        {stats.suspiciousQuestions.map((q) => (
                                            <tr key={q.questionId}>
                                                <td>{q.questionText}</td>
                                                <td className="num" dir="ltr">{q.correctAnswer}</td>
                                                <td className="num" dir="ltr">{q.commonWrongAnswer}</td>
                                                <td className="num">{q.commonWrongCount}/{q.answered} ({q.commonWrongPct}%)</td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </section>

                        {stats.topics.length > 0 && (
                            <section className="flex flex-wrap gap-2">
                                {stats.topics.map((t) => (
                                    <span key={t.topic} className={`chip ${t.accuracyPct < 50 ? "chip--danger" : t.accuracyPct < 75 ? "chip--amber" : "chip--mint"}`}>
                                        {t.topic}: <span className="num">{t.accuracyPct}%</span>
                                    </span>
                                ))}
                            </section>
                        )}
                    </>
                )}
            </FormShell>

            <div className="page page--wide grid grid-cols-1 lg:grid-cols-2 gap-5">
                <AiUsagePanel />
                <FlaggedMessagesPanel />
            </div>
        </div>
    );
}
