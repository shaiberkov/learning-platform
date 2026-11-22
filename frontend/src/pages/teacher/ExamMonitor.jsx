import { useCallback, useContext, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { FiEyeOff, FiRefreshCw } from "react-icons/fi";
import { UserContext } from "../../context/UserContext.jsx";
import { examGet } from "../../components/exam/examApi.js";
import { EXAM_ATTEMPTS } from "../../constants/pages.constants.js";

const REFRESH_MS = 15000;

const STATUS = {
    NOT_STARTED: "לא התחיל",
    IN_PROGRESS: "בתהליך",
    SUBMITTED: "הוגש",
    AUTO_SUBMITTED: "הוגש אוטומטית",
};

const EVENT = {
    TAB_HIDDEN: "יצא מהלשונית",
    TAB_VISIBLE: "חזר ללשונית",
    WINDOW_BLUR: "החלון איבד פוקוס",
    WINDOW_FOCUS: "חזר לחלון",
};

// Live view of one exam: each student's status, score and tab-leave log. Refreshes every 15 seconds.
export default function ExamMonitor() {
    const { testId } = useParams();
    const { user } = useContext(UserContext);
    const [rows, setRows] = useState([]);
    const [error, setError] = useState("");
    const [open, setOpen] = useState(null);

    const load = useCallback(async () => {
        if (!user?.userId) return;
        try {
            const res = await examGet(EXAM_ATTEMPTS, { teacherId: user.userId, testId });
            if (res.success) {
                setRows(res.data);
                setError("");
            } else {
                setError(res.errorCode);
            }
        } catch {
            setError("אירעה שגיאה בטעינת הנתונים");
        }
    }, [user?.userId, testId]);

    useEffect(() => {
        load();
        const timer = setInterval(load, REFRESH_MS);
        return () => clearInterval(timer);
    }, [load]);

    return (
        <div className="page page--wide" dir="rtl">
            <div className="glass p-5 sm:p-8 space-y-6 reveal">
                <div className="flex flex-wrap items-end justify-between gap-4">
                    <div className="space-y-2">
                        <span className="eyebrow">Exam monitor · מעקב מבחן</span>
                        <h2 className="display text-4xl">מעקב נבחנים</h2>
                        <p className="faint text-sm">יציאות מהלשונית נרשמות בשרת בזמן אמת. הדף מתעדכן כל 15 שניות.</p>
                    </div>
                    <div className="flex gap-2">
                        <button className="btn btn-ghost btn-sm" onClick={load}><FiRefreshCw /> רענון</button>
                        <Link className="btn btn-ghost btn-sm" to="/teacher-exams">חזרה</Link>
                    </div>
                </div>
                {error && <div className="notice">{error}</div>}
                <div className="overflow-x-auto -mx-2 px-2">
                    <table className="data-table min-w-[720px] text-right">
                        <thead>
                        <tr>
                            <th>תלמיד</th>
                            <th>סטטוס</th>
                            <th>התחיל</th>
                            <th>הגיש</th>
                            <th>ציון</th>
                            <th>יציאות מהלשונית</th>
                        </tr>
                        </thead>
                        <tbody>
                        {rows.map((r) => (
                            <tr key={r.studentId}>
                                <td><span className="!text-ink font-semibold">{r.studentName}</span> <span className="faint num text-xs">{r.studentId}</span></td>
                                <td>{STATUS[r.status] || r.status}</td>
                                <td className="num" dir="ltr">{r.startedAt || "-"}</td>
                                <td className="num" dir="ltr">{r.submittedAt || "-"}</td>
                                <td className="num">{r.score ?? "-"}</td>
                                <td>
                                    {r.tabLeaveCount > 0 ? (
                                        <button className="chip chip--danger num" onClick={() => setOpen(r)}>
                                            <FiEyeOff /> {r.tabLeaveCount}
                                        </button>
                                    ) : (
                                        <span className="num faint">0</span>
                                    )}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {open && (
                <div className="modal-backdrop">
                    <div className="modal space-y-4 text-right max-h-[85vh] overflow-y-auto" dir="rtl">
                        <span className="title text-xl">יומן פעילות: {open.studentName}</span>
                        <ul className="space-y-1">
                            {open.events.map((e, i) => (
                                <li key={i} className="row-card !py-2 flex justify-between gap-3">
                                    <span className={e.type === "TAB_HIDDEN" ? "text-danger" : "text-ink"}>{EVENT[e.type] || e.type}</span>
                                    <span className="num faint text-xs" dir="ltr">{e.at}</span>
                                </li>
                            ))}
                        </ul>
                        <div className="flex justify-end">
                            <button className="btn btn-ghost" onClick={() => setOpen(null)}>סגור</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
