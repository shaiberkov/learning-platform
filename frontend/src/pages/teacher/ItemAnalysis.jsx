import { useContext, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { FiAlertTriangle, FiBarChart2 } from "react-icons/fi";
import { UserContext } from "../../context/UserContext.jsx";
import MathText from "../../components/exam/MathText.jsx";
import { examGet, KIND_LABELS } from "../../components/exam/examApi.js";
import { ITEM_ANALYSIS } from "../../constants/pages.constants.js";

const MIN_RELIABLE = 10;

const pct = (v) => (v === null || v === undefined ? "-" : `${Math.round(v * 100)}%`);
const fixed = (v) => (v === null || v === undefined ? "-" : v.toFixed(2));

// A bar from 0 to 1 (difficulty) or -1 to 1 (discrimination), drawn with theme tokens.
function Meter({ value, min = 0 }) {
    if (value === null || value === undefined) return null;
    const width = Math.max(0, Math.min(1, (value - min) / (1 - min))) * 100;
    return (
        <div className="h-1.5 rounded-full bg-line mt-1 overflow-hidden" aria-hidden="true">
            <div className={`h-full ${value < 0 ? "bg-danger" : "bg-accent"}`} style={{ width: `${width}%` }} />
        </div>
    );
}

// Item analysis per exam: difficulty index (share correct) and discrimination index
// (upper 27% minus lower 27% by total score), with questions worth reviewing flagged.
export default function ItemAnalysis() {
    const { testId } = useParams();
    const { user } = useContext(UserContext);
    const [data, setData] = useState(null);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!user?.userId) return;
        examGet(ITEM_ANALYSIS, { teacherId: user.userId, testId })
            .then((res) => (res.success ? setData(res.data) : setError(res.errorCode)))
            .catch(() => setError("אירעה שגיאה בטעינת הניתוח"));
    }, [user?.userId, testId]);

    const flagged = data ? data.items.filter((i) => i.flags.length > 0).length : 0;

    return (
        <div className="page page--wide" dir="rtl">
            <div className="glass p-5 sm:p-8 space-y-6 reveal">
                <div className="flex flex-wrap items-end justify-between gap-4">
                    <div className="space-y-2">
                        <span className="eyebrow">Item analysis · ניתוח פריטים</span>
                        <h2 className="display text-4xl flex items-center gap-3">
                            <span className="icon-badge"><FiBarChart2 /></span>
                            {data?.title || "ניתוח פריטים"}
                        </h2>
                    </div>
                    <Link className="btn btn-ghost btn-sm" to="/teacher-exams">חזרה</Link>
                </div>

                {error && <div className="notice">{error}</div>}

                {data && (
                    <>
                        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                            <div className="glass--sunk p-4"><div className="faint text-xs">נבחנים שהגישו</div><div className="num text-3xl text-ink">{data.examineeCount}</div></div>
                            <div className="glass--sunk p-4"><div className="faint text-xs">גודל קבוצה עליונה / תחתונה (27%)</div><div className="num text-3xl text-ink">{data.groupSize}</div></div>
                            <div className="glass--sunk p-4"><div className="faint text-xs">שאלות מסומנות לבדיקה</div><div className="num text-3xl text-ink">{flagged}</div></div>
                        </div>
                        <p className="faint text-sm">
                            מדד קושי: אחוז הנבחנים שענו נכון (גבוה = קל). מדד הבחנה: אחוז הנכונים בקבוצה העליונה פחות
                            אחוז הנכונים בקבוצה התחתונה (0.3 ומעלה טוב, מתחת ל-0.2 חלש, שלילי מחייב בדיקה).
                        </p>
                        {data.examineeCount < MIN_RELIABLE && (
                            <div className="notice">פחות מ-{MIN_RELIABLE} נבחנים: המדדים עדיין לא יציבים, יש להתייחס אליהם בזהירות.</div>
                        )}
                        <div className="overflow-x-auto -mx-2 px-2">
                            <table className="data-table min-w-[760px] text-right">
                                <thead>
                                <tr>
                                    <th>#</th>
                                    <th>שאלה</th>
                                    <th>נכונים</th>
                                    <th>מדד קושי</th>
                                    <th>מדד הבחנה</th>
                                    <th>הערות</th>
                                </tr>
                                </thead>
                                <tbody>
                                {data.items.map((item) => (
                                    <tr key={item.questionId}>
                                        <td className="num">{item.position}</td>
                                        <td>
                                            <span className="!text-ink"><MathText text={item.text} /></span>
                                            <div className="faint text-xs">{KIND_LABELS[item.kind] || item.kind}</div>
                                        </td>
                                        <td className="num">{item.correctCount}/{item.answeredCount}</td>
                                        <td className="num">{pct(item.difficulty)}<Meter value={item.difficulty} /></td>
                                        <td className="num">{fixed(item.discrimination)}<Meter value={item.discrimination} min={-1} /></td>
                                        <td>
                                            {item.flags.map((f) => (
                                                <div key={f} className="text-danger text-xs flex items-start gap-1"><FiAlertTriangle className="shrink-0 mt-0.5" /> {f}</div>
                                            ))}
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}
