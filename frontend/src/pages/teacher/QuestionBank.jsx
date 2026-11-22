import { useCallback, useContext, useEffect, useState } from "react";
import axios from "axios";
import { FaDatabase, FaFileImport, FaPlus, FaSearch, FaShareAlt, FaTrash, FaEdit } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import { apiDelete, apiGet, apiPost, apiPut, authConfig } from "../../Utils/authRequest.js";
import {
    ADD_BANK_QUESTION, DELETE_BANK_QUESTION, GET_BANK_TAGS, IMPORT_BANK_QUESTIONS, QUESTION_BANK_BASE_PATH,
    SEARCH_BANK_QUESTIONS, SET_BANK_QUESTION_SHARED, UPDATE_BANK_QUESTION,
} from "../../constants/pages.constants.js";

const LEVELS = ["קל", "בינוני", "קשה"];
const MAX_IMPORT_BYTES = 1024 * 1024;
const EMPTY_FORM = { questionText: "", answer: "", optionsText: "", subject: "", topic: "", level: "", grade: "", shared: false };
const EMPTY_FILTERS = { text: "", scope: "all", subject: "", topic: "", level: "", grade: "" };

function QuestionBank() {
    const { user } = useContext(UserContext);
    const [questions, setQuestions] = useState([]);
    const [tags, setTags] = useState({ subjects: [], topics: [], levels: [], grades: [] });
    const [filters, setFilters] = useState(EMPTY_FILTERS);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState(null);
    const [editing, setEditing] = useState(null); // null = closed, {} = new, {id...} = edit
    const [form, setForm] = useState(EMPTY_FORM);
    const [formError, setFormError] = useState("");
    const [importOpen, setImportOpen] = useState(false);

    const teacherId = user?.userId;

    const loadTags = useCallback(async () => {
        if (!teacherId) return;
        const res = await apiGet(`${QUESTION_BANK_BASE_PATH}${GET_BANK_TAGS}`, { teacherId });
        if (res.success) setTags(res.data);
    }, [teacherId]);

    const search = useCallback(async (f) => {
        if (!teacherId) return;
        setLoading(true);
        const params = { teacherId };
        Object.entries(f).forEach(([k, v]) => { if (v) params[k] = v; });
        const res = await apiGet(`${QUESTION_BANK_BASE_PATH}${SEARCH_BANK_QUESTIONS}`, params);
        setLoading(false);
        if (res.success) setQuestions(res.data || []);
        else setMessage({ ok: false, text: res.errorCode });
    }, [teacherId]);

    useEffect(() => {
        loadTags();
        search(EMPTY_FILTERS);
    }, [loadTags, search]);

    const refresh = () => {
        loadTags();
        search(filters);
    };

    const openNew = () => {
        setForm(EMPTY_FORM);
        setFormError("");
        setEditing({});
    };

    const openEdit = (q) => {
        setForm({
            questionText: q.questionText, answer: q.answer, optionsText: (q.options || []).join("\n"),
            subject: q.subject || "", topic: q.topic || "", level: q.level || "", grade: q.grade || "", shared: q.shared,
        });
        setFormError("");
        setEditing(q);
    };

    const saveForm = async (e) => {
        e.preventDefault();
        const body = {
            questionText: form.questionText, answer: form.answer,
            options: form.optionsText.split("\n").map((o) => o.trim()).filter(Boolean),
            subject: form.subject, topic: form.topic, level: form.level, grade: form.grade, shared: form.shared,
        };
        const res = editing?.id
            ? await apiPut(`${QUESTION_BANK_BASE_PATH}${UPDATE_BANK_QUESTION}`, body, { teacherId, questionId: editing.id })
            : await apiPost(`${QUESTION_BANK_BASE_PATH}${ADD_BANK_QUESTION}`, body, { teacherId });
        if (!res.success) {
            setFormError(res.errorCode);
            return;
        }
        setEditing(null);
        setMessage({ ok: true, text: res.errorCode });
        refresh();
    };

    const remove = async (q) => {
        if (!window.confirm("למחוק את השאלה מהמאגר? לא ניתן לבטל פעולה זו.")) return;
        const res = await apiDelete(`${QUESTION_BANK_BASE_PATH}${DELETE_BANK_QUESTION}`, { teacherId, questionId: q.id });
        setMessage({ ok: res.success, text: res.errorCode });
        if (res.success) refresh();
    };

    const toggleShare = async (q) => {
        const res = await apiPost(`${QUESTION_BANK_BASE_PATH}${SET_BANK_QUESTION_SHARED}`, null,
            { teacherId, questionId: q.id, shared: !q.shared });
        setMessage({ ok: res.success, text: res.errorCode });
        if (res.success) setQuestions((list) => list.map((x) => (x.id === q.id ? res.data : x)));
    };

    const setFilter = (key, value) => setFilters((f) => ({ ...f, [key]: value }));

    return (
        <div className="page page--wide" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-6 reveal">
                <div className="panel-head flex-wrap">
                    <div className="flex items-center gap-4">
                        <span className="icon-badge"><FaDatabase /></span>
                        <div>
                            <span className="eyebrow">Question bank · מאגר שאלות</span>
                            <h2 className="title text-2xl sm:text-3xl mt-1">מאגר השאלות שלי</h2>
                        </div>
                    </div>
                    <div className="flex gap-2 flex-wrap">
                        <button className="btn btn-ghost btn-sm" onClick={() => setImportOpen((v) => !v)}>
                            <FaFileImport /> ייבוא מקובץ
                        </button>
                        <button className="btn btn-primary btn-sm" onClick={openNew}><FaPlus /> שאלה חדשה</button>
                    </div>
                </div>

                {message && <div className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</div>}

                {importOpen && (
                    <ImportPanel teacherId={teacherId} onDone={(text, ok) => { setMessage({ ok, text }); refresh(); }} />
                )}

                <form
                    className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-3 items-end"
                    onSubmit={(e) => { e.preventDefault(); search(filters); }}
                >
                    <div className="lg:col-span-2">
                        <label className="label">חיפוש בנוסח</label>
                        <input className="field" value={filters.text} onChange={(e) => setFilter("text", e.target.value)} placeholder="מילה או ביטוי" />
                    </div>
                    <TagSelect label="מקצוע" value={filters.subject} options={tags.subjects} onChange={(v) => setFilter("subject", v)} />
                    <TagSelect label="נושא" value={filters.topic} options={tags.topics} onChange={(v) => setFilter("topic", v)} />
                    <TagSelect label="רמה" value={filters.level} options={tags.levels} onChange={(v) => setFilter("level", v)} />
                    <TagSelect label="שכבה" value={filters.grade} options={tags.grades} onChange={(v) => setFilter("grade", v)} />
                    <div className="lg:col-span-2">
                        <label className="label">מקור</label>
                        <div className="flex gap-2">
                            <button type="button" className={`choice ${filters.scope === "all" ? "is-active" : ""}`} onClick={() => setFilter("scope", "all")}>שלי ומשותפות</button>
                            <button type="button" className={`choice ${filters.scope === "mine" ? "is-active" : ""}`} onClick={() => setFilter("scope", "mine")}>רק שלי</button>
                        </div>
                    </div>
                    <div className="flex gap-2 lg:col-span-2">
                        <button type="submit" className="btn btn-primary"><FaSearch /> חיפוש</button>
                        <button type="button" className="btn btn-ghost" onClick={() => { setFilters(EMPTY_FILTERS); search(EMPTY_FILTERS); }}>ניקוי</button>
                    </div>
                </form>

                <div className="flex items-center justify-between">
                    <span className="muted text-sm">{loading ? "טוען..." : `${questions.length} שאלות`}</span>
                </div>

                <ul className="space-y-3">
                    {questions.map((q) => (
                        <li key={q.id} className="row-card space-y-2">
                            <div className="flex items-start justify-between gap-3 flex-wrap">
                                <p className="font-semibold text-ink whitespace-pre-wrap break-words min-w-0 flex-1">{q.questionText}</p>
                                <div className="flex gap-1 flex-wrap">
                                    {[q.subject, q.topic, q.level, q.grade && `שכבה ${q.grade}`].filter(Boolean).map((t) => (
                                        <span key={t} className="chip">{t}</span>
                                    ))}
                                    {q.shared && <span className="chip chip--mint">משותפת</span>}
                                </div>
                            </div>
                            {q.options?.length > 0 && (
                                <ol className="list-decimal pr-5 text-sm text-ink-2">
                                    {q.options.map((o) => <li key={o} className={o === q.answer ? "font-bold text-mint" : ""}>{o}</li>)}
                                </ol>
                            )}
                            <div className="flex items-center justify-between gap-3 flex-wrap text-sm">
                                <span className="text-ink-2">תשובה: <strong className="text-ink">{q.answer}</strong></span>
                                <span className="muted">
                                    נשאלה <span className="num">{q.timesAsked}</span> פעמים ·{" "}
                                    <span className="num">{q.attempts}</span> תשובות ·{" "}
                                    {q.successRate == null ? "אין עדיין נתוני הצלחה" : <>הצלחה <span className="num">{q.successRate}%</span></>}
                                    {!q.mine && <> · מאת {q.ownerName}</>}
                                </span>
                            </div>
                            {q.mine && (
                                <div className="flex gap-2 flex-wrap">
                                    <button className="btn btn-ghost btn-sm" onClick={() => openEdit(q)}><FaEdit /> עריכה</button>
                                    <button className="btn btn-ghost btn-sm" onClick={() => toggleShare(q)}>
                                        <FaShareAlt /> {q.shared ? "ביטול שיתוף" : "שיתוף עם מורי בית הספר"}
                                    </button>
                                    <button className="btn btn-danger btn-sm" onClick={() => remove(q)}><FaTrash /> מחיקה</button>
                                </div>
                            )}
                        </li>
                    ))}
                    {!loading && questions.length === 0 && (
                        <li className="muted text-center py-8">לא נמצאו שאלות. אפשר להוסיף שאלה חדשה או לייבא מקובץ.</li>
                    )}
                </ul>
            </div>

            {editing && (
                <div className="modal-backdrop" onClick={() => setEditing(null)}>
                    <form className="modal !max-w-xl w-full space-y-4 p-6 max-h-[90dvh] overflow-y-auto" dir="rtl"
                          onClick={(e) => e.stopPropagation()} onSubmit={saveForm}>
                        <h3 className="title text-xl">{editing.id ? "עריכת שאלה" : "שאלה חדשה"}</h3>
                        <div>
                            <label className="label">נוסח השאלה</label>
                            <textarea className="field min-h-24" value={form.questionText} maxLength={2000}
                                      onChange={(e) => setForm({ ...form, questionText: e.target.value })} required />
                        </div>
                        <div>
                            <label className="label">תשובה נכונה</label>
                            <input className="field" value={form.answer} maxLength={500}
                                   onChange={(e) => setForm({ ...form, answer: e.target.value })} required />
                        </div>
                        <div>
                            <label className="label">אפשרויות לשאלה אמריקאית (אחת בכל שורה, אופציונלי)</label>
                            <textarea className="field min-h-20" value={form.optionsText}
                                      onChange={(e) => setForm({ ...form, optionsText: e.target.value })}
                                      placeholder={"3\n4\n5"} />
                            <p className="muted text-xs mt-1">אם ממלאים אפשרויות, התשובה הנכונה חייבת להיות אחת מהן.</p>
                        </div>
                        <div className="grid grid-cols-2 gap-3">
                            <TagInput label="מקצוע" value={form.subject} options={tags.subjects} onChange={(v) => setForm({ ...form, subject: v })} />
                            <TagInput label="נושא" value={form.topic} options={tags.topics} onChange={(v) => setForm({ ...form, topic: v })} />
                            <TagInput label="רמה" value={form.level} options={[...new Set([...LEVELS, ...tags.levels])]} onChange={(v) => setForm({ ...form, level: v })} />
                            <TagInput label="שכבה" value={form.grade} options={tags.grades} onChange={(v) => setForm({ ...form, grade: v })} />
                        </div>
                        <label className="flex items-center gap-2 text-sm">
                            <input type="checkbox" checked={form.shared} onChange={(e) => setForm({ ...form, shared: e.target.checked })} />
                            שיתוף עם שאר המורים בבית הספר
                        </label>
                        {formError && <div className="notice notice--danger">{formError}</div>}
                        <div className="flex gap-2 justify-end">
                            <button type="button" className="btn btn-ghost" onClick={() => setEditing(null)}>ביטול</button>
                            <button type="submit" className="btn btn-primary">שמירה</button>
                        </div>
                    </form>
                </div>
            )}
        </div>
    );
}

