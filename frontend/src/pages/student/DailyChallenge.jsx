import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaCalendarCheck, FaFire, FaCheckCircle, FaTimesCircle } from "react-icons/fa";
import { apiGet, apiPost, errorMessage } from "../../lib/api.js";
import {
    DAILY_CHALLENGE_ANSWER,
    DAILY_CHALLENGE_BASE_PATH,
    DAILY_CHALLENGE_START,
    DAILY_CHALLENGE_TODAY,
} from "../../constants/pages.constants.js";
import EmptyState from "../../components/ui/EmptyState.jsx";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import { DAILY_CHALLENGE_QUERY_KEY, formatClock, streakLabel } from "../../features/competition/competitionHelpers.js";

// [s03] The daily challenge: one question a day for the whole grade, 2 minutes from "start",
// the solution unlocks tomorrow.

function DailyChallenge() {
    const queryClient = useQueryClient();
    const [typed, setTyped] = useState("");
    const [message, setMessage] = useState(null);

    const today = useQuery({
        queryKey: DAILY_CHALLENGE_QUERY_KEY,
        queryFn: () => apiGet(`${DAILY_CHALLENGE_BASE_PATH}${DAILY_CHALLENGE_TODAY}`),
    });
    const data = today.data;

    const start = useMutation({
        mutationFn: () => apiPost(`${DAILY_CHALLENGE_BASE_PATH}${DAILY_CHALLENGE_START}`),
        onSuccess: (res) => queryClient.setQueryData(DAILY_CHALLENGE_QUERY_KEY, res.data),
        onError: (e) => setMessage({ tone: "danger", text: errorMessage(e, "פתיחת האתגר נכשלה") }),
    });
    const answer = useMutation({
        mutationFn: (value) => apiPost(`${DAILY_CHALLENGE_BASE_PATH}${DAILY_CHALLENGE_ANSWER}`, null, { answer: value }),
        onSuccess: (res) => {
            queryClient.setQueryData(DAILY_CHALLENGE_QUERY_KEY, res.data);
            setMessage({ tone: res.data?.correct ? "mint" : "danger", text: res.errorCode });
        },
        onError: (e) => setMessage({ tone: "danger", text: errorMessage(e, "שליחת התשובה נכשלה") }),
    });

    const secondsLeft = useCountdown(data?.status === "IN_PROGRESS" ? data.secondsLeft : null, () =>
        queryClient.invalidateQueries({ queryKey: DAILY_CHALLENGE_QUERY_KEY })
    );

    return (
        <div className="page page--narrow space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-6 reveal">
                <div className="flex items-center gap-4">
                    <span className="icon-badge icon-badge--mint"><FaCalendarCheck /></span>
                    <div>
                        <span className="eyebrow">Daily · אתגר יומי</span>
                        <h1 className="title text-2xl mt-1">האתגר של היום</h1>
                        {data && <p className="muted text-sm">שכבה {data.gradeName} · {data.topic} · {data.subTopic}</p>}
                    </div>
                    {data && (
                        <span className="chip chip--amber ms-auto" aria-label={streakLabel(data.streak)}>
                            <FaFire aria-hidden="true" /> <span className="num">{data.streak}</span>
                        </span>
                    )}
                </div>

                {today.isLoading && <SkeletonList rows={2} />}
                {today.isError && (
                    <EmptyState title="האתגר היומי אינו זמין" body={errorMessage(today.error, "נסו שוב מאוחר יותר")} />
                )}

                {data?.status === "NOT_STARTED" && (
                    <div className="space-y-4">
                        <p>
                            שאלה אחת לכל השכבה. ברגע שתלחצו "התחלה" יש לכם <strong>2 דקות</strong> לענות, ותשובה אחת בלבד.
                            הפתרון ייפתח מחר.
                        </p>
                        <button className="btn btn-primary" disabled={start.isPending} onClick={() => start.mutate()}>
                            {start.isPending ? "פותח..." : "התחלה"}
                        </button>
                    </div>
                )}

                {data?.questionText && (
                    <section className="space-y-4" aria-labelledby="daily-question">
                        {data.status === "IN_PROGRESS" && (
                            <p className="chip" role="timer" aria-live="off" aria-label="זמן שנותר">
                                זמן שנותר: <span className="num">{formatClock(secondsLeft)}</span>
                            </p>
                        )}
                        <p id="daily-question" className="display text-2xl whitespace-pre-wrap break-words" dir="ltr">
                            {data.questionText}
                        </p>
                        {data.status === "IN_PROGRESS" && (
                            <form className="flex gap-2" onSubmit={(e) => {
                                e.preventDefault();
                                if (typed.trim()) answer.mutate(typed);
                            }}>
                                <input className="field text-lg" dir="ltr" value={typed} maxLength={200} autoFocus
                                       aria-label="התשובה שלך" placeholder="התשובה שלך"
                                       onChange={(e) => setTyped(e.target.value)} />
                                <button className="btn btn-primary" type="submit" disabled={answer.isPending || !typed.trim()}>
                                    שליחה
                                </button>
                            </form>
                        )}
                        {data.status === "ANSWERED" && (
                            <div className={`notice ${data.correct ? "notice--mint" : "notice--danger"}`}>
                                {data.correct ? <FaCheckCircle aria-hidden="true" /> : <FaTimesCircle aria-hidden="true" />}
                                {" "}ענית <span className="num">{data.myAnswer}</span>.
                                {data.correct ? " תשובה נכונה!" : " התשובה לא נכונה."} הפתרון ייפתח מחר.
                            </div>
                        )}
                        {data.status === "TIMED_OUT" && (
                            <div className="notice notice--danger">הזמן נגמר. נסו שוב מחר. הפתרון ייפתח מחר.</div>
                        )}
                    </section>
                )}

                {message && (!data || data.status === "NOT_STARTED" || data.status === "IN_PROGRESS") && (
                    <div role="status" className={`notice ${message.tone === "mint" ? "notice--mint" : "notice--danger"}`}>{message.text}</div>
                )}

                {data && (
                    <p className="muted text-sm">
                        {streakLabel(data.streak)} · <span className="num">{data.solvedInGrade}</span> תלמידים בשכבה פתרו נכון היום
                    </p>
                )}
            </div>

            {data && (
                <section className="glass p-6 space-y-3 reveal" style={{ "--i": 1 }} aria-labelledby="yesterday-title">
                    <h2 id="yesterday-title" className="title text-lg">הפתרון של אתמול</h2>
                    {!data.yesterday ? (
                        <EmptyState title="לא היה אתגר אתמול" body="מחר יופיע כאן הפתרון של האתגר של היום." />
                    ) : (
                        <div className="space-y-2">
                            <p className="muted text-sm">{data.yesterday.challenge.subTopic}</p>
                            <p dir="ltr" className="text-start text-lg">{data.yesterday.challenge.questionText}</p>
                            <p>התשובה הנכונה: <strong className="num">{data.yesterday.challenge.answer}</strong></p>
                            <p className="text-sm">
                                {!data.yesterday.attempted ? "לא השתתפת אתמול."
                                    : data.yesterday.correct ? "צדקת אתמול!"
                                        : `ענית: ${data.yesterday.myAnswer ?? "-"}`}
                            </p>
                        </div>
                    )}
                </section>
            )}
        </div>
    );
}

/** Counts down from the server's secondsLeft; calls onDone once when it reaches 0. */
function useCountdown(initialSeconds, onDone) {
    const [left, setLeft] = useState(initialSeconds ?? 0);

    useEffect(() => {
        if (initialSeconds == null) return undefined;
        const deadline = Date.now() + initialSeconds * 1000;
        setLeft(initialSeconds);
        let done = false;
        const id = setInterval(() => {
            const remaining = Math.max(0, Math.round((deadline - Date.now()) / 1000));
            setLeft(remaining);
            if (remaining === 0 && !done) {
                done = true;
                clearInterval(id);
                onDone();
            }
        }, 1000);
        return () => clearInterval(id);
        // onDone only refetches; restarting the clock on its identity would reset the countdown.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [initialSeconds]);

    return left;
}

export default DailyChallenge;
