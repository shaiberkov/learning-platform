import { useCallback, useContext, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { FaClipboardList } from "react-icons/fa";
import { FiCopy, FiEye, FiBarChart2, FiPlus } from "react-icons/fi";
import { UserContext } from "../../context/UserContext.jsx";
import ExamScheduleFields from "../../components/exam/ExamScheduleFields.jsx";
import { emptySchedule, schedulePayload } from "../../components/exam/examForms.js";
import { examGet, examPost } from "../../components/exam/examApi.js";
import { CLONE_EXAM, TEACHER_EXAMS } from "../../constants/pages.constants.js";

const statusChip = (exam) => {
    if (exam.template) return <span className="chip">תבנית</span>;
    if (!exam.published) return <span className="chip chip--danger">יפורסם {exam.publishAt}</span>;
    return <span className="chip chip--mint">פורסם</span>;
};

// The teacher's exams: status, submissions, links to the live monitor and item analysis, cloning.
export default function TeacherExams() {
    const { user } = useContext(UserContext);
    const [exams, setExams] = useState([]);
    const [error, setError] = useState("");
    const [cloneOf, setCloneOf] = useState(null);
    const [cloneTitle, setCloneTitle] = useState("");
    const [schedule, setSchedule] = useState(emptySchedule());
    const [cloneMessage, setCloneMessage] = useState("");

    const load = useCallback(async () => {
        if (!user?.userId) return;
        try {
            const res = await examGet(TEACHER_EXAMS, { teacherId: user.userId });
            if (res.success) setExams(res.data);
            else setError(res.errorCode);
        } catch {
            setError("אירעה שגיאה בטעינת המבחנים");
        }
    }, [user?.userId]);

    useEffect(() => { load(); }, [load]);

    const openClone = (exam) => {
        setCloneOf(exam);
        setCloneTitle(`${exam.title} (עותק)`);
        setSchedule({ ...emptySchedule(), timeLimitMinutes: exam.timeLimitMinutes });
        setCloneMessage("");
    };

    const clone = async () => {
        try {
            const res = await examPost(CLONE_EXAM, { teacherId: user.userId, testId: cloneOf.testId }, {
                title: cloneTitle,
                ...schedulePayload(schedule),
            });
            setCloneMessage(res.errorCode);
            if (res.success) {
                setCloneOf(null);
                load();
            }
        } catch {
            setCloneMessage("השכפול נכשל");
        }
    };

    return (
        <div className="page page--wide" dir="rtl">
            <div className="glass p-5 sm:p-8 space-y-6 reveal">
                <div className="flex flex-wrap items-end justify-between gap-4">
                    <div className="space-y-2">
                        <span className="eyebrow">My exams · המבחנים שלי</span>
                        <h2 className="display text-4xl flex items-center gap-3">
                            <span className="icon-badge icon-badge--mint"><FaClipboardList /></span>
                            המבחנים שלי
                        </h2>
                    </div>
                    <Link to="/exam-builder" className="btn btn-primary"><FiPlus /> מבחן חדש</Link>
                </div>
                {error && <div className="notice">{error}</div>}
                {exams.length === 0 ? (
                    <p className="faint text-center py-10">עדיין אין מבחנים</p>
                ) : (
                    <div className="overflow-x-auto -mx-2 px-2">
                        <table className="data-table min-w-[760px] text-right">
                            <thead>
                            <tr>
                                <th>מבחן</th>
                                <th>מועד</th>
                                <th>סטטוס</th>
                                <th>הגישו</th>
                                <th>פעולות</th>
                            </tr>
                            </thead>
                            <tbody>
                            {exams.map((exam) => (
                                <tr key={exam.testId}>
                                    <td>
                                        <span className="font-semibold !text-ink">{exam.title}</span>
                                        {exam.versionLabel && <span className="chip num !text-xs ms-2">גרסה {exam.versionLabel}</span>}
                                        <div className="faint text-xs">{exam.subject} · {exam.questionCount} שאלות</div>
                                    </td>
                                    <td className="num" dir="ltr">{exam.startTime || "-"}</td>
                                    <td>{statusChip(exam)}</td>
                                    <td className="num">{exam.template ? "-" : `${exam.submittedCount}/${exam.studentCount}`}</td>
                                    <td>
                                        <div className="flex flex-wrap gap-2">
                                            {!exam.template && (
                                                <>
                                                    <Link className="btn btn-ghost btn-sm" to={`/teacher-exams/${exam.testId}/monitor`}><FiEye /> מעקב</Link>
                                                    <Link className="btn btn-ghost btn-sm" to={`/teacher-exams/${exam.testId}/analysis`}><FiBarChart2 /> ניתוח פריטים</Link>
                                                </>
                                            )}
                                            <button className="btn btn-ghost btn-sm" onClick={() => openClone(exam)}><FiCopy /> שכפול</button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {cloneOf && (
                <div className="modal-backdrop">
                    <div className="modal space-y-4 text-right max-h-[90vh] overflow-y-auto" dir="rtl">
                        <span className="title text-xl">שכפול: {cloneOf.title}</span>
                        <div>
                            <label className="label">כותרת העותק</label>
                            <input className="field" value={cloneTitle} onChange={(e) => setCloneTitle(e.target.value)} />
                        </div>
                        <ExamScheduleFields value={schedule} onChange={setSchedule} />
                        {cloneMessage && <div className="notice">{cloneMessage}</div>}
                        <div className="flex justify-end gap-2">
                            <button className="btn btn-primary" onClick={clone}>שכפול</button>
                            <button className="btn btn-ghost" onClick={() => setCloneOf(null)}>ביטול</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
