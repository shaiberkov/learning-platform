import { useCallback, useContext, useEffect, useState } from "react";
import { FaBolt, FaCheckCircle, FaTimesCircle } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import { useNotificationContext } from "../../context/NotificationContext.jsx";
import { apiGet, apiPost } from "../../Utils/authRequest.js";
import { ANSWER_LIVE_QUESTION, GET_CURRENT_LIVE_QUIZ, LIVE_QUIZ_BASE_PATH } from "../../constants/pages.constants.js";

const POLL_MS = 4000;

// Student's phone screen for the live in-class quiz.
function LiveQuizPlay() {
    const { user } = useContext(UserContext);
    const { liveQuiz } = useNotificationContext();
    const studentId = user?.userId;
    const [state, setState] = useState(null);
    const [loaded, setLoaded] = useState(false);
    const [typed, setTyped] = useState("");
    const [error, setError] = useState("");
    const [sending, setSending] = useState(false);

    const refresh = useCallback(async () => {
        if (!studentId) return;
        const res = await apiGet(`${LIVE_QUIZ_BASE_PATH}${GET_CURRENT_LIVE_QUIZ}`, { studentId });
        if (res.success) setState(res.data);
        setLoaded(true);
    }, [studentId]);

    useEffect(() => { refresh(); }, [refresh]);
    useEffect(() => {
        const id = setInterval(refresh, POLL_MS);
        return () => clearInterval(id);
    }, [refresh]);
    // An SSE push means the question changed: fetch this student's own view of it.
    useEffect(() => { if (liveQuiz) refresh(); }, [liveQuiz, refresh]);
    useEffect(() => { setTyped(""); setError(""); }, [state?.questionNumber]);

    const send = async (answer) => {
        if (!answer?.trim() || sending) return;
        setSending(true);
        const res = await apiPost(`${LIVE_QUIZ_BASE_PATH}${ANSWER_LIVE_QUESTION}`, { answer }, { studentId, code: state.code });
        setSending(false);
        if (res.success) {
            setState(res.data);
            setError("");
        } else {
            setError(res.errorCode);
            refresh();
        }
    };

    return (
        <div className="page page--narrow" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-6 reveal">
                <div className="flex items-center gap-4">
                    <span className="icon-badge icon-badge--mint"><FaBolt /></span>
                    <div>
                        <span className="eyebrow">Live quiz · חידון בכיתה</span>
                        <h2 className="title text-2xl mt-1">{state ? `חידון של ${state.teacherName}` : "חידון בכיתה"}</h2>
                    </div>
                </div>

                {!loaded && <p className="muted">טוען...</p>}

                {loaded && !state && (
                    <div className="notice">אין כרגע חידון פעיל בכיתה שלך. המסך יתעדכן כשהמורה יפתח חידון.</div>
                )}

                {state && !state.questionText && (
                    <div className="notice notice--mint">החידון נפתח! מחכים לשאלה הראשונה...</div>
                )}

                {state?.questionText && (
                    <section className="space-y-5">
                        <div className="flex justify-between items-center">
                            <span className="chip">שאלה <span className="num">{state.questionNumber}</span></span>
                            {state.questionOpen ? <span className="chip chip--mint">פתוחה</span> : <span className="chip">נסגרה</span>}
                        </div>
                        <p className="display text-2xl whitespace-pre-wrap break-words">{state.questionText}</p>

                        {state.questionOpen && !state.myAnswer && (
                            state.options?.length > 0 ? (
                                <div className="grid gap-3">
                                    {state.options.map((o) => (
                                        <button key={o} className="choice !min-h-14 text-lg" disabled={sending} onClick={() => send(o)}>{o}</button>
                                    ))}
                                </div>
                            ) : (
                                <form className="flex gap-2" onSubmit={(e) => { e.preventDefault(); send(typed); }}>
                                    <input className="field text-lg" value={typed} maxLength={300} autoFocus
                                           onChange={(e) => setTyped(e.target.value)} placeholder="התשובה שלך" />
                                    <button className="btn btn-primary" type="submit" disabled={sending || !typed.trim()}>שליחה</button>
                                </form>
                            )
                        )}

                        {state.myAnswer && state.questionOpen && (
                            <div className="notice notice--mint">התשובה "{state.myAnswer}" נקלטה. מחכים שהמורה יסגור את השאלה.</div>
                        )}

                        {!state.questionOpen && (
                            <div className="space-y-2">
                                {state.myAnswer == null ? (
                                    <div className="notice">לא ענית על שאלה זו.</div>
                                ) : state.myAnswerCorrect ? (
                                    <div className="notice notice--mint"><FaCheckCircle /> צדקת!</div>
                                ) : (
                                    <div className="notice notice--danger"><FaTimesCircle /> ענית "{state.myAnswer}".</div>
                                )}
                                <p className="text-center">התשובה הנכונה: <strong>{state.correctAnswer}</strong></p>
                                <p className="muted text-center text-sm">מחכים לשאלה הבאה...</p>
                            </div>
                        )}

                        {error && <div className="notice notice--danger">{error}</div>}
                    </section>
                )}
            </div>
        </div>
    );
}

export default LiveQuizPlay;
