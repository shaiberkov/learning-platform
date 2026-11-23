import { useCallback, useContext, useEffect, useState } from "react";
import axios from "axios";
import { FaBookOpen, FaFileExcel, FaLock, FaPlus, FaTrash } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import { apiDelete, apiGet, apiPost, authConfig } from "../../Utils/authRequest.js";
import useTeacherClasses from "../../components/teacher/useTeacherClasses.js";
import {
    ADD_GRADEBOOK_ASSIGNMENT, DELETE_GRADEBOOK_ASSIGNMENT, EXPORT_GRADEBOOK, GET_GRADEBOOK, GRADEBOOK_BASE_PATH,
    SET_GRADEBOOK_GRADE, SET_GRADEBOOK_WEIGHTS,
} from "../../constants/pages.constants.js";

const today = () => new Date().toISOString().slice(0, 10);

function Gradebook() {
    const { user } = useContext(UserContext);
    const teacherId = user?.userId;
    const { classes, error: classesError } = useTeacherClasses(teacherId);
    const [classRoomId, setClassRoomId] = useState(null);
    const [book, setBook] = useState(null);
    const [weights, setWeights] = useState({});
    const [message, setMessage] = useState(null);
    const [newAssignment, setNewAssignment] = useState({ title: "", category: "HOMEWORK", maxScore: 100, date: today() });

    const load = useCallback(async (id) => {
        if (!teacherId || !id) return;
        const res = await apiGet(`${GRADEBOOK_BASE_PATH}${GET_GRADEBOOK}`, { teacherId, classRoomId: id });
        if (res.success) {
            setBook(res.data);
            setWeights(res.data.weights);
        } else {
            setMessage({ ok: false, text: res.errorCode });
        }
    }, [teacherId]);

    useEffect(() => {
        if (!classRoomId && classes.length > 0) setClassRoomId(classes[0].classRoomId);
    }, [classes, classRoomId]);

    useEffect(() => { load(classRoomId); }, [classRoomId, load]);

    const applyResponse = (res) => {
        setMessage({ ok: res.success, text: res.errorCode });
        if (res.success && res.data) {
            setBook(res.data);
            setWeights(res.data.weights);
        }
    };

    const saveWeights = async () => {
        const body = Object.fromEntries(Object.entries(weights).map(([k, v]) => [k, Number(v) || 0]));
        applyResponse(await apiPost(`${GRADEBOOK_BASE_PATH}${SET_GRADEBOOK_WEIGHTS}`, body, { teacherId, classRoomId }));
    };

    const addAssignment = async (e) => {
        e.preventDefault();
        const res = await apiPost(`${GRADEBOOK_BASE_PATH}${ADD_GRADEBOOK_ASSIGNMENT}`,
            { ...newAssignment, classRoomId, maxScore: Number(newAssignment.maxScore) }, { teacherId });
        applyResponse(res);
        if (res.success) setNewAssignment((a) => ({ ...a, title: "" }));
    };

    const deleteAssignment = async (column) => {
        if (!window.confirm(`למחוק את "${column.title}" ואת כל הציונים שלה? לא ניתן לבטל פעולה זו.`)) return;
        applyResponse(await apiDelete(`${GRADEBOOK_BASE_PATH}${DELETE_GRADEBOOK_ASSIGNMENT}`,
            { teacherId, assignmentId: column.assignmentId }));
    };

    const saveGrade = async (column, studentId, raw) => {
        const current = book.grades[column.key]?.[studentId];
        const value = raw === "" ? null : Number(raw);
        if (value === (current ?? null)) return;
        if (value !== null && Number.isNaN(value)) {
            setMessage({ ok: false, text: "הציון חייב להיות מספר" });
            return;
        }
        const params = { teacherId, assignmentId: column.assignmentId, studentId };
        if (value !== null) params.score = value;
        const res = await apiPost(`${GRADEBOOK_BASE_PATH}${SET_GRADEBOOK_GRADE}`, null, params);
        if (!res.success) setMessage({ ok: false, text: res.errorCode });
        load(classRoomId);
    };

    const exportExcel = async () => {
        try {
            const response = await axios.get(`${GRADEBOOK_BASE_PATH}${EXPORT_GRADEBOOK}`,
                authConfig({ teacherId, classRoomId }, { responseType: "blob" }));
            if (response.data.type?.includes("json")) {
                const res = JSON.parse(await response.data.text());
                setMessage({ ok: false, text: res.errorCode });
                return;
            }
            const url = URL.createObjectURL(response.data);
            const link = document.createElement("a");
            link.href = url;
            link.download = `יומן-ציונים-${book?.className || classRoomId}.xlsx`;
            link.click();
            URL.revokeObjectURL(url);
        } catch (e) {
            console.error(e);
            setMessage({ ok: false, text: "שגיאה בייצוא הקובץ" });
        }
    };

    const weightSum = Object.values(weights).reduce((s, v) => s + (Number(v) || 0), 0);
    const categories = book ? Object.keys(book.categoryLabels) : [];

    return (
        <div className="page page--wide" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-6 reveal">
                <div className="panel-head flex-wrap">
                    <div className="flex items-center gap-4">
                        <span className="icon-badge icon-badge--amber"><FaBookOpen /></span>
                        <div>
                            <span className="eyebrow">Gradebook · יומן ציונים</span>
                            <h2 className="title text-2xl sm:text-3xl mt-1">יומן ציונים</h2>
                        </div>
                    </div>
                    {book && (
                        <button className="btn btn-mint btn-sm" onClick={exportExcel}><FaFileExcel /> ייצוא ל-Excel</button>
                    )}
                </div>

                {classesError && <div className="notice">{classesError}</div>}
                {message && <div className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</div>}

                <div className="flex flex-wrap gap-2">
                    {classes.map((c) => (
                        <button key={c.classRoomId} className={`choice !w-auto ${c.classRoomId === classRoomId ? "is-active" : ""}`}
                                onClick={() => { setMessage(null); setClassRoomId(c.classRoomId); }}>
                            {c.className} <span className="muted text-xs">({c.studentCount} תלמידים)</span>
                        </button>
                    ))}
                </div>

                {book && (
                    <>
                        <section className="glass glass--sunk p-4 space-y-3">
                            <h3 className="title text-lg">משקלים לחישוב הציון המשוקלל</h3>
                            <div className="flex flex-wrap items-end gap-3">
                                {categories.map((cat) => (
                                    <div key={cat} className="w-32">
                                        <label className="label">{book.categoryLabels[cat]} (%)</label>
                                        <input type="number" min="0" max="100" className="field num" value={weights[cat] ?? 0}
                                               onChange={(e) => setWeights({ ...weights, [cat]: e.target.value })} />
                                    </div>
                                ))}
                                <span className={`chip ${weightSum === 100 ? "chip--mint" : "chip--danger"}`}>סה"כ <span className="num">{weightSum}%</span></span>
                                <button className="btn btn-primary btn-sm" disabled={weightSum !== 100} onClick={saveWeights}>שמירת משקלים</button>
                            </div>
                            <p className="muted text-xs">קטגוריה שאין בה ציונים לתלמיד אינה נספרת, והמשקל שלה מתחלק בין השאר.</p>
                        </section>

                        <form className="flex flex-wrap items-end gap-3" onSubmit={addAssignment}>
                            <div className="flex-1 min-w-48">
                                <label className="label">מטלה חדשה</label>
                                <input className="field" value={newAssignment.title} maxLength={120} placeholder="למשל: שיעורי בית 3"
                                       onChange={(e) => setNewAssignment({ ...newAssignment, title: e.target.value })} required />
                            </div>
                            <div>
                                <label className="label">סוג</label>
                                <select className="field" value={newAssignment.category}
                                        onChange={(e) => setNewAssignment({ ...newAssignment, category: e.target.value })}>
                                    {categories.map((cat) => <option key={cat} value={cat}>{book.categoryLabels[cat]}</option>)}
                                </select>
                            </div>
                            <div className="w-28">
                                <label className="label">ניקוד מרבי</label>
                                <input type="number" min="1" max="1000" className="field num" value={newAssignment.maxScore}
                                       onChange={(e) => setNewAssignment({ ...newAssignment, maxScore: e.target.value })} />
                            </div>
                            <div>
                                <label className="label">תאריך</label>
                                <input type="date" className="field" value={newAssignment.date}
                                       onChange={(e) => setNewAssignment({ ...newAssignment, date: e.target.value })} />
                            </div>
                            <button type="submit" className="btn btn-primary"><FaPlus /> הוספה</button>
                        </form>

                        {book.students.length === 0 ? (
                            <p className="muted text-center py-6">אין תלמידים בכיתה זו.</p>
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="data-table text-right min-w-[640px]">
                                    <thead>
                                    <tr>
                                        <th className="sticky right-0 bg-surface-strong z-10">תלמיד/ה</th>
                                        {book.columns.map((col) => (
                                            <th key={col.key} className="align-bottom min-w-28">
                                                <div className="flex flex-col gap-1">
                                                    <span className="font-semibold text-ink">{col.title}</span>
                                                    <span className="text-xs muted">
                                                        {col.categoryLabel} · מתוך <span className="num">{col.maxScore}</span>
                                                        {col.date && <> · <span className="num">{col.date}</span></>}
                                                    </span>
                                                    {col.editable ? (
                                                        <button type="button" className="text-danger text-xs flex items-center gap-1" onClick={() => deleteAssignment(col)}>
                                                            <FaTrash /> מחיקה
                                                        </button>
                                                    ) : (
                                                        <span className="text-xs muted flex items-center gap-1" title="הציונים נלקחים מתוצאות המבחן במערכת">
                                                            <FaLock /> מבחן מהמערכת
                                                        </span>
                                                    )}
                                                </div>
                                            </th>
                                        ))}
                                        {categories.map((cat) => (
                                            <th key={cat} className="text-xs">ממוצע {book.categoryLabels[cat]} <span className="num">({book.weights[cat]}%)</span></th>
                                        ))}
                                        <th>ציון משוקלל</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {book.students.map((s) => (
                                        <tr key={s.userId}>
                                            <td className="sticky right-0 bg-surface-strong z-10 font-medium">{s.name}</td>
                                            {book.columns.map((col) => {
                                                const score = book.grades[col.key]?.[s.userId];
                                                return (
                                                    <td key={col.key}>
                                                        {col.editable ? (
                                                            <input
                                                                key={`${col.key}-${s.userId}-${score ?? ""}`}
                                                                type="number" min="0" max={col.maxScore} step="any"
                                                                className="field num !py-1 !px-2 w-20"
                                                                defaultValue={score ?? ""}
                                                                aria-label={`ציון של ${s.name} ב${col.title}`}
                                                                onBlur={(e) => saveGrade(col, s.userId, e.target.value.trim())}
                                                                onKeyDown={(e) => { if (e.key === "Enter") e.currentTarget.blur(); }}
                                                            />
                                                        ) : (
                                                            <span className="num">{score ?? "—"}</span>
                                                        )}
                                                    </td>
                                                );
                                            })}
                                            {categories.map((cat) => (
                                                <td key={cat} className="num text-ink-2">{book.categoryAverages[s.userId]?.[cat] ?? "—"}</td>
                                            ))}
                                            <td className="num font-bold text-ink">{book.finalGrades[s.userId] ?? "—"}</td>
                                        </tr>
                                    ))}
                                    </tbody>
                                    <tfoot>
                                    <tr>
                                        <td className="sticky right-0 bg-surface-strong z-10 font-semibold">ממוצע כיתתי</td>
                                        <td colSpan={book.columns.length + categories.length} />
                                        <td className="num font-bold">{book.classAverage ?? "—"}</td>
                                    </tr>
                                    </tfoot>
                                </table>
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}

export default Gradebook;
