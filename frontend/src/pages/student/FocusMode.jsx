import { useCallback, useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaBullseye, FaPause, FaPlay, FaStop, FaLightbulb } from "react-icons/fa";
import { apiGet, apiPost, errorMessage } from "../../lib/api.js";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import EmptyState from "../../components/ui/EmptyState.jsx";
import ProgressRing from "../../components/ui/ProgressRing.jsx";
import { elapsedSeconds, formatClock } from "./studyTimer.js";
import { FOCUS_BASE_PATH, FOCUS_INSIGHTS, FOCUS_RECORD_SESSION } from "../../constants/pages.constants.js";

const DURATIONS = [15, 25, 45, 60];

function BucketBars({ title, buckets, unit }) {
    const shown = buckets.filter((b) => b.answers > 0);
    return (
        <div className="space-y-2">
            <h4 className="title text-base">{title}</h4>
            {shown.length === 0 ? (
                <p className="faint text-sm">אין עדיין מספיק תשובות במבחנים.</p>
            ) : (
                <ul className="space-y-2">
                    {shown.map((b) => (
                        <li key={b.label} className="flex items-center gap-3">
                            <span className="num text-sm w-16 shrink-0 text-ink-2" dir="ltr">{b.label}{unit}</span>
                            <span className="flex-1 h-3 rounded-full bg-surface-sunk overflow-hidden" aria-hidden="true">
                                <span className="block h-full rounded-full bg-accent" style={{ width: `${b.accuracyPercent}%` }} />
                            </span>
                            <span className="num text-sm w-24 shrink-0 text-ink-2">
                                {b.accuracyPercent}% <span className="faint">({b.answers})</span>
                            </span>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}

function Insights() {
    const insights = useQuery({
        queryKey: ["focusInsights"],
        queryFn: () => apiGet(`${FOCUS_BASE_PATH}${FOCUS_INSIGHTS}`),
    });

    if (insights.isPending) return <SkeletonList rows={3} />;
    if (insights.isError) {
        return <div className="notice notice--danger" role="alert">{errorMessage(insights.error, "לא הצלחנו לטעון את התובנות")}</div>;
    }
    const data = insights.data;
    return (
        <div className="space-y-5">
            {data.insights.length === 0 ? (
                <EmptyState icon={<FaLightbulb />} title="עוד אין מספיק נתונים"
                            body="אחרי כמה מבחנים ומפגשי ריכוז נראה לך מתי ואיך את/ה לומד/ת הכי טוב." />
            ) : (
                <ul className="space-y-2">
                    {data.insights.map((text) => (
                        <li key={text} className="row-card flex items-center gap-3">
                            <span className="icon-badge icon-badge--amber !w-9 !h-9 shrink-0" aria-hidden="true"><FaLightbulb /></span>
                            <span className="text-ink">{text}</span>
                        </li>
                    ))}
                </ul>
            )}
            <div className="flex flex-wrap gap-2">
                <span className="chip">השבוע: <span className="num">&nbsp;{data.focus.minutesThisWeek}</span>&nbsp;דקות ריכוז</span>
                <span className="chip">סה"כ מפגשים: <span className="num">&nbsp;{data.focus.totalSessions}</span></span>
                <span className="chip">הושלמו עד הסוף: <span className="num">&nbsp;{data.focus.completionPercent}%</span></span>
                <span className="chip">תשובות שנותחו: <span className="num">&nbsp;{data.answersAnalyzed}</span></span>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <BucketBars title="דיוק לפי שעה ביום" buckets={data.byHour} unit="" />
                <BucketBars title="דיוק לפי דקות מתחילת המבחן" buckets={data.bySessionMinute} unit="′" />
            </div>
        </div>
    );
}

// Focus mode: a clean full-screen timer without menus or notifications, plus study-habit insights.
export default function FocusMode() {
    const queryClient = useQueryClient();
    const [minutes, setMinutes] = useState(25);
    const [task, setTask] = useState("");
    const [session, setSession] = useState(null); // { accumulatedMs, runningSince }
    const [now, setNow] = useState(() => Date.now());
    const [result, setResult] = useState(null);
    const startButtonRef = useRef(null);
    const pauseButtonRef = useRef(null);

    const record = useMutation({
        mutationFn: (body) => apiPost(`${FOCUS_BASE_PATH}${FOCUS_RECORD_SESSION}`, null, body),
        onSuccess: (res) => {
            setResult({ ok: true, text: res.errorCode });
            queryClient.invalidateQueries({ queryKey: ["focusInsights"] });
        },
        onError: (err) => setResult({ ok: false, text: errorMessage(err, "המפגש לא נשמר") }),
    });
    const recordSession = record.mutate;

    const elapsed = session ? elapsedSeconds(session.accumulatedMs, session.runningSince, now) : 0;
    const remaining = minutes * 60 - elapsed;

    const finish = useCallback((completed) => {
        if (!session) return;
        const seconds = elapsedSeconds(session.accumulatedMs, session.runningSince, Date.now());
        const actualMinutes = Math.min(minutes, Math.floor(seconds / 60));
        setSession(null);
        if (document.fullscreenElement) document.exitFullscreen?.().catch(() => {});
        if (actualMinutes < 1) {
            setResult({ ok: false, text: "מפגש קצר מדקה לא נשמר" });
        } else {
            recordSession({ plannedMinutes: minutes, actualMinutes, task: task.trim() || null, completed });
        }
        requestAnimationFrame(() => startButtonRef.current?.focus());
    }, [session, minutes, task, recordSession]);

    useEffect(() => {
        if (!session) return undefined;
        const id = setInterval(() => setNow(Date.now()), 500);
        return () => clearInterval(id);
    }, [session]);

    useEffect(() => {
        if (session && remaining <= 0) finish(true);
    }, [session, remaining, finish]);

    useEffect(() => {
        if (!session) return undefined;
        const onKey = (e) => { if (e.key === "Escape") finish(false); };
        window.addEventListener("keydown", onKey);
        return () => window.removeEventListener("keydown", onKey);
    }, [session, finish]);

    const start = () => {
        setResult(null);
        const startedAt = Date.now();
        setNow(startedAt);
        setSession({ accumulatedMs: 0, runningSince: startedAt });
        document.documentElement.requestFullscreen?.().catch(() => {});
        requestAnimationFrame(() => pauseButtonRef.current?.focus());
    };

    const togglePause = () => {
        const t = Date.now();
        setNow(t);
        setSession((s) => s.runningSince == null
            ? { ...s, runningSince: t }
            : { accumulatedMs: s.accumulatedMs + (t - s.runningSince), runningSince: null });
    };

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-5 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge"><FaBullseye /></span>
                    <div>
                        <span className="eyebrow">Focus mode · מצב ריכוז</span>
                        <h2 className="display text-3xl sm:text-4xl">מסך נקי, רק אני והחומר</h2>
                    </div>
                </div>
                <p className="muted">בזמן מצב ריכוז המסך מתמלא בטיימר בלבד: בלי תפריטים ובלי התראות. Esc מסיים את המפגש.</p>
                <fieldset className="space-y-2">
                    <legend className="label">כמה זמן?</legend>
                    <div className="flex flex-wrap gap-2">
                        {DURATIONS.map((d) => (
                            <button key={d} type="button" className={`choice ${minutes === d ? "is-active" : ""}`}
                                    aria-pressed={minutes === d} onClick={() => setMinutes(d)}>
                                <span className="num">{d}</span> דקות
                            </button>
                        ))}
                    </div>
                </fieldset>
                <div className="field-wrap max-w-md">
                    <label htmlFor="focus-task" className="label">על מה אני עובד/ת (לא חובה)</label>
                    <input id="focus-task" className="field" maxLength={80} value={task}
                           placeholder="למשל: שברים" onChange={(e) => setTask(e.target.value)} />
                </div>
                <button ref={startButtonRef} type="button" className="btn btn-primary" onClick={start}>
                    <FaPlay aria-hidden="true" /> התחלת מפגש ריכוז
                </button>
                {result && (
                    <div className={`notice ${result.ok ? "notice--mint" : "notice--danger"}`} role="status">{result.text}</div>
                )}
            </div>

            <div className="glass p-6 sm:p-8 space-y-4 reveal" style={{ "--i": 1 }}>
                <div>
                    <span className="eyebrow">Insights · תובנות</span>
                    <h3 className="title text-2xl">ההרגלים שלי</h3>
                </div>
                <Insights />
            </div>

            {session && (
                <div role="dialog" aria-modal="true" aria-label="מצב ריכוז" dir="rtl"
                     className="fixed inset-0 z-[250] bg-bg flex flex-col items-center justify-center gap-8 p-4">
                    {task.trim() && <p className="title text-xl text-ink-2">{task.trim()}</p>}
                    <ProgressRing value={Math.round((elapsed / (minutes * 60)) * 100)} size={260} stroke={14}>
                        <span className="num text-6xl font-semibold text-ink" role="timer" aria-live="off">
                            {formatClock(remaining)}
                        </span>
                    </ProgressRing>
                    <p className="muted" aria-live="polite">{session.runningSince == null ? "מושהה" : "מתרכזים..."}</p>
                    <div className="flex gap-3">
                        <button ref={pauseButtonRef} type="button" className="btn btn-ghost" onClick={togglePause}>
                            {session.runningSince == null
                                ? <><FaPlay aria-hidden="true" /> המשך</>
                                : <><FaPause aria-hidden="true" /> השהיה</>}
                        </button>
                        <button type="button" className="btn btn-danger" onClick={() => finish(false)}>
                            <FaStop aria-hidden="true" /> סיום
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}
