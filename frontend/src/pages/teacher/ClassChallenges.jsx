import { useCallback, useContext, useEffect, useState } from "react";
import { FaFlag, FaPlus } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import { apiGet, apiPost, errorText, formatShortDate } from "../../Utils/authApi.js";
import {
    ENGAGEMENT_BASE_PATH, ENGAGEMENT_CREATE_CLASS_CHALLENGE, ENGAGEMENT_TEACHER_CHALLENGES,
    HOMEWORK_BASE_PATH, HOMEWORK_TEACHER_CLASSES,
} from "../../constants/pages.constants.js";

const STATUS = { ACTIVE: "פעיל", UPCOMING: "בקרוב", ENDED: "הסתיים" };
const EMPTY = { classRoomId: "", title: "", targetXp: 1000, startDate: "", endDate: "" };

// Shared class goals: the whole class works toward one XP target. Only the class total is shown.
export default function ClassChallenges() {
    const { user } = useContext(UserContext);
    const [classes, setClasses] = useState([]);
    const [challenges, setChallenges] = useState([]);
    const [form, setForm] = useState(EMPTY);
    const [message, setMessage] = useState(null);

    const load = useCallback(() => {
        if (!user?.userId) return;
        apiGet(`${ENGAGEMENT_BASE_PATH}${ENGAGEMENT_TEACHER_CHALLENGES}`, { teacherId: user.userId })
            .then((res) => res.success && setChallenges(res.data))
            .catch((err) => setMessage({ ok: false, text: errorText(err) }));
    }, [user?.userId]);

    useEffect(() => {
        if (!user?.userId) return;
        apiGet(`${HOMEWORK_BASE_PATH}${HOMEWORK_TEACHER_CLASSES}`, { teacherId: user.userId })
            .then((res) => res.success && setClasses(res.data))
            .catch(() => {});
        load();
    }, [user?.userId, load]);

    const create = async (e) => {
        e.preventDefault();
        try {
            const res = await apiPost(`${ENGAGEMENT_BASE_PATH}${ENGAGEMENT_CREATE_CLASS_CHALLENGE}`, { teacherId: user.userId },
                { ...form, classRoomId: Number(form.classRoomId), targetXp: Number(form.targetXp) });
            setMessage({ ok: res.success, text: res.errorCode });
            if (res.success) {
                setForm(EMPTY);
                load();
            }
        } catch (err) {
            setMessage({ ok: false, text: errorText(err) });
        }
    };

    return (
        <div className="page page--wide grid grid-cols-1 lg:grid-cols-12 gap-4 sm:gap-5 items-start" dir="rtl">
            <form onSubmit={create} className="lg:col-span-5 glass p-6 sm:p-8 space-y-5 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge icon-badge--amber"><FaFlag /></span>
                    <div>
                        <span className="eyebrow">Class challenge · אתגר כיתתי</span>
                        <h2 className="title text-2xl sm:text-3xl">אתגר חדש</h2>
                    </div>
                </div>
                <p className="muted text-sm">כל הכיתה צוברת נקודות יחד מתרגול, מבחנים, שיעורים ושיעורי בית. התלמידים רואים רק את סך הכיתה, בלי דירוג אישי.</p>
                <div className="field-wrap">
                    <label className="label" htmlFor="ch-class">כיתה</label>
                    <select id="ch-class" className="field" required value={form.classRoomId} onChange={(e) => setForm({ ...form, classRoomId: e.target.value })}>
                        <option value="">בחר כיתה</option>
                        {classes.map((c) => <option key={c.classRoomId} value={c.classRoomId}>{c.className}</option>)}
                    </select>
                </div>
                <div className="field-wrap">
                    <label className="label" htmlFor="ch-title">שם האתגר</label>
                    <input id="ch-title" className="field" required value={form.title} placeholder="לדוגמה: שבוע השברים"
                           onChange={(e) => setForm({ ...form, title: e.target.value })} />
                </div>
                <div className="field-wrap">
                    <label className="label" htmlFor="ch-target">יעד נקודות לכל הכיתה</label>
                    <input id="ch-target" type="number" min="1" className="field num" required value={form.targetXp}
                           onChange={(e) => setForm({ ...form, targetXp: e.target.value })} />
                </div>
                <div className="grid grid-cols-2 gap-3">
                    <div className="field-wrap">
                        <label className="label" htmlFor="ch-start">מתאריך</label>
                        <input id="ch-start" type="date" className="field" required value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
                    </div>
                    <div className="field-wrap">
                        <label className="label" htmlFor="ch-end">עד תאריך</label>
                        <input id="ch-end" type="date" className="field" required value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} />
                    </div>
                </div>
                {message && <div className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</div>}
                <button className="btn btn-primary"><FaPlus /> פתיחת אתגר</button>
            </form>

            <div className="lg:col-span-7 glass p-6 sm:p-8 space-y-4 reveal" style={{ "--i": 1 }}>
                <h3 className="title text-2xl">האתגרים שלי</h3>
                {challenges.length === 0 && <p className="muted">עוד לא פתחת אתגרים.</p>}
                {challenges.map((c) => (
                    <div key={c.id} className="row-card space-y-3">
                        <div className="flex items-center justify-between gap-2">
                            <span className="font-semibold text-ink">{c.title} · {c.className}</span>
                            <span className={`chip ${c.status === "ACTIVE" ? "chip--mint" : ""}`}>{STATUS[c.status]}</span>
                        </div>
                        <div className="track"><span style={{ width: `${c.percent}%` }} /></div>
                        <p className="faint text-xs">
                            <span className="num">{c.classXp}</span> / <span className="num">{c.targetXp}</span> נקודות ·{" "}
                            <span className="num">{formatShortDate(c.startDate)}–{formatShortDate(c.endDate)}</span>
                        </p>
                    </div>
                ))}
            </div>
        </div>
    );
}
