import { useCallback, useContext, useEffect, useState } from "react";
import { FaBolt, FaCheck, FaPlay, FaStop, FaPaperPlane } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import { useNotificationContext } from "../../context/NotificationContext.jsx";
import { apiGet, apiPost } from "../../Utils/authRequest.js";
import useTeacherClasses from "../../components/teacher/useTeacherClasses.js";
import {
    CLOSE_LIVE_QUESTION, END_LIVE_QUIZ, GET_LIVE_QUIZ_RESULTS, LIVE_QUIZ_BASE_PATH, PUSH_LIVE_QUESTION,
    QUESTION_BANK_BASE_PATH, SEARCH_BANK_QUESTIONS, START_LIVE_QUIZ,
} from "../../constants/pages.constants.js";

const POLL_MS = 3000;

// Teacher's screen for a live quiz: meant to be projected in class.
function LiveQuizHost() {
    const { user } = useContext(UserContext);
    const { liveQuizResults } = useNotificationContext();
    const teacherId = user?.userId;
    const { classes, error: classesError } = useTeacherClasses(teacherId);
    const [quiz, setQuiz] = useState(null);
    const [message, setMessage] = useState(null);
    const [summary, setSummary] = useState(null);

    const refresh = useCallback(async () => {
        if (!teacherId) return;
        const res = await apiGet(`${LIVE_QUIZ_BASE_PATH}${GET_LIVE_QUIZ_RESULTS}`, { teacherId });
        if (res.success) setQuiz(res.data);
    }, [teacherId]);

    // Resume a running quiz after a reload, then keep polling while one is open (SSE also pushes).
    useEffect(() => { refresh(); }, [refresh]);
    useEffect(() => {
        if (!quiz?.active) return;
        const id = setInterval(refresh, POLL_MS);
        return () => clearInterval(id);
    }, [quiz?.active, refresh]);
    useEffect(() => {
        if (liveQuizResults && liveQuizResults.code === quiz?.code) setQuiz(liveQuizResults);
    }, [liveQuizResults, quiz?.code]);

    const act = async (path, body, params) => {
        const res = await apiPost(`${LIVE_QUIZ_BASE_PATH}${path}`, body, { teacherId, ...params });
        setMessage({ ok: res.success, text: res.errorCode });
        return res;
    };

    const start = async (classRoomId) => {
        setSummary(null);
        const res = await act(START_LIVE_QUIZ, null, { classRoomId });
        if (res.success) setQuiz(res.data);
    };

    const end = async () => {
        if (!window.confirm("לסיים את החידון?")) return;
        const res = await act(END_LIVE_QUIZ, null, { code: quiz.code });
        if (res.success) {
            setSummary(res.data);
            setQuiz(null);
        }
    };

    const close = async () => {
        const res = await act(CLOSE_LIVE_QUESTION, null, { code: quiz.code });
        if (res.success) setQuiz(res.data);
    };

    const push = async (question) => {
        const res = await act(PUSH_LIVE_QUESTION, question, { code: quiz.code });
        if (res.success) setQuiz(res.data);
        return res.success;
    };

    return (
        <div className="page page--wide" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-6 reveal">
                <div className="panel-head flex-wrap">
                    <div className="flex items-center gap-4">
                        <span className="icon-badge icon-badge--mint"><FaBolt /></span>
                        <div>
                            <span className="eyebrow">Live quiz · חידון בכיתה</span>
                            <h2 className="title text-2xl sm:text-3xl mt-1">חידון חי בכיתה</h2>
                        </div>
                    </div>
                    {quiz?.active && (
                        <button className="btn btn-danger btn-sm" onClick={end}><FaStop /> סיום החידון</button>
                    )}
                </div>

                {classesError && <div className="notice">{classesError}</div>}
                {message?.text && <div className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</div>}

                {!quiz?.active && (
                    <section className="space-y-3">
                        <p className="muted">בחרו כיתה כדי לפתוח חידון. התלמידים נכנסים מהטלפון דרך "חידון בכיתה" בתפריט.</p>
                        <div className="flex flex-wrap gap-2">
                            {classes.map((c) => (
                                <button key={c.classRoomId} className="choice !w-auto" onClick={() => start(c.classRoomId)}>
                                    <FaPlay /> {c.className} <span className="muted text-xs">({c.studentCount} תלמידים)</span>
                                </button>
                            ))}
                        </div>
                        {summary && <Scoreboard rows={summary.scoreboard} title="תוצאות החידון האחרון" />}
                    </section>
                )}

                {quiz?.active && (
                    <div className="grid grid-cols-1 lg:grid-cols-5 gap-5">
                        <div className="lg:col-span-3 space-y-5">
                            <div className="flex flex-wrap items-center gap-3">
                                <span className="chip">כיתה {quiz.className}</span>
                                <span className="chip">קוד <span className="num text-base">{quiz.code}</span></span>
                                {quiz.questionNumber > 0 && <span className="chip">שאלה <span className="num">{quiz.questionNumber}</span></span>}
                            </div>

                            {quiz.questionText ? (
                                <CurrentQuestion quiz={quiz} onClose={close} />
                            ) : (
                                <p className="muted">החידון פתוח. שלחו את השאלה הראשונה.</p>
                            )}

                            <QuestionComposer teacherId={teacherId} onPush={push} />
                        </div>
                        <div className="lg:col-span-2">
                            <Scoreboard rows={quiz.scoreboard} title="טבלת ניקוד" />
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

function CurrentQuestion({ quiz, onClose }) {
    const counts = Object.entries(quiz.answerCounts || {});
    const max = Math.max(1, ...counts.map(([, n]) => n));
    const progress = quiz.totalStudents ? Math.round((100 * quiz.answeredCount) / quiz.totalStudents) : 0;
    return (
        <section className="glass glass--sunk p-5 space-y-4">
            <p className="display text-2xl sm:text-3xl whitespace-pre-wrap break-words">{quiz.questionText}</p>
            <div className="space-y-1">
                <div className="flex justify-between text-sm">
                    <span>ענו <span className="num">{quiz.answeredCount}</span> מתוך <span className="num">{quiz.totalStudents}</span></span>
                    {!quiz.questionOpen && <span>נכונות: <span className="num">{quiz.correctCount}</span></span>}
                </div>
                <div className="h-2 rounded-full bg-surface-strong overflow-hidden">
                    <div className="h-full bg-accent transition-all duration-500" style={{ width: `${progress}%` }} />
                </div>
            </div>
            <ul className="space-y-2">
                {counts.map(([answer, n]) => {
                    const correct = answer === quiz.correctAnswer;
                    return (
                        <li key={answer} className="space-y-1">
                            <div className="flex justify-between text-sm gap-2">
                                <span className={`break-words ${!quiz.questionOpen && correct ? "font-bold text-mint" : ""}`}>
                                    {!quiz.questionOpen && correct && <FaCheck className="inline ml-1" />}{answer}
                                </span>
                                <span className="num">{n}</span>
                            </div>
                            <div className="h-3 rounded-full bg-surface-strong overflow-hidden">
                                <div className={`h-full transition-all duration-500 ${!quiz.questionOpen && correct ? "bg-mint" : "bg-accent"}`}
                                     style={{ width: `${(100 * n) / max}%` }} />
                            </div>
                        </li>
                    );
                })}
                {counts.length === 0 && <li className="muted text-sm">עדיין אין תשובות.</li>}
            </ul>
            <div className="flex items-center justify-between flex-wrap gap-2">
                <span className="muted text-sm">התשובה הנכונה (רק אצלך): <strong>{quiz.correctAnswer}</strong></span>
                {quiz.questionOpen
                    ? <button className="btn btn-primary btn-sm" onClick={onClose}>סגירת השאלה והצגת התשובה</button>
                    : <span className="chip chip--mint">השאלה נסגרה</span>}
            </div>
        </section>
    );
}

function QuestionComposer({ teacherId, onPush }) {
    const [mode, setMode] = useState("bank");
    const [text, setText] = useState("");
    const [results, setResults] = useState([]);
    const [typed, setTyped] = useState({ questionText: "", optionsText: "", answer: "" });

    const searchBank = useCallback(async (q) => {
        const params = { teacherId };
        if (q) params.text = q;
        const res = await apiGet(`${QUESTION_BANK_BASE_PATH}${SEARCH_BANK_QUESTIONS}`, params);
        if (res.success) setResults((res.data || []).slice(0, 20));
    }, [teacherId]);

    useEffect(() => { if (mode === "bank") searchBank(""); }, [mode, searchBank]);

    const pushTyped = async (e) => {
        e.preventDefault();
        const ok = await onPush({
            questionText: typed.questionText, answer: typed.answer,
            options: typed.optionsText.split("\n").map((o) => o.trim()).filter(Boolean),
        });
        if (ok) setTyped({ questionText: "", optionsText: "", answer: "" });
    };

    return (
        <section className="space-y-3">
            <div className="flex gap-2">
                <button className={`choice !w-auto ${mode === "bank" ? "is-active" : ""}`} onClick={() => setMode("bank")}>שאלה מהמאגר</button>
                <button className={`choice !w-auto ${mode === "typed" ? "is-active" : ""}`} onClick={() => setMode("typed")}>שאלה חדשה</button>
            </div>

            {mode === "bank" ? (
                <div className="space-y-2">
                    <form className="flex gap-2" onSubmit={(e) => { e.preventDefault(); searchBank(text); }}>
                        <input className="field" value={text} onChange={(e) => setText(e.target.value)} placeholder="חיפוש במאגר" />
                        <button className="btn btn-ghost btn-sm" type="submit">חיפוש</button>
                    </form>
                    <ul className="space-y-2 max-h-80 overflow-y-auto">
                        {results.map((q) => (
                            <li key={q.id} className="row-card flex items-center justify-between gap-3">
                                <div className="min-w-0">
                                    <p className="truncate font-medium">{q.questionText}</p>
                                    <p className="muted text-xs">
                                        {[q.topic, q.level, q.options?.length ? "אמריקאית" : "פתוחה"].filter(Boolean).join(" · ")}
                                        {q.successRate != null && <> · הצלחה <span className="num">{q.successRate}%</span></>}
                                    </p>
                                </div>
                                <button className="btn btn-primary btn-sm shrink-0" onClick={() => onPush({ bankQuestionId: q.id })}>
                                    <FaPaperPlane /> שליחה
                                </button>
                            </li>
                        ))}
                        {results.length === 0 && <li className="muted text-sm">לא נמצאו שאלות במאגר.</li>}
                    </ul>
                </div>
            ) : (
                <form className="space-y-3" onSubmit={pushTyped}>
                    <textarea className="field min-h-20" placeholder="נוסח השאלה" value={typed.questionText} maxLength={2000}
                              onChange={(e) => setTyped({ ...typed, questionText: e.target.value })} required />
                    <textarea className="field min-h-16" placeholder={"אפשרויות, אחת בכל שורה (ריק = שאלה פתוחה)"} value={typed.optionsText}
                              onChange={(e) => setTyped({ ...typed, optionsText: e.target.value })} />
                    <input className="field" placeholder="התשובה הנכונה" value={typed.answer} maxLength={500}
                           onChange={(e) => setTyped({ ...typed, answer: e.target.value })} required />
                    <button className="btn btn-primary btn-sm" type="submit"><FaPaperPlane /> שליחה לכיתה</button>
                </form>
            )}
        </section>
    );
}

function Scoreboard({ rows, title }) {
    if (!rows?.length) return null;
    return (
        <section className="glass glass--sunk p-4 space-y-2">
            <h3 className="title text-lg">{title}</h3>
            <ol className="space-y-1">
                {rows.map((r, i) => (
                    <li key={`${r.studentName}-${i}`} className="flex justify-between text-sm gap-2">
                        <span><span className="num muted">{i + 1}.</span> {r.studentName}</span>
                        <span className="num">{r.correct}/{r.answered}</span>
                    </li>
                ))}
            </ol>
        </section>
    );
}

export default LiveQuizHost;
