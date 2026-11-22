import { useCallback, useContext, useEffect, useState } from "react";
import { FaClipboardCheck, FaPaperPlane } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import { useNotificationContext } from "../../context/NotificationContext.jsx";
import OrbitLoader from "../../components/ui/OrbitLoader.jsx";
import { apiGet, apiPost, errorText, formatDateTime } from "../../Utils/authApi.js";
import {
    HOMEWORK_BASE_PATH, HOMEWORK_STUDENT_HOMEWORK, HOMEWORK_SUBMIT,
} from "../../constants/pages.constants.js";

const HOMEWORK_STATUS = {
    PENDING: { label: "ממתין להגשה", chip: "chip--amber" },
    SUBMITTED: { label: "הוגש", chip: "chip--mint" },
    LATE: { label: "הוגש באיחור", chip: "chip--amber" },
    MISSING: { label: "לא הוגש", chip: "chip--danger" },
};

function HomeworkCard({ hw, studentId, onSubmitted, index }) {
    const [text, setText] = useState(hw.submittedContent ?? "");
    const [open, setOpen] = useState(hw.status === "PENDING" || hw.status === "MISSING");
    const [message, setMessage] = useState(null);
    const [sending, setSending] = useState(false);
    const status = HOMEWORK_STATUS[hw.status] ?? HOMEWORK_STATUS.PENDING;

    const submit = async (e) => {
        e.preventDefault();
        setSending(true);
        try {
            const res = await apiPost(`${HOMEWORK_BASE_PATH}${HOMEWORK_SUBMIT}`, { studentId, homeworkId: hw.id }, { content: text });
            setMessage({ ok: res.success, text: res.errorCode });
            if (res.success) onSubmitted();
        } catch (err) {
            setMessage({ ok: false, text: errorText(err) });
        } finally {
            setSending(false);
        }
    };

    return (
        <div className="glass p-5 sm:p-6 space-y-3 reveal" style={{ "--i": Math.min(index, 8) }}>
            <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="space-y-1">
                    {hw.subject && <span className="eyebrow">{hw.subject}</span>}
                    <h3 className="title text-xl">{hw.title}</h3>
                    <p className="faint text-xs">להגשה עד <span className="num">{formatDateTime(hw.dueDate)}</span></p>
                </div>
                <span className={`chip ${status.chip}`}>{status.label}</span>
            </div>
            {hw.description && <p className="whitespace-pre-line text-ink">{hw.description}</p>}
            {!open ? (
                <button className="btn btn-ghost btn-sm" onClick={() => setOpen(true)}>עריכת ההגשה</button>
            ) : (
                <form onSubmit={submit} className="space-y-3">
                    <label className="label" htmlFor={`hw-${hw.id}`}>התשובה שלך</label>
                    <textarea id={`hw-${hw.id}`} className="field min-h-[8rem]" value={text} onChange={(e) => setText(e.target.value)} />
                    {hw.status === "MISSING" && <p className="faint text-xs">מועד ההגשה עבר. אפשר עדיין להגיש, וההגשה תסומן כמאוחרת.</p>}
                    <button className="btn btn-primary" disabled={sending || !text.trim()}>
                        <FaPaperPlane /> {hw.submittedAt ? "הגשה מחדש" : "הגשה"}
                    </button>
                </form>
            )}
            {message && <div className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</div>}
        </div>
    );
}

// The student's homework: due dates, status (late is flagged) and submission.
export default function Homework() {
    const { user } = useContext(UserContext);
    const { homeworkAlerts = [] } = useNotificationContext() ?? {};
    const [items, setItems] = useState(null);
    const [error, setError] = useState("");

    const load = useCallback(() => {
        if (!user?.userId) return;
        apiGet(`${HOMEWORK_BASE_PATH}${HOMEWORK_STUDENT_HOMEWORK}`, { studentId: user.userId })
            .then((res) => (res.success ? setItems(res.data) : setError(res.errorCode)))
            .catch((err) => setError(errorText(err)));
    }, [user?.userId]);

    useEffect(load, [load, homeworkAlerts.length]);

    if (!items && !error) return <OrbitLoader title="טוען שיעורי בית..." />;

    return (
        <div className="page page--medium space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-4 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge"><FaClipboardCheck /></span>
                    <div>
                        <span className="eyebrow">Homework · שיעורי בית</span>
                        <h2 className="display text-3xl sm:text-4xl">שיעורי הבית שלי</h2>
                    </div>
                </div>
                {homeworkAlerts.slice(0, 3).map((a, i) => (
                    <div key={i} className="notice">{a.message}</div>
                ))}
                {error && <div className="notice notice--danger">{error}</div>}
                {items?.length === 0 && <p className="muted">אין שיעורי בית כרגע.</p>}
            </div>
            {items?.map((hw, i) => (
                <HomeworkCard key={hw.id} hw={hw} studentId={user.userId} onSubmitted={load} index={i} />
            ))}
        </div>
    );
}
