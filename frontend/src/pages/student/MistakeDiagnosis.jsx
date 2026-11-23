import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { FaStethoscope } from "react-icons/fa";
import EmptyState from "../../components/ui/EmptyState.jsx";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import { analyzeMistakeWithAi, fetchMistakeDiagnosis, selfCheckMistake } from "../../features/diagnosis/diagnosisApi.js";
import { dominantAdvice, mistakeType, summarizeCounts } from "../../features/diagnosis/mistakeTypes.js";

// [s06] Mistake-type diagnosis: every mistake in the notebook is tagged as a calculation slip,
// a conceptual mistake or a misread question, and each type gets its own practice.

function SelfCheck({ item }) {
    const [answer, setAnswer] = useState("");
    const [result, setResult] = useState(null);
    const [sending, setSending] = useState(false);

    const submit = async (e) => {
        e.preventDefault();
        if (!answer.trim()) return;
        setSending(true);
        setResult(await selfCheckMistake(item.id, answer));
        setSending(false);
    };

    return (
        <form onSubmit={submit} className="space-y-2">
            <label className="label" htmlFor={`self-check-${item.id}`}>פתרו שוב וכתבו את התשובה</label>
            <div className="flex gap-2">
                <input
                    id={`self-check-${item.id}`}
                    type="text"
                    dir="ltr"
                    className="field num"
                    value={answer}
                    onChange={(e) => {
                        setAnswer(e.target.value);
                        setResult(null);
                    }}
                />
                <button type="submit" className="btn btn-primary btn-sm" disabled={!answer.trim() || sending}>בדוק</button>
            </div>
            {result && (
                <p role="status" className={`notice ${result.success && result.data.correct ? "notice--mint" : "notice--danger"}`}>
                    {!result.success
                        ? result.errorCode
                        : result.data.correct
                            ? "נכון! הפעם בדקת את עצמך עד הסוף."
                            : <>עדיין לא. התשובה הנכונה: <span className="num" dir="ltr">{result.data.correctAnswer}</span></>}
                </p>
            )}
        </form>
    );
}

function MistakeCard({ item, onUpdated }) {
    const type = mistakeType(item.errorType);
    const [open, setOpen] = useState(false);
    const [notice, setNotice] = useState("");
    const [analyzing, setAnalyzing] = useState(false);

    const askAi = async () => {
        setAnalyzing(true);
        const res = await analyzeMistakeWithAi(item.id);
        setAnalyzing(false);
        if (res.success) {
            setNotice("");
            onUpdated(res.data);
        } else {
            setNotice(res.errorCode);
        }
    };

    return (
        <article className="row-card !block space-y-3">
            <div className="flex flex-wrap items-center gap-2">
                <span className={`chip ${type.chip}`}>{type.label}</span>
                {item.errorTypeSource === "AI" && <span className="chip">אבחון AI</span>}
                {item.topic && <span className="chip">{item.topic}</span>}
                {item.subTopic && <span className="chip">{item.subTopic}</span>}
            </div>
            <p className="num text-ink break-words" dir="ltr">{item.questionText}</p>
            {item.givenAnswer && (
                <p className="text-sm muted">התשובה שלך: <span className="num" dir="ltr">{item.givenAnswer}</span></p>
            )}
            {item.errorNote && <p className="text-sm">{item.errorNote}</p>}
            <p className="text-sm faint">{type.advice}</p>
            <div className="flex flex-wrap gap-2">
                {type.action === "LESSON" && <Link to="/lessons" className="btn btn-primary btn-sm">{type.actionLabel}</Link>}
                {type.action === "SELF_CHECK" && (
                    <button type="button" className="btn btn-primary btn-sm" aria-expanded={open} onClick={() => setOpen(!open)}>
                        {type.actionLabel}
                    </button>
                )}
                <button type="button" className="btn btn-ghost btn-sm" onClick={askAi} disabled={analyzing || !item.givenAnswer}>
                    {analyzing ? "מנתח..." : item.errorTypeSource === "AI" ? "נתח שוב עם AI" : "אבחון עם AI"}
                </button>
            </div>
            {notice && <p role="status" className="notice notice--danger">{notice}</p>}
            {open && type.action === "SELF_CHECK" && <SelfCheck item={item} />}
        </article>
    );
}

