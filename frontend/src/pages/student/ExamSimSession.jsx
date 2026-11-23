import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { FiClock, FiSend } from "react-icons/fi";
import { FaExclamationCircle, FaGraduationCap } from "react-icons/fa";
import ProgressRing from "../../components/ui/ProgressRing.jsx";
import OrbitLoader from "../../components/ui/OrbitLoader.jsx";
import MathText from "../../components/exam/MathText.jsx";
import { errorMessage } from "../../lib/api.js";
import { saveSimulation, startSimulation, submitSimulation } from "../../features/examsim/examSimApi.js";
import { countAnswered, formatClock } from "../../features/examsim/examSim.js";
import SimResult from "../../features/examsim/SimResult.jsx";

const AUTOSAVE_MS = 10000;

// A past exam sat under full conditions, with the same flow as ExamSession: the server sets the
// deadline, answers are autosaved, and the exam submits itself when the time is up.
export default function ExamSimSession() {
    const { examId } = useParams();
    const navigate = useNavigate();

    const [phase, setPhase] = useState("loading");
    const [message, setMessage] = useState("");
    const [attempt, setAttempt] = useState(null);
    const [answers, setAnswers] = useState({});
    const [remaining, setRemaining] = useState(0);
    const [result, setResult] = useState(null);
    const [showConfirm, setShowConfirm] = useState(false);

    const endAt = useRef(0);
    const dirty = useRef(false);
    const answersRef = useRef({});
    const submitting = useRef(false);
    const submitButtonRef = useRef(null);

    const syncClock = (seconds) => {
        endAt.current = Date.now() + seconds * 1000;
        setRemaining(seconds);
    };

    // Started once per mount (a ref guard, so a double effect run cannot open two attempts).
    const started = useRef(null);
    useEffect(() => {
        if (started.current === examId) return;
        started.current = examId;
        (async () => {
            try {
                const res = await startSimulation(examId);
                const saved = res.data.savedAnswers || {};
                answersRef.current = saved;
                setAnswers(saved);
                setAttempt(res.data);
                setMessage(res.errorCode || "");
                syncClock(res.data.remainingSeconds);
                setPhase("running");
            } catch (error) {
                setMessage(errorMessage(error, "לא ניתן להתחיל את הסימולציה"));
                setPhase("error");
            }
        })();
    }, [examId]);

    const save = useCallback(async () => {
        if (!attempt || !dirty.current || submitting.current) return;
        dirty.current = false;
        try {
            const res = await saveSimulation(attempt.attemptId, answersRef.current);
            syncClock(res.data);
        } catch (error) {
            dirty.current = true;
            if (error?.message) setMessage(error.message);
        }
    }, [attempt]);

    const submit = useCallback(async () => {
        if (!attempt || submitting.current) return;
        submitting.current = true;
        setPhase("submitting");
        try {
            const res = await submitSimulation(attempt.attemptId, answersRef.current);
            setMessage(res.errorCode || "");
            if (res.data) {
                setResult(res.data);
                setPhase("result");
            } else {
                setPhase("error");
            }
        } catch {
            submitting.current = false;
            setMessage("ההגשה נכשלה, נסו שוב");
            setPhase("running");
        }
    }, [attempt]);

    // Countdown against the server deadline; submits at zero.
    useEffect(() => {
        if (phase !== "running") return;
        const timer = setInterval(() => {
            const left = Math.max(0, Math.ceil((endAt.current - Date.now()) / 1000));
            setRemaining(left);
            if (left === 0) {
                clearInterval(timer);
                submit();
            }
        }, 1000);
        return () => clearInterval(timer);
    }, [phase, submit]);

    useEffect(() => {
        if (phase !== "running") return;
        const timer = setInterval(save, AUTOSAVE_MS);
        const onHide = () => document.visibilityState === "hidden" && save();
        document.addEventListener("visibilitychange", onHide);
        return () => {
            clearInterval(timer);
            document.removeEventListener("visibilitychange", onHide);
        };
    }, [phase, save]);

    // The confirmation dialog closes with Esc and gives the focus back to the submit button.
    useEffect(() => {
        if (!showConfirm) return;
        const returnTo = submitButtonRef.current;
        const onKey = (e) => e.key === "Escape" && setShowConfirm(false);
        document.addEventListener("keydown", onKey);
        return () => {
            document.removeEventListener("keydown", onKey);
            returnTo?.focus();
        };
    }, [showConfirm]);

    const setAnswer = (questionId, value) => {
        const next = { ...answersRef.current, [questionId]: value };
        answersRef.current = next;
        dirty.current = true;
        setAnswers(next);
    };

    if (phase === "loading") {
        return <OrbitLoader title="מכין את הסימולציה..." subtitle="השרת מפעיל את השעון" />;
    }
    if (phase === "submitting") {
        return <OrbitLoader title="בודק לפי המחוון..." subtitle="הסימולציה נבדקת בשרת" />;
    }
    if (phase === "error") {
        return (
            <div className="page page--medium" dir="rtl">
                <div className="glass p-8 space-y-5 reveal text-center">
                    <span className="icon-badge icon-badge--amber mx-auto"><FaExclamationCircle aria-hidden="true" /></span>
                    <p className="title text-xl" role="alert">{message}</p>
                    <button type="button" className="btn btn-primary" onClick={() => navigate("/exam-simulations")}>חזרה למאגר הבחינות</button>
                </div>
            </div>
        );
    }
    if (phase === "result") {
        return <SimResult result={result} message={message} />;
    }

    const { answered, total } = countAnswered(attempt.sections, answers);
    const progress = total ? (answered / total) * 100 : 0;

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="grid grid-cols-1 md:grid-cols-12 gap-5">
                <div className="md:col-span-7 glass p-6 sm:p-7 reveal">
                    <div className="flex items-center gap-3 mb-4">
                        <span className="icon-badge icon-badge--mint"><FaGraduationCap aria-hidden="true" /></span>
                        <div>
                            <span className="eyebrow">Simulation · סימולציה בתנאי מבחן</span>
                            <h1 className="title text-2xl mt-0.5">{attempt.title}</h1>
                        </div>
                    </div>
                    <p className="faint text-sm">
                        משך המבחן <span className="num">{attempt.durationMinutes}</span> דקות, נמדד בשרת. התשובות נשמרות אוטומטית,
                        והמבחן יוגש מעצמו בתום הזמן. הבדיקה לפי מחוון: ניקוד חלקי לכל רכיב נכון בתשובה.
                    </p>
                    {attempt.predictedScore !== null && attempt.predictedScore !== undefined && (
                        <p className="muted text-sm mt-2">
                            הציון הצפוי שלך: <span className="num" dir="ltr">{attempt.predictedScore} ± {attempt.predictedMargin}</span>
                        </p>
                    )}
                    {message && <div className="notice mt-3" role="status">{message}</div>}
                </div>
                <div className="md:col-span-5 glass p-6 sm:p-7 flex items-center justify-between gap-6 reveal" style={{ "--i": 1 }}>
                    <div className="space-y-2">
                        <span className="eyebrow flex items-center gap-1"><FiClock aria-hidden="true" /> זמן נותר</span>
                        <div className={`num text-5xl font-semibold ${remaining <= 60 ? "text-danger" : "text-ink"}`} dir="ltr"
                             role="timer" aria-label={`זמן נותר ${formatClock(remaining)}`}>
                            {formatClock(remaining)}
                        </div>
                        <span className="faint text-xs">נענו <span className="num">{answered}/{total}</span></span>
                    </div>
                    <ProgressRing value={progress} size={112} stroke={9}>
                        <span className="num text-xl font-semibold text-ink">{Math.round(progress)}%</span>
                    </ProgressRing>
                </div>
            </div>

            <form onSubmit={(e) => { e.preventDefault(); setShowConfirm(true); }} className="space-y-5">
                {attempt.sections.map((section, si) => (
                    <section key={si} className="space-y-4" aria-labelledby={`sim-section-${si}`}>
                        <div className="glass p-5 sm:p-6">
                            <h2 id={`sim-section-${si}`} className="title text-xl">{section.title}</h2>
                            {section.instructions && <p className="muted text-sm mt-1">{section.instructions}</p>}
                        </div>
                        {section.questions.map((q) => (
                            <div key={q.id}
                                 className={`glass p-5 sm:p-6 space-y-4 ${answers[q.id] ? "!border-[color-mix(in_oklab,var(--mint)_45%,transparent)]" : ""}`}>
                                <div className="flex items-center justify-between">
                                    <span className="num text-xs text-ink-3">שאלה {q.number}</span>
                                    <span className="chip num !text-xs">{q.points} נק׳</span>
                                </div>
                                <p id={`sim-q-${q.id}`} className="text-lg font-medium text-ink leading-relaxed whitespace-pre-line">
                                    <MathText text={q.text} />
                                </p>
                                {q.answerHint && (
                                    <p className="faint text-xs">מבנה התשובה: <span className="num" dir="ltr">{q.answerHint}</span></p>
                                )}
                                <input
                                    type="text"
                                    dir="ltr"
                                    className="field num"
                                    aria-label={`תשובה לשאלה ${q.number}`}
                                    aria-describedby={`sim-q-${q.id}`}
                                    value={answers[q.id] || ""}
                                    maxLength={500}
                                    onChange={(e) => setAnswer(q.id, e.target.value)}
                                />
                            </div>
                        ))}
                    </section>
                ))}
                <div className="sticky bottom-4 z-20">
                    <button ref={submitButtonRef} type="submit" className="btn btn-primary btn-block !min-h-[3.5rem] shadow-[var(--shadow-2)]">
                        <FiSend aria-hidden="true" /> הגש סימולציה
                    </button>
                </div>
            </form>

            {showConfirm && (
                <div className="modal-backdrop">
                    <div className="modal space-y-5 text-right" dir="rtl" role="dialog" aria-modal="true" aria-labelledby="sim-confirm-title">
                        <div className="flex items-center gap-3">
                            <span className="icon-badge icon-badge--amber"><FaExclamationCircle aria-hidden="true" /></span>
                            <span id="sim-confirm-title" className="title text-xl">אישור הגשה</span>
                        </div>
                        <p className="muted leading-relaxed">
                            ענית על <span className="num">{answered}</span> מתוך <span className="num">{total}</span> שאלות.
                            לאחר ההגשה לא ניתן לשנות תשובות.
                        </p>
                        <div className="flex justify-end gap-2 pt-2">
                            <button type="button" className="btn btn-primary" autoFocus onClick={() => { setShowConfirm(false); submit(); }}>הגש</button>
                            <button type="button" className="btn btn-ghost" onClick={() => setShowConfirm(false)}>חזור</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
