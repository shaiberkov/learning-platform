import { useCallback, useContext, useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { FiBookOpen, FiSend, FiEyeOff } from "react-icons/fi";
import { FaExclamationCircle } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import ProgressRing from "../../components/ui/ProgressRing.jsx";
import OrbitLoader from "../../components/ui/OrbitLoader.jsx";
import MathText from "../../components/exam/MathText.jsx";
import QuestionAnswer from "../../components/exam/QuestionAnswer.jsx";
import { examPost } from "../../components/exam/examApi.js";
import ExamFeedback from "../../features/adaptive/ExamFeedback.jsx";
import { fetchExamFeedback, sendExamQuestionTimes } from "../../features/adaptive/adaptiveApi.js";
import { INTEGRITY_EVENT, SAVE_ANSWERS, START_ATTEMPT, SUBMIT_ATTEMPT } from "../../constants/pages.constants.js";

const AUTOSAVE_MS = 10000;

// A teacher exam run against the server clock: the server sets the deadline, answers are
// autosaved, leaving the tab is reported, and the exam submits itself when time runs out.
export default function ExamSession() {
    const { testId } = useParams();
    const { user } = useContext(UserContext);
    const navigate = useNavigate();

    const [phase, setPhase] = useState("loading");
    const [message, setMessage] = useState("");
    const [attempt, setAttempt] = useState(null);
    const [answers, setAnswers] = useState({});
    const [remaining, setRemaining] = useState(0);
    const [result, setResult] = useState(null);
    const [showConfirm, setShowConfirm] = useState(false);
    const [tabLeaves, setTabLeaves] = useState(0);
    const [feedback, setFeedback] = useState(null);
    const [feedbackError, setFeedbackError] = useState(null);
    // Time per question: the question that holds the focus collects the time (ms).
    const questionTimesRef = useRef({});
    const activeQuestionRef = useRef({ id: null, since: 0 });

    const endAt = useRef(0);
    const dirty = useRef(false);
    const answersRef = useRef({});
    const submitting = useRef(false);

    const flushQuestionTime = () => {
        const { id, since } = activeQuestionRef.current;
        if (id === null) return;
        const now = Date.now();
        questionTimesRef.current[id] = (questionTimesRef.current[id] || 0) + (now - since);
        activeQuestionRef.current = { id, since: now };
    };

    const focusQuestion = (qid) => {
        if (activeQuestionRef.current.id === qid) return;
        flushQuestionTime();
        activeQuestionRef.current = { id: qid, since: Date.now() };
    };

    // After grading: send the time per question, then load the by-topic feedback.
    const loadFeedback = useCallback(async (examTestId) => {
        flushQuestionTime();
        activeQuestionRef.current = { id: null, since: 0 };
        const seconds = Object.fromEntries(
            Object.entries(questionTimesRef.current).map(([qid, ms]) => [qid, Math.round(ms / 1000)])
        );
        await sendExamQuestionTimes(user.userId, examTestId, seconds);
        const res = await fetchExamFeedback(user.userId, examTestId);
        if (res.success) setFeedback(res.data);
        else setFeedbackError(res.errorCode);
    }, [user?.userId]);

    const syncClock = (seconds) => {
        endAt.current = Date.now() + seconds * 1000;
        setRemaining(seconds);
    };

    useEffect(() => {
        if (!user?.userId) return;
        (async () => {
            try {
                const res = await examPost(START_ATTEMPT, { userId: user.userId, testId });
                if (!res.success) {
                    setMessage(res.errorCode || "לא ניתן להתחיל את המבחן");
                    setPhase("error");
                    return;
                }
                const saved = res.data.savedAnswers || {};
                answersRef.current = saved;
                setAnswers(saved);
                setAttempt(res.data);
                syncClock(res.data.remainingSeconds);
                setPhase("running");
            } catch {
                setMessage("אירעה שגיאה בעת טעינת המבחן");
                setPhase("error");
            }
        })();
    }, [user?.userId, testId]);

    const save = useCallback(async () => {
        if (!attempt || !dirty.current || submitting.current) return;
        dirty.current = false;
        try {
            const res = await examPost(SAVE_ANSWERS, { userId: user.userId, attemptId: attempt.attemptId }, answersRef.current);
            if (res.success) {
                syncClock(res.data);
            } else {
                setMessage(res.errorCode);
                setPhase("error");
            }
        } catch {
            dirty.current = true;
        }
    }, [attempt, user?.userId]);

    const submit = useCallback(async () => {
        if (!attempt || submitting.current) return;
        submitting.current = true;
        setPhase("submitting");
        try {
            const res = await examPost(SUBMIT_ATTEMPT, { userId: user.userId, attemptId: attempt.attemptId }, answersRef.current);
            setMessage(res.errorCode || "");
            if (res.data) {
                setResult(res.data);
                setPhase("result");
                loadFeedback(attempt.testId ?? testId);
            } else {
                setPhase("error");
            }
        } catch {
            submitting.current = false;
            setMessage("ההגשה נכשלה, נסו שוב");
            setPhase("running");
        }
    }, [attempt, user?.userId, loadFeedback, testId]);

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
        return () => clearInterval(timer);
    }, [phase, save]);

    // Tab-leave detection: reported to the server and shown to the teacher.
    useEffect(() => {
        if (phase !== "running" || !attempt) return;
        const report = (type) =>
            examPost(INTEGRITY_EVENT, { userId: user.userId, attemptId: attempt.attemptId, type })
                .then((res) => res.success && typeof res.data === "number" && setTabLeaves(res.data))
                .catch(() => {});
        const onVisibility = () => {
            if (document.visibilityState === "hidden") {
                report("TAB_HIDDEN");
                save();
            } else {
                report("TAB_VISIBLE");
            }
        };
        const onBlur = () => document.visibilityState === "visible" && report("WINDOW_BLUR");
        const onFocus = () => report("WINDOW_FOCUS");
        document.addEventListener("visibilitychange", onVisibility);
        window.addEventListener("blur", onBlur);
        window.addEventListener("focus", onFocus);
        return () => {
            document.removeEventListener("visibilitychange", onVisibility);
            window.removeEventListener("blur", onBlur);
            window.removeEventListener("focus", onFocus);
        };
    }, [phase, attempt, user?.userId, save]);

    const setAnswer = (questionId, value) => {
        const next = { ...answersRef.current, [questionId]: value };
        answersRef.current = next;
        dirty.current = true;
        setAnswers(next);
    };

    if (phase === "loading") {
        return <OrbitLoader title="טוען את המבחן..." subtitle="השרת מכין עבורך את המבחן" />;
    }
    if (phase === "submitting") {
        return <OrbitLoader title="בודק את התשובות שלך..." subtitle="המבחן נבדק בשרת" />;
    }
    if (phase === "error") {
        return (
            <div className="page page--medium" dir="rtl">
                <div className="glass p-8 space-y-5 reveal text-center">
                    <span className="icon-badge icon-badge--amber mx-auto"><FaExclamationCircle /></span>
                    <p className="title text-xl">{message}</p>
                    <button className="btn btn-primary" onClick={() => navigate("/teacher-tests")}>חזרה לרשימת המבחנים</button>
                </div>
            </div>
        );
    }

    if (phase === "result") {
        return (
            <div className="page page--wide space-y-5" dir="rtl">
                {message && <div className="notice">{message}</div>}
                <div className="grid grid-cols-1 md:grid-cols-12 gap-5">
                    <div className="md:col-span-5 glass p-8 flex flex-col items-center gap-5 reveal">
                        <span className="eyebrow">תוצאות המבחן</span>
                        <ProgressRing value={result.score} size={190} stroke={14}>
                            <span className="grid">
                                <span className="num text-5xl font-semibold text-ink">{result.score}</span>
                                <span className="faint text-xs mt-1">ציון</span>
                            </span>
                        </ProgressRing>
                        <p className="muted">
                            תשובות נכונות: <span className="num font-semibold text-mint">{result.correctCount} מתוך {result.totalQuestions}</span>
                        </p>
                        {result.autoSubmitted && <span className="chip">הוגש אוטומטית בתום הזמן</span>}
                    </div>
                    <div className="md:col-span-7 glass p-6 space-y-2 reveal" style={{ "--i": 1 }}>
                        <h4 className="title text-lg mb-2">פירוט לפי שאלה</h4>
                        {result.questions.map((q, i) => (
                            <div key={q.questionId} className="row-card !py-2.5 flex items-center gap-3">
                                <span className={q.correct ? "text-mint" : "text-danger"}>{q.correct ? "✓" : "✕"}</span>
                                <span className="num text-xs text-ink-3">{i + 1}.</span>
                                <span className="flex-1 text-ink text-sm"><MathText text={q.text} /></span>
                                <span className="num text-xs faint">{q.pointsEarned}/{q.points}</span>
                            </div>
                        ))}
                        <button className="btn btn-ghost mt-3" onClick={() => navigate("/teacher-tests")}>חזרה לרשימת המבחנים</button>
                    </div>
                </div>
                {feedback && <ExamFeedback feedback={feedback} />}
                {feedbackError && <p className="notice notice--danger">{feedbackError}</p>}
            </div>
        );
    }

    const questions = attempt.questions;
    const answered = questions.filter((q) => answers[q.id]).length;
    const progress = questions.length ? (answered / questions.length) * 100 : 0;
    const clock = `${Math.floor(remaining / 60)}:${String(remaining % 60).padStart(2, "0")}`;

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="grid grid-cols-1 md:grid-cols-12 gap-5">
                <div className="md:col-span-7 glass p-6 sm:p-7 reveal">
                    <div className="flex items-center gap-3 mb-4">
                        <span className="icon-badge icon-badge--mint"><FiBookOpen /></span>
                        <div>
                            <span className="eyebrow">Exam · מבחן{attempt.versionLabel ? ` · גרסה ${attempt.versionLabel}` : ""}</span>
                            <h2 className="title text-2xl mt-0.5">{attempt.title}</h2>
                        </div>
                    </div>
                    <p className="faint text-sm">
                        הזמן נמדד בשרת. התשובות נשמרות אוטומטית, והמבחן יוגש מעצמו בתום הזמן.
                        יציאה מהלשונית נרשמת ומוצגת למורה.
                    </p>
                    {tabLeaves > 0 && (
                        <div className="notice mt-3 flex items-center gap-2">
                            <FiEyeOff /> נרשמו <span className="num">{tabLeaves}</span> יציאות מהלשונית
                        </div>
                    )}
                </div>
                <div className="md:col-span-5 glass p-6 sm:p-7 flex items-center justify-between gap-6 reveal" style={{ "--i": 1 }}>
                    <div className="space-y-2">
                        <span className="eyebrow">זמן נותר</span>
                        <div className={`num text-5xl font-semibold ${remaining <= 60 ? "text-danger" : "text-ink"}`} dir="ltr">{clock}</div>
                        <span className="faint text-xs">נענו <span className="num">{answered}/{questions.length}</span></span>
                    </div>
                    <ProgressRing value={progress} size={112} stroke={9}>
                        <span className="num text-xl font-semibold text-ink">{Math.round(progress)}%</span>
                    </ProgressRing>
                </div>
            </div>

            <form
                onSubmit={(e) => { e.preventDefault(); setShowConfirm(true); }}
                className="space-y-4"
            >
                {questions.map((q, i) => (
                    <div key={q.id} onFocusCapture={() => focusQuestion(q.id)} onPointerDown={() => focusQuestion(q.id)}
                         className={`glass p-5 sm:p-6 space-y-4 ${answers[q.id] ? "!border-[color-mix(in_oklab,var(--mint)_45%,transparent)]" : ""}`}>
                        <div className="flex items-center justify-between">
                            <span className="num text-xs text-ink-3">שאלה {i + 1}</span>
                            <span className="chip num !text-xs">{q.points} נק׳</span>
                        </div>
                        <p className="text-lg font-medium text-ink leading-relaxed"><MathText text={q.text} /></p>
                        <QuestionAnswer question={q} value={answers[q.id] || ""} onChange={(v) => setAnswer(q.id, v)} />
                    </div>
                ))}
                <div className="sticky bottom-4 z-20">
                    <button type="submit" className="btn btn-primary btn-block !min-h-[3.5rem] shadow-[var(--shadow-2)]">
                        <FiSend /> הגש מבחן
                    </button>
                </div>
            </form>

            {showConfirm && (
                <div className="modal-backdrop">
                    <div className="modal space-y-5 text-right" dir="rtl">
                        <div className="flex items-center gap-3">
                            <span className="icon-badge icon-badge--amber"><FaExclamationCircle /></span>
                            <span className="title text-xl">אישור הגשה</span>
                        </div>
                        <p className="muted leading-relaxed">
                            ענית על <span className="num">{answered}</span> מתוך <span className="num">{questions.length}</span> שאלות.
                            לאחר ההגשה לא ניתן לשנות תשובות.
                        </p>
                        <div className="flex justify-end gap-2 pt-2">
                            <button className="btn btn-primary" onClick={() => { setShowConfirm(false); submit(); }}>הגש</button>
                            <button className="btn btn-ghost" onClick={() => setShowConfirm(false)}>חזור</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
