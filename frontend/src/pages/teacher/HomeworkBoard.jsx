import { useCallback, useContext, useEffect, useState } from "react";
import { FaClipboardList, FaPaperPlane, FaChevronDown } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import { apiGet, apiPost, errorText, formatDateTime } from "../../Utils/authApi.js";
import { SUBJECTS } from "../../constants/curriculum.constants.js";
import {
    HOMEWORK_BASE_PATH, HOMEWORK_CREATE, HOMEWORK_TEACHER_BOARD, HOMEWORK_TEACHER_CLASSES,
} from "../../constants/pages.constants.js";

const STATUS = {
    PENDING: { label: "טרם הוגש", chip: "" },
    SUBMITTED: { label: "הוגש", chip: "chip--mint" },
    LATE: { label: "באיחור", chip: "chip--amber" },
    MISSING: { label: "לא הוגש", chip: "chip--danger" },
};

const EMPTY = { title: "", description: "", subject: SUBJECTS[0], dueDate: "", classRoomIds: [] };

function BoardCard({ hw, index }) {
    const [open, setOpen] = useState(false);
    const [shown, setShown] = useState(null);
    return (
        <div className="glass p-5 sm:p-6 space-y-3 reveal" style={{ "--i": Math.min(index, 8) }}>
            <button className="w-full flex flex-wrap items-center justify-between gap-3 text-right" onClick={() => setOpen((o) => !o)} aria-expanded={open}>
                <div>
                    <h3 className="title text-lg">{hw.title}</h3>
                    <p className="faint text-xs">
                        עד <span className="num">{formatDateTime(hw.dueDate)}</span>
                        {hw.reminderSent && " · נשלחה תזכורת"}
                    </p>
                </div>
                <div className="flex items-center gap-2">
                    <span className="chip chip--mint num">{hw.submitted}/{hw.assigned} הגישו</span>
                    {hw.late > 0 && <span className="chip chip--amber num">{hw.late} באיחור</span>}
                    <FaChevronDown className={`transition-transform ${open ? "rotate-180" : ""}`} />
                </div>
            </button>
            {open && (
                <div className="overflow-x-auto -mx-2 px-2">
                    <table className="data-table min-w-[520px] text-right">
                        <thead>
                        <tr><th>תלמיד</th><th>כיתה</th><th>סטטוס</th><th>הוגש ב</th><th></th></tr>
                        </thead>
                        <tbody>
                        {hw.students.map((s) => (
                            <tr key={s.studentUserId}>
                                <td>{s.studentName}</td>
                                <td>{s.className}</td>
                                <td><span className={`chip ${STATUS[s.status]?.chip ?? ""}`}>{STATUS[s.status]?.label ?? s.status}</span></td>
                                <td className="num">{formatDateTime(s.submittedAt)}</td>
                                <td>{s.content && <button className="link" onClick={() => setShown(s)}>צפייה</button>}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
            {shown && (
                <div className="modal-backdrop" onClick={() => setShown(null)}>
                    <div className="modal space-y-4 text-right" dir="rtl" onClick={(e) => e.stopPropagation()}>
                        <h4 className="title text-xl">{shown.studentName}</h4>
                        <p className="whitespace-pre-line text-ink max-h-[50vh] overflow-y-auto">{shown.content}</p>
                        <button className="btn btn-primary" onClick={() => setShown(null)}>סגירה</button>
                    </div>
                </div>
            )}
        </div>
    );
}

// Teachers give homework to whole classes and see who submitted, on time or late.
export default function HomeworkBoard() {
    const { user } = useContext(UserContext);
    const [classes, setClasses] = useState([]);
    const [board, setBoard] = useState([]);
    const [form, setForm] = useState(EMPTY);
    const [message, setMessage] = useState(null);
    const [sending, setSending] = useState(false);

    const loadBoard = useCallback(() => {
        if (!user?.userId) return;
        apiGet(`${HOMEWORK_BASE_PATH}${HOMEWORK_TEACHER_BOARD}`, { teacherId: user.userId })
            .then((res) => res.success && setBoard(res.data))
            .catch((err) => setMessage({ ok: false, text: errorText(err) }));
    }, [user?.userId]);

    useEffect(() => {
        if (!user?.userId) return;
        apiGet(`${HOMEWORK_BASE_PATH}${HOMEWORK_TEACHER_CLASSES}`, { teacherId: user.userId })
            .then((res) => res.success && setClasses(res.data))
            .catch(() => {});
        loadBoard();
    }, [user?.userId, loadBoard]);

    const toggleClass = (id) => setForm((f) => ({
        ...f,
        classRoomIds: f.classRoomIds.includes(id) ? f.classRoomIds.filter((x) => x !== id) : [...f.classRoomIds, id],
    }));

    const create = async (e) => {
        e.preventDefault();
        setSending(true);
        try {
            const res = await apiPost(`${HOMEWORK_BASE_PATH}${HOMEWORK_CREATE}`, { teacherId: user.userId }, form);
            setMessage({ ok: res.success, text: res.errorCode });
            if (res.success) {
                setForm(EMPTY);
                loadBoard();
            }
        } catch (err) {
            setMessage({ ok: false, text: errorText(err) });
        } finally {
            setSending(false);
        }
    };

    return (
        <div className="page page--wide grid grid-cols-1 lg:grid-cols-12 gap-4 sm:gap-5 items-start" dir="rtl">
            <form onSubmit={create} className="lg:col-span-5 glass p-6 sm:p-8 space-y-5 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge"><FaClipboardList /></span>
                    <div>
                        <span className="eyebrow">Homework · שיעורי בית</span>
                        <h2 className="title text-2xl sm:text-3xl">משימה חדשה</h2>
                    </div>
                </div>
                <div className="field-wrap">
                    <label className="label" htmlFor="hw-title">כותרת</label>
                    <input id="hw-title" className="field" required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
                </div>
                <div className="field-wrap">
                    <label className="label" htmlFor="hw-desc">הנחיות</label>
                    <textarea id="hw-desc" className="field min-h-[7rem]" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div className="field-wrap">
                        <label className="label" htmlFor="hw-subject">מקצוע</label>
                        <select id="hw-subject" className="field" value={form.subject} onChange={(e) => setForm({ ...form, subject: e.target.value })}>
                            {SUBJECTS.map((s) => <option key={s}>{s}</option>)}
                        </select>
                    </div>
                    <div className="field-wrap">
                        <label className="label" htmlFor="hw-due">מועד הגשה</label>
                        <input id="hw-due" type="datetime-local" className="field" required value={form.dueDate} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} />
                    </div>
                </div>
                <div className="space-y-2">
                    <span className="label">כיתות</span>
                    {classes.length === 0 && <p className="muted text-sm">לא נמצאו כיתות עם תלמידים שאתה מלמד.</p>}
                    <div className="grid grid-cols-2 gap-2">
                        {classes.map((c) => (
                            <button type="button" key={c.classRoomId} onClick={() => toggleClass(c.classRoomId)}
                                    className={`choice ${form.classRoomIds.includes(c.classRoomId) ? "is-active" : ""}`}>
                                {c.className} <span className="num faint">({c.studentCount})</span>
                            </button>
                        ))}
                    </div>
                </div>
                <p className="faint text-xs">תלמידים שלא הגישו יקבלו תזכורת אוטומטית 24 שעות לפני המועד.</p>
                {message && <div className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</div>}
                <button className="btn btn-primary" disabled={sending || form.classRoomIds.length === 0}><FaPaperPlane /> שליחה לכיתות</button>
            </form>

            <div className="lg:col-span-7 space-y-4">
                <div className="glass p-6 sm:p-8 reveal" style={{ "--i": 1 }}>
                    <h3 className="title text-2xl">לוח הגשות</h3>
                    {board.length === 0 && <p className="muted mt-2">עוד לא נתת שיעורי בית.</p>}
                </div>
                {board.map((hw, i) => <BoardCard key={hw.id} hw={hw} index={i + 2} />)}
            </div>
        </div>
    );
}
