import { useCallback, useContext, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { FaCalendarCheck, FaCheck, FaBookOpen } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import ProgressRing from "../../components/ui/ProgressRing.jsx";
import OrbitLoader from "../../components/ui/OrbitLoader.jsx";
import { apiGet, apiPost, errorText, formatShortDate } from "../../Utils/authApi.js";
import {
    STUDY_PLAN_BASE_PATH, STUDY_PLAN_COMPLETE_TASK, STUDY_PLAN_GET_PLAN,
} from "../../constants/pages.constants.js";

const TYPE_LABEL = { LEARN: "לימוד", PRACTICE: "תרגול", REVIEW: "חזרה" };

const todayIso = () => {
    const d = new Date();
    const pad = (n) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

function TaskRow({ task, onDone, busy }) {
    const isPast = task.date < todayIso();
    const isFuture = task.date > todayIso();
    return (
        <li className={`row-card flex items-center justify-between gap-3 ${task.completed ? "!border-[color-mix(in_oklab,var(--mint)_45%,transparent)]" : ""}`}>
            <div className="min-w-0 space-y-1">
                <div className="flex items-center gap-2 flex-wrap">
                    <span className="chip num">{formatShortDate(task.date)}</span>
                    <span className="chip">{TYPE_LABEL[task.taskType] ?? task.taskType}</span>
                    {isPast && !task.completed && <span className="chip chip--amber">פוספס</span>}
                </div>
                <p className="text-ink">{task.description}</p>
            </div>
            {task.completed ? (
                <span className="chip chip--mint"><FaCheck /> בוצע</span>
            ) : (
                <button className="btn btn-mint btn-sm" disabled={isFuture || busy} onClick={() => onDone(task.id)}>
                    <FaCheck /> סמן כבוצע
                </button>
            )}
        </li>
    );
}

// Daily study plans for upcoming exams, with how well the student kept to each plan.
export default function StudyPlan() {
    const { user } = useContext(UserContext);
    const [plan, setPlan] = useState(null);
    const [message, setMessage] = useState(null);
    const [busy, setBusy] = useState(false);

    const load = useCallback(() => {
        if (!user?.userId) return;
        apiGet(`${STUDY_PLAN_BASE_PATH}${STUDY_PLAN_GET_PLAN}`, { studentId: user.userId })
            .then((res) => (res.success ? setPlan(res.data) : setMessage({ ok: false, text: res.errorCode })))
            .catch((err) => setMessage({ ok: false, text: errorText(err) }));
    }, [user?.userId]);

    useEffect(load, [load]);

    const complete = async (taskId) => {
        setBusy(true);
        try {
            const res = await apiPost(`${STUDY_PLAN_BASE_PATH}${STUDY_PLAN_COMPLETE_TASK}`, { studentId: user.userId, taskId });
            setMessage({ ok: res.success, text: res.errorCode });
            if (res.success) load();
        } catch (err) {
            setMessage({ ok: false, text: errorText(err) });
        } finally {
            setBusy(false);
        }
    };

    if (!plan && !message) return <OrbitLoader title="בונה את תוכנית הלמידה שלך..." />;

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-5 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge"><FaCalendarCheck /></span>
                    <div>
                        <span className="eyebrow">Study plan · תוכנית למידה</span>
                        <h2 className="display text-3xl sm:text-4xl">מה לומדים היום?</h2>
                    </div>
                </div>
                {message && <div className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</div>}
                {plan && plan.today.length === 0 && (
                    <p className="muted">אין משימות להיום. כשהמורה יקבע מבחן, תופיע כאן תוכנית יומית עד למבחן.</p>
                )}
                {plan && plan.today.length > 0 && (
                    <ul className="space-y-3">
                        {plan.today.map((t) => <TaskRow key={t.id} task={t} onDone={complete} busy={busy} />)}
                    </ul>
                )}
                <div className="flex flex-wrap gap-2">
                    <Link to="/lessons" className="btn btn-ghost btn-sm"><FaBookOpen /> לשיעורים</Link>
                    <Link to="/question-practice-selector" className="btn btn-ghost btn-sm">לתרגול</Link>
                    <Link to="/test-configurator" className="btn btn-ghost btn-sm">למבחן תרגול</Link>
                </div>
            </div>

            {plan?.exams.map((exam, i) => (
                <div key={exam.testId} className="glass p-6 sm:p-8 space-y-5 reveal" style={{ "--i": i + 1 }}>
                    <div className="flex flex-wrap items-center justify-between gap-4">
                        <div className="space-y-1">
                            <span className="eyebrow">מבחן ב{exam.subject}</span>
                            <h3 className="title text-2xl">{exam.topic}</h3>
                            <p className="muted text-sm">
                                <span className="num">{exam.examStartTime}</span> · עוד <span className="num">{exam.daysLeft}</span> ימים
                            </p>
                        </div>
                        <div className="flex items-center gap-3">
                            <ProgressRing value={exam.adherencePercent} size={84} stroke={8}>
                                <span className="num text-lg font-semibold text-ink">{exam.adherencePercent}%</span>
                            </ProgressRing>
                            <p className="faint text-xs max-w-[9rem]">
                                עמידה בתוכנית: <span className="num">{exam.completedDueTasks}/{exam.dueTasks}</span> משימות עד היום
                            </p>
                        </div>
                    </div>
                    <ul className="space-y-2">
                        {exam.tasks.map((t) => <TaskRow key={t.id} task={t} onDone={complete} busy={busy} />)}
                    </ul>
                </div>
            ))}
        </div>
    );
}
