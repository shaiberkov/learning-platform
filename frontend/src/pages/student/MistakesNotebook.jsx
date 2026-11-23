import { useContext, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { FaBookOpen, FaCheck, FaRedo } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import OrbitLoader from "../../components/ui/OrbitLoader.jsx";
import { answerReviewItem, fetchDueReviewItems, fetchReviewSummary } from "../../features/adaptive/adaptiveApi.js";
import { mistakeType } from "../../features/diagnosis/mistakeTypes.js";

const SOURCE_LABELS = {
    PRACTICE: "תרגול שאלות",
    PRACTICE_TEST: "מבחן תרגול",
    TEACHER_TEST: "מבחן מורה",
};

const formatDate = (iso) => {
    if (!iso) return "";
    const [y, m, d] = iso.split("-");
    return `${d}/${m}/${y}`;
};

function MistakesNotebook() {
    const { user } = useContext(UserContext);
    const [summary, setSummary] = useState(null);
    const [items, setItems] = useState([]);
    const [index, setIndex] = useState(0);
    const [answer, setAnswer] = useState("");
    const [result, setResult] = useState(null);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(true);
    const [sending, setSending] = useState(false);

    const load = async () => {
        setLoading(true);
        const [s, due] = await Promise.all([fetchReviewSummary(user.userId), fetchDueReviewItems(user.userId)]);
        if (s.success) setSummary(s.data);
        if (due.success) {
            setItems(due.data);
            setIndex(0);
            setError(null);
        } else {
            setError(due.errorCode);
        }
        setResult(null);
        setAnswer("");
        setLoading(false);
    };

    useEffect(() => {
        if (user?.userId) load();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [user?.userId]);

    const current = items[index];

    const submit = async (e) => {
        e.preventDefault();
        if (!current || !answer.trim()) return;
        setSending(true);
        const res = await answerReviewItem(user.userId, current.id, answer);
        setSending(false);
        if (!res.success) {
            setError(res.errorCode);
            return;
        }
        setResult(res.data);
        setSummary((prev) => (prev ? { ...prev, dueToday: res.data.remainingDue } : prev));
    };

    const next = () => {
        setResult(null);
        setAnswer("");
        setIndex((i) => i + 1);
    };

    if (loading) {
        return <OrbitLoader title="טוען את מחברת הטעויות..." subtitle="מחפשים את השאלות שמחכות לחזרה היום" />;
    }

    const finished = !current;

    return (
        <div className="page page--wide space-y-4 sm:space-y-5" dir="rtl">
            <div className="grid grid-cols-1 md:grid-cols-12 gap-4 sm:gap-5">
                <div className="md:col-span-7 glass p-6 sm:p-8 reveal">
                    <div className="flex items-center gap-3">
                        <span className="icon-badge icon-badge--amber"><FaBookOpen /></span>
                        <div>
                            <span className="eyebrow">Mistakes notebook · מחברת טעויות</span>
                            <h1 className="display text-3xl sm:text-4xl mt-1">חזרה על טעויות</h1>
                        </div>
                    </div>
                    <p className="muted mt-4 leading-relaxed">
                        כל שאלה שטעית בה נכנסת לכאן. שאלה שעונים עליה נכון חוזרת בעוד יום, אחר כך בעוד שבוע ואז ברווחים
                        הולכים וגדלים; טעות מחזירה אותה להתחלה.
                    </p>
                </div>
                <div className="md:col-span-5 grid grid-cols-2 gap-4 reveal" style={{ "--i": 1 }}>
                    <div className="glass tile p-5">
                        <span className="faint text-xs">לחזרה היום</span>
                        <div className="num text-4xl font-semibold text-ink mt-1">{summary?.dueToday ?? 0}</div>
                    </div>
                    <div className="glass tile p-5">
                        <span className="faint text-xs">מחר</span>
                        <div className="num text-4xl font-semibold text-ink-2 mt-1">{summary?.dueTomorrow ?? 0}</div>
                    </div>
                    <div className="glass tile p-5">
                        <span className="faint text-xs">במחברת</span>
                        <div className="num text-4xl font-semibold text-ink-2 mt-1">{summary?.total ?? 0}</div>
                    </div>
                    <div className="glass tile p-5">
                        <span className="faint text-xs">נלמדו (3 ברצף)</span>
                        <div className="num text-4xl font-semibold text-mint mt-1">{summary?.learned ?? 0}</div>
                    </div>
                </div>
            </div>

            {error && <p className="notice notice--danger">{error}</p>}

            {finished ? (
                <div className="glass p-8 sm:p-10 text-center space-y-4 reveal">
                    <span className="icon-badge icon-badge--mint mx-auto"><FaCheck /></span>
                    <h2 className="title text-2xl">אין שאלות לחזרה כרגע</h2>
                    <p className="muted">
                        {summary?.total
                            ? "סיימת את החזרות של היום. השאלות הבאות יחכו לך כאן במועד שלהן."
                            : "המחברת ריקה. שאלות שתטעה בהן בתרגול או במבחן יופיעו כאן."}
                    </p>
                    <div className="flex justify-center gap-2 pt-2">
                        <Link to="/skill-map" className="btn btn-primary">למפת המיומנויות</Link>
                        <button onClick={load} className="btn btn-ghost"><FaRedo /> רענן</button>
                    </div>
                </div>
            ) : (
                <div className="glass p-6 sm:p-8 space-y-5 reveal" style={{ "--i": 2 }}>
                    <div className="flex flex-wrap items-center justify-between gap-2">
                        <span className="num text-xs text-ink-3">
                            {index + 1} / {items.length}
                        </span>
                        <div className="flex flex-wrap gap-2">
                            <span className="chip">{current.topic}</span>
                            <span className="chip">{current.subTopic}</span>
                            <span className="chip">{SOURCE_LABELS[current.source] || current.source}</span>
                            {current.lapses > 0 && <span className="chip">טעויות: <span className="num">{current.lapses}</span></span>}
                            {/* [s06] mistake type, with the practice it calls for on the diagnosis page */}
                            {current.errorType && (
                                <Link to="/mistake-diagnosis" className={`chip ${mistakeType(current.errorType).chip}`}>
                                    {mistakeType(current.errorType).label} · {mistakeType(current.errorType).actionLabel}
                                </Link>
                            )}
                        </div>
                    </div>

                    <div className="glass--sunk p-5 sm:p-6" dir="ltr">
                        <h2 className="num text-xl sm:text-2xl font-medium text-ink leading-relaxed break-words">
                            {current.questionText}
                        </h2>
                    </div>

                    {!result ? (
                        <form onSubmit={submit} className="space-y-3">
                            <input
                                type="text"
                                placeholder="תשובה"
                                value={answer}
                                onChange={(e) => setAnswer(e.target.value)}
                                className="field num !text-lg"
                                autoFocus
                            />
                            <button type="submit" disabled={!answer.trim() || sending} className="btn btn-primary btn-block">
                                בדוק
                            </button>
                        </form>
                    ) : (
                        <div className="space-y-4">
                            <p className={`notice ${result.correct ? "notice--mint" : "notice--danger"}`}>
                                {result.correct ? "נכון! " : "לא נכון. "}
                                התשובה: <span className="num" dir="ltr">{result.correctAnswer}</span>
                                {" · "}
                                {result.correct
                                    ? `השאלה תחזור בעוד ${result.intervalDays} ימים (${formatDate(result.nextDueDate)}).`
                                    : `השאלה תחזור מחר (${formatDate(result.nextDueDate)}).`}
                            </p>
                            <button onClick={next} className="btn btn-primary btn-block">
                                {index + 1 < items.length ? "לשאלה הבאה" : "סיום"}
                            </button>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

export default MistakesNotebook;