export default function MistakeDiagnosis() {
    const [data, setData] = useState(null);
    const [error, setError] = useState("");
    const [filter, setFilter] = useState("");

    useEffect(() => {
        let cancelled = false;
        fetchMistakeDiagnosis().then((res) => {
            if (cancelled) return;
            if (res.success) setData(res.data);
            else setError(res.errorCode);
        });
        return () => {
            cancelled = true;
        };
    }, []);

    const updateItem = (updated) => {
        setData((prev) => {
            const old = prev.items.find((i) => i.id === updated.id);
            const counts = { ...prev.counts };
            if (old && old.errorType !== updated.errorType) {
                counts[old.errorType] = Math.max(0, (counts[old.errorType] || 0) - 1);
                counts[updated.errorType] = (counts[updated.errorType] || 0) + 1;
            }
            return { ...prev, counts, items: prev.items.map((i) => (i.id === updated.id ? updated : i)) };
        });
    };

    const rows = data ? summarizeCounts(data.counts) : [];
    const items = data ? data.items.filter((i) => !filter || i.errorType === filter) : [];

    return (
        <div className="page page--wide space-y-4 sm:space-y-5" dir="rtl">
            <section className="glass p-6 sm:p-8 reveal space-y-3">
                <div className="flex items-center gap-3">
                    <span className="icon-badge icon-badge--amber"><FaStethoscope aria-hidden="true" /></span>
                    <div>
                        <span className="eyebrow">Mistake diagnosis · אבחון טעויות</span>
                        <h1 className="display text-3xl sm:text-4xl mt-1">איזה סוג טעויות אני עושה?</h1>
                    </div>
                </div>
                <p className="muted leading-relaxed">
                    כל טעות במחברת הטעויות מקבלת סוג: טעות חישוב, טעות מושגית או קריאה לא נכונה של השאלה. לכל סוג יש תרגול
                    משלו: בטעות חישוב בודקים את עצמנו, בטעות מושגית חוזרים לשיעור.
                </p>
                {data && <p className="notice">{dominantAdvice(data.dominantType)}</p>}
            </section>

            {error && <p className="notice notice--danger">{error}</p>}

            {!data && !error && <SkeletonList rows={3} />}

            {data && (
                <>
                    <section aria-label="סיכום לפי סוג טעות" className="grid grid-cols-2 md:grid-cols-4 gap-4 reveal" style={{ "--i": 1 }}>
                        {rows.map((row) => (
                            <button
                                key={row.type}
                                type="button"
                                aria-pressed={filter === row.type}
                                onClick={() => setFilter(filter === row.type ? "" : row.type)}
                                className={`glass tile p-5 text-start choice ${filter === row.type ? "is-active" : ""}`}
                            >
                                <span className="faint text-xs">{row.label}</span>
                                <div className="num text-4xl font-semibold text-ink mt-1">{row.count}</div>
                                <span className="num text-xs text-ink-3">{row.percent}%</span>
                            </button>
                        ))}
                    </section>

                    <section className="glass p-6 sm:p-8 space-y-3 reveal" style={{ "--i": 2 }}>
                        <div className="flex flex-wrap items-center justify-between gap-2">
                            <h2 className="title text-xl">הטעויות שלי</h2>
                            {filter && (
                                <button type="button" className="btn btn-ghost btn-sm" onClick={() => setFilter("")}>הצג הכל</button>
                            )}
                        </div>
                        {items.length === 0 ? (
                            <EmptyState
                                title={data.total ? "אין טעויות מהסוג הזה" : "עוד אין טעויות במחברת"}
                                body="שאלות שתטעה בהן בתרגול או במבחן יופיעו כאן עם סוג הטעות."
                                actionLabel="לתרגול"
                                actionTo="/question-practice-selector"
                            />
                        ) : (
                            items.map((item) => <MistakeCard key={item.id} item={item} onUpdated={updateItem} />)
                        )}
                    </section>
                </>
            )}
        </div>
    );
}
