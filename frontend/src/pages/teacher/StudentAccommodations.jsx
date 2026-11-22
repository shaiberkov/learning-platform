import { useContext, useEffect, useState } from "react";
import { FaUniversalAccess } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import { apiGet, apiPost, errorText } from "../../Utils/authApi.js";
import {
    ACCOMMODATION_BASE_PATH, ACCOMMODATION_SET_EXTRA_TIME, ACCOMMODATION_TEACHER_STUDENTS,
} from "../../constants/pages.constants.js";

const OPTIONS = [0, 10, 25, 33, 50, 100];

// Teachers set each student's extra exam time; the exam timer applies it automatically.
export default function StudentAccommodations() {
    const { user } = useContext(UserContext);
    const [students, setStudents] = useState(null);
    const [message, setMessage] = useState(null);
    const [filter, setFilter] = useState("");

    useEffect(() => {
        if (!user?.userId) return;
        apiGet(`${ACCOMMODATION_BASE_PATH}${ACCOMMODATION_TEACHER_STUDENTS}`, { teacherId: user.userId })
            .then((res) => (res.success ? setStudents(res.data) : setMessage({ ok: false, text: res.errorCode })))
            .catch((err) => setMessage({ ok: false, text: errorText(err) }));
    }, [user?.userId]);

    const setExtra = async (studentId, percent) => {
        try {
            const res = await apiPost(`${ACCOMMODATION_BASE_PATH}${ACCOMMODATION_SET_EXTRA_TIME}`,
                { teacherId: user.userId, studentId, percent });
            setMessage({ ok: res.success, text: res.errorCode });
            if (res.success) {
                setStudents((list) => list.map((s) => (s.studentUserId === studentId ? { ...s, extraTimePercent: percent } : s)));
            }
        } catch (err) {
            setMessage({ ok: false, text: errorText(err) });
        }
    };

    const shown = (students ?? []).filter((s) =>
        !filter || s.studentName?.includes(filter) || s.className?.includes(filter) || s.studentUserId.includes(filter));

    return (
        <div className="page page--wide" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-5 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge icon-badge--mint"><FaUniversalAccess /></span>
                    <div>
                        <span className="eyebrow">Accommodations · התאמות</span>
                        <h2 className="title text-2xl sm:text-3xl">תוספת זמן במבחנים</h2>
                    </div>
                </div>
                <p className="muted text-sm">התוספת מחושבת אוטומטית בטיימר של כל מבחן. הקראה וגופן לדיסלקציה נבחרים על ידי התלמיד.</p>
                <input className="field max-w-sm" placeholder="חיפוש לפי שם, כיתה או ת.ז." value={filter} onChange={(e) => setFilter(e.target.value)} />
                {message && <div className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</div>}
                {students?.length === 0 && <p className="muted">לא נמצאו תלמידים בכיתות שלך.</p>}
                {shown.length > 0 && (
                    <div className="overflow-x-auto -mx-2 px-2">
                        <table className="data-table min-w-[560px] text-right">
                            <thead>
                            <tr><th>תלמיד</th><th>כיתה</th><th>תוספת זמן</th><th>הקראה</th><th>גופן דיסלקציה</th></tr>
                            </thead>
                            <tbody>
                            {shown.map((s) => (
                                <tr key={s.studentUserId}>
                                    <td>{s.studentName}</td>
                                    <td>{s.className}</td>
                                    <td>
                                        <select className="field !py-1 num" aria-label={`תוספת זמן ל${s.studentName}`}
                                                value={s.extraTimePercent}
                                                onChange={(e) => setExtra(s.studentUserId, Number(e.target.value))}>
                                            {[...new Set([...OPTIONS, s.extraTimePercent])].sort((a, b) => a - b).map((p) => (
                                                <option key={p} value={p}>{p === 0 ? "ללא" : `${p}%`}</option>
                                            ))}
                                        </select>
                                    </td>
                                    <td>{s.readAloud ? "כן" : "–"}</td>
                                    <td>{s.dyslexiaFont ? "כן" : "–"}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
}