function TagSelect({ label, value, options, onChange }) {
    return (
        <div>
            <label className="label">{label}</label>
            <select className="field" value={value} onChange={(e) => onChange(e.target.value)}>
                <option value="">הכל</option>
                {options.map((o) => <option key={o} value={o}>{o}</option>)}
            </select>
        </div>
    );
}

function TagInput({ label, value, options, onChange }) {
    const listId = `tag-list-${label}`;
    return (
        <div>
            <label className="label">{label}</label>
            <input className="field" list={listId} value={value} maxLength={100} onChange={(e) => onChange(e.target.value)} />
            <datalist id={listId}>{options.map((o) => <option key={o} value={o} />)}</datalist>
        </div>
    );
}

function ImportPanel({ teacherId, onDone }) {
    const [file, setFile] = useState(null);
    const [shared, setShared] = useState(false);
    const [busy, setBusy] = useState(false);
    const [errors, setErrors] = useState([]);

    const upload = async () => {
        if (!file) return;
        if (file.size > MAX_IMPORT_BYTES) {
            onDone("הקובץ גדול מ-1MB. פצלו אותו לכמה קבצים.", false);
            return;
        }
        setBusy(true);
        const data = new FormData();
        data.append("file", file);
        let res;
        try {
            res = (await axios.post(`${QUESTION_BANK_BASE_PATH}${IMPORT_BANK_QUESTIONS}`, data,
                authConfig({ teacherId, shared }))).data;
        } catch (e) {
            console.error(e);
            res = { success: false, errorCode: "שגיאה בהעלאת הקובץ" };
        }
        setBusy(false);
        setErrors(res.data?.errors || []);
        onDone(res.errorCode, res.success);
    };

    return (
        <div className="glass glass--sunk p-5 space-y-3">
            <h3 className="title text-lg">ייבוא שאלות מקובץ CSV או Excel</h3>
            <p className="muted text-sm">
                שורה ראשונה: כותרות העמודות <strong>שאלה, תשובה, מקצוע, נושא, רמה, שכבה, אפשרויות</strong> (או באנגלית).
                רק שאלה ותשובה הן חובה. אפשרויות לשאלה אמריקאית מופרדות ב־; . עד 1,000 שאלות בקובץ.
            </p>
            <div className="flex flex-wrap items-center gap-3">
                <input type="file" accept=".csv,.xlsx,.xls,.txt" className="field max-w-xs" onChange={(e) => setFile(e.target.files?.[0] || null)} />
                <label className="flex items-center gap-2 text-sm">
                    <input type="checkbox" checked={shared} onChange={(e) => setShared(e.target.checked)} />
                    לשתף את השאלות המיובאות
                </label>
                <button className="btn btn-mint btn-sm" disabled={!file || busy} onClick={upload}>
                    <FaFileImport /> {busy ? "מייבא..." : "ייבוא"}
                </button>
            </div>
            {errors.length > 0 && (
                <ul className="text-sm text-danger list-disc pr-5 max-h-40 overflow-y-auto">
                    {errors.map((e, i) => <li key={i}>{e}</li>)}
                </ul>
            )}
        </div>
    );
}

export default QuestionBank;
