import { useCallback, useContext, useEffect, useState } from "react";
import { FaUserCheck } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import { apiGet, apiPost } from "../../Utils/authRequest.js";
import AttendanceReportView from "../../components/AttendanceReportView.jsx";
import {
    ATTENDANCE_BASE_PATH, GET_ATTENDANCE_LESSONS, GET_ATTENDANCE_SHEET, MARK_ATTENDANCE,
} from "../../constants/pages.constants.js";

const STATUSES = [
    { value: "PRESENT", label: "נוכח/ת", tone: "chip--mint" },
    { value: "LATE", label: "איחור", tone: "chip--amber" },
    { value: "ABSENT", label: "נעדר/ת", tone: "chip--danger" },
    { value: "EXCUSED", label: "מוצדקת", tone: "" },
];

const localToday = () => {
    const d = new Date();
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
};

function Attendance() {
    const { user } = useContext(UserContext);
    const teacherId = user?.userId;
    const [tab, setTab] = useState("mark");
    const [date, setDate] = useState(localToday());
    const [lessons, setLessons] = useState([]);
    const [lessonsNote, setLessonsNote] = useState("");
    const [lessonId, setLessonId] = useState(null);
    const [sheet, setSheet] = useState(null);
    const [statuses, setStatuses] = useState({});
    const [message, setMessage] = useState(null);
    const [saving, setSaving] = useState(false);

    const loadLessons = useCallback(async () => {
        if (!teacherId) return;
        const res = await apiGet(`${ATTENDANCE_BASE_PATH}${GET_ATTENDANCE_LESSONS}`, { teacherId, date });
        setLessons(res.success ? res.data || [] : []);
        setLessonsNote(res.errorCode || "");
    }, [teacherId, date]);

    useEffect(() => {
        setLessonId(null);
        setSheet(null);
        loadLessons();
    }, [loadLessons]);

    const openSheet = async (id) => {
        setLessonId(id);
        setMessage(null);
        const res = await apiGet(`${ATTENDANCE_BASE_PATH}${GET_ATTENDANCE_SHEET}`, { teacherId, lessonId: id, date });
        if (!res.success) {
            setMessage({ ok: false, text: res.errorCode });
            setSheet(null);
            return;
        }
        setSheet(res.data);
        // Unmarked students start as present, so the teacher only taps the exceptions.
        setStatuses(Object.fromEntries(res.data.students.map((s) => [s.studentId, s.status || "PRESENT"])));
    };

    const save = async () => {
        setSaving(true);
        const body = Object.entries(statuses).map(([studentId, status]) => ({ studentId, status }));
        const res = await apiPost(`${ATTENDANCE_BASE_PATH}${MARK_ATTENDANCE}`, body, { teacherId, lessonId, date });
        setSaving(false);
        setMessage({ ok: res.success, text: res.errorCode });
        if (res.success) {
            setSheet(res.data);
            loadLessons();
        }
    };

    const isFuture = date > localToday();
    const counts = STATUSES.map((s) => ({ ...s, n: Object.values(statuses).filter((v) => v === s.value).length }));

    return (
        <div className="page page--wide" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-6 reveal">
                <div className="panel-head flex-wrap">
                    <div className="flex items-center gap-4">
                        <span className="icon-badge"><FaUserCheck /></span>
                        <div>
                            <span className="eyebrow">Attendance · נוכחות</span>
                            <h2 className="title text-2xl sm:text-3xl mt-1">נוכחות</h2>
                        </div>
                    </div>
                    <div className="flex gap-2">
                        <button className={`choice !w-auto ${tab === "mark" ? "is-active" : ""}`} onClick={() => setTab("mark")}>סימון נוכחות</button>
                        <button className={`choice !w-auto ${tab === "report" ? "is-active" : ""}`} onClick={() => setTab("report")}>דוח היעדרויות</button>
                    </div>
                </div>

                {tab === "report" ? (
                    <AttendanceReportView userId={teacherId} />
                ) : (
                    <div className="space-y-5">
                        <div className="flex flex-wrap items-end gap-3">
                            <div>
                                <label className="label">תאריך</label>
                                <input type="date" className="field" value={date} max={localToday()} onChange={(e) => setDate(e.target.value)} />
                            </div>
                            {isFuture && <span className="chip chip--danger">לא ניתן לסמן לתאריך עתידי</span>}
                        </div>

                        {lessonsNote && <div className="notice">{lessonsNote}</div>}

                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                            {lessons.map((l) => (
                                <button key={l.lessonId} onClick={() => openSheet(l.lessonId)}
                                        className={`choice !items-start !flex-col !gap-1 text-right ${l.lessonId === lessonId ? "is-active" : ""}`}>
                                    <span className="font-semibold">{l.subject} · כיתה {l.className}</span>
                                    <span className="num text-sm muted">{l.startTime}–{l.endTime}</span>
                                    <span className={`chip ${l.markedCount >= l.studentCount && l.studentCount > 0 ? "chip--mint" : ""}`}>
                                        סומנו <span className="num">{l.markedCount}/{l.studentCount}</span>
                                    </span>
                                </button>
                            ))}
                        </div>

                        {message && <div className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</div>}

                        {sheet && (
                            <section className="glass glass--sunk p-4 space-y-4">
                                <div className="flex flex-wrap items-center justify-between gap-2">
                                    <h3 className="title text-lg">{sheet.subject} · כיתה {sheet.className} · <span className="num">{sheet.date}</span></h3>
                                    <div className="flex gap-1 flex-wrap">
                                        {counts.map((c) => <span key={c.value} className={`chip ${c.tone}`}>{c.label} <span className="num">{c.n}</span></span>)}
                                    </div>
                                </div>
                                {sheet.students.length === 0 && <p className="muted">אין תלמידים בכיתה זו.</p>}
                                <ul className="space-y-2">
                                    {sheet.students.map((s) => (
                                        <li key={s.studentId} className="row-card flex flex-wrap items-center justify-between gap-2">
                                            <span className="font-medium">
                                                {s.name} {!s.status && <span className="chip mr-1">טרם סומן</span>}
                                            </span>
                                            <div className="flex gap-1 flex-wrap" role="radiogroup" aria-label={`נוכחות של ${s.name}`}>
                                                {STATUSES.map((st) => (
                                                    <button key={st.value} type="button" role="radio"
                                                            aria-checked={statuses[s.studentId] === st.value}
                                                            className={`btn btn-sm ${statuses[s.studentId] === st.value ? (st.value === "ABSENT" ? "btn-danger" : st.value === "PRESENT" ? "btn-mint" : "btn-primary") : "btn-ghost"}`}
                                                            onClick={() => setStatuses({ ...statuses, [s.studentId]: st.value })}>
                                                        {st.label}
                                                    </button>
                                                ))}
                                            </div>
                                        </li>
                                    ))}
                                </ul>
                                {sheet.students.length > 0 && (
                                    <button className="btn btn-primary" disabled={saving || isFuture} onClick={save}>
                                        {saving ? "שומר..." : "שמירת נוכחות"}
                                    </button>
                                )}
                            </section>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}

export default Attendance;
