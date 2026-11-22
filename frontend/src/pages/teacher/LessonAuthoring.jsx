import { useCallback, useContext, useEffect, useState } from "react";
import { FaBookOpen, FaPlus, FaTrash, FaEdit, FaSave } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import { apiDelete, apiGet, apiPost, apiPut, errorText, formatDateTime } from "../../Utils/authApi.js";
import { SUBJECTS, subTopicsOf, topicsOf } from "../../constants/curriculum.constants.js";
import {
    LESSON_CONTENT_BASE_PATH, LESSON_CONTENT_CREATE, LESSON_CONTENT_DELETE,
    LESSON_CONTENT_TEACHER_LESSONS, LESSON_CONTENT_UPDATE,
} from "../../constants/pages.constants.js";

const EMPTY = { title: "", subject: SUBJECTS[0], topic: "", subTopic: "", body: "", videoUrl: "", examples: [""] };

// Teachers write lesson pages (text, video link, worked examples) linked to a practice topic.
export default function LessonAuthoring() {
    const { user } = useContext(UserContext);
    const [lessons, setLessons] = useState([]);
    const [form, setForm] = useState(EMPTY);
    const [editingId, setEditingId] = useState(null);
    const [message, setMessage] = useState(null);
    const [saving, setSaving] = useState(false);

    const load = useCallback(() => {
        if (!user?.userId) return;
        apiGet(`${LESSON_CONTENT_BASE_PATH}${LESSON_CONTENT_TEACHER_LESSONS}`, { teacherId: user.userId })
            .then((res) => res.success && setLessons(res.data))
            .catch((err) => setMessage({ ok: false, text: errorText(err) }));
    }, [user?.userId]);

    useEffect(load, [load]);

    const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

    const edit = (l) => {
        setEditingId(l.id);
        setForm({
            title: l.title, subject: l.subject, topic: l.topic, subTopic: l.subTopic ?? "",
            body: l.body ?? "", videoUrl: l.videoEmbedUrl ?? "",
            examples: l.examples.length ? l.examples : [""],
        });
        setMessage(null);
        window.scrollTo({ top: 0, behavior: "smooth" });
    };

    const reset = () => {
        setEditingId(null);
        setForm(EMPTY);
    };

    const save = async (e) => {
        e.preventDefault();
        setSaving(true);
        const body = { ...form, examples: form.examples.filter((x) => x.trim()) };
        try {
            const url = `${LESSON_CONTENT_BASE_PATH}${editingId ? LESSON_CONTENT_UPDATE : LESSON_CONTENT_CREATE}`;
            const res = editingId
                ? await apiPut(url, { teacherId: user.userId, lessonId: editingId }, body)
                : await apiPost(url, { teacherId: user.userId }, body);
            setMessage({ ok: res.success, text: res.errorCode });
            if (res.success) {
                reset();
                load();
            }
        } catch (err) {
            setMessage({ ok: false, text: errorText(err) });
        } finally {
            setSaving(false);
        }
    };

    const remove = async (id) => {
        if (!window.confirm("למחוק את השיעור? ההתקדמות של התלמידים בשיעור תימחק גם היא.")) return;
        try {
            const res = await apiDelete(`${LESSON_CONTENT_BASE_PATH}${LESSON_CONTENT_DELETE}`, { teacherId: user.userId, lessonId: id });
            setMessage({ ok: res.success, text: res.errorCode });
            if (res.success) {
                if (editingId === id) reset();
                load();
            }
        } catch (err) {
            setMessage({ ok: false, text: errorText(err) });
        }
    };

    const topics = topicsOf(form.subject);
    const subTopics = subTopicsOf(form.subject, form.topic);

    return (
        <div className="page page--wide grid grid-cols-1 lg:grid-cols-12 gap-4 sm:gap-5 items-start" dir="rtl">
            <form onSubmit={save} className="lg:col-span-7 glass p-6 sm:p-8 space-y-5 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge"><FaBookOpen /></span>
                    <div>
                        <span className="eyebrow">Lesson studio · שיעורים</span>
                        <h2 className="title text-2xl sm:text-3xl">{editingId ? "עריכת שיעור" : "שיעור חדש"}</h2>
                    </div>
                </div>

                <div className="field-wrap">
                    <label className="label" htmlFor="lc-title">כותרת</label>
                    <input id="lc-title" className="field" value={form.title} onChange={set("title")} required maxLength={200} />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div className="field-wrap">
                        <label className="label" htmlFor="lc-subject">מקצוע</label>
                        <select id="lc-subject" className="field" value={form.subject}
                                onChange={(e) => setForm((f) => ({ ...f, subject: e.target.value, topic: "", subTopic: "" }))}>
                            {SUBJECTS.map((s) => <option key={s} value={s}>{s}</option>)}
                        </select>
                    </div>
                    <div className="field-wrap">
                        <label className="label" htmlFor="lc-topic">נושא</label>
                        <select id="lc-topic" className="field" value={form.topic} required
                                onChange={(e) => setForm((f) => ({ ...f, topic: e.target.value, subTopic: "" }))}>
                            <option value="">בחר נושא</option>
                            {topics.map((t) => <option key={t.name} value={t.name}>{t.name}</option>)}
                        </select>
                    </div>
                    <div className="field-wrap">
                        <label className="label" htmlFor="lc-sub">תת-נושא לתרגול</label>
                        <select id="lc-sub" className="field" value={form.subTopic} onChange={set("subTopic")} disabled={!form.topic}>
                            <option value="">כל הנושא</option>
                            {subTopics.map((s) => <option key={s} value={s}>{s}</option>)}
                        </select>
                    </div>
                </div>

                <div className="field-wrap">
                    <label className="label" htmlFor="lc-body">תוכן השיעור</label>
                    <textarea id="lc-body" className="field min-h-[12rem]" value={form.body} onChange={set("body")} maxLength={20000}
                              placeholder="הסבר את הנושא בשפה פשוטה. שורה ריקה מפרידה בין פסקאות." />
                </div>

                <div className="field-wrap">
                    <label className="label" htmlFor="lc-video">קישור לסרטון (YouTube או Vimeo, לא חובה)</label>
                    <input id="lc-video" className="field" dir="ltr" value={form.videoUrl} onChange={set("videoUrl")}
                           placeholder="https://www.youtube.com/watch?v=..." />
                </div>

                <div className="space-y-2">
                    <span className="label">דוגמאות פתורות</span>
                    {form.examples.map((ex, i) => (
                        <div key={i} className="flex gap-2">
                            <textarea className="field min-h-[4.5rem]" value={ex} aria-label={`דוגמה ${i + 1}`}
                                      onChange={(e) => setForm((f) => ({ ...f, examples: f.examples.map((x, j) => (j === i ? e.target.value : x)) }))}
                                      placeholder="שאלה ופתרון צעד אחר צעד" />
                            <button type="button" className="btn btn-ghost btn-sm" aria-label="הסר דוגמה"
                                    onClick={() => setForm((f) => ({ ...f, examples: f.examples.filter((_, j) => j !== i) }))}>
                                <FaTrash />
                            </button>
                        </div>
                    ))}
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => setForm((f) => ({ ...f, examples: [...f.examples, ""] }))}>
                        <FaPlus /> הוסף דוגמה
                    </button>
                </div>

                {message && <div className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</div>}

                <div className="flex gap-2">
                    <button className="btn btn-primary" disabled={saving}><FaSave /> {editingId ? "שמירת שינויים" : "פרסום שיעור"}</button>
                    {editingId && <button type="button" className="btn btn-ghost" onClick={reset}>ביטול</button>}
                </div>
            </form>

            <div className="lg:col-span-5 glass p-6 sm:p-8 space-y-4 reveal" style={{ "--i": 1 }}>
                <h3 className="title text-xl">השיעורים שלי <span className="chip num">{lessons.length}</span></h3>
                {lessons.length === 0 && <p className="muted">עוד לא כתבת שיעורים. תלמידי הכיתות שלך יראו אותם בעמוד "שיעורים".</p>}
                <ul className="space-y-2">
                    {lessons.map((l) => (
                        <li key={l.id} className="row-card flex items-center justify-between gap-3">
                            <div className="min-w-0">
                                <div className="font-semibold text-ink truncate">{l.title}</div>
                                <div className="faint text-xs">{l.topic}{l.subTopic ? ` · ${l.subTopic}` : ""} · <span className="num">{formatDateTime(l.updatedAt)}</span></div>
                            </div>
                            <div className="flex gap-1 shrink-0">
                                <button className="btn btn-ghost btn-sm" onClick={() => edit(l)} aria-label="עריכה"><FaEdit /></button>
                                <button className="btn btn-danger btn-sm" onClick={() => remove(l.id)} aria-label="מחיקה"><FaTrash /></button>
                            </div>
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    );
}
