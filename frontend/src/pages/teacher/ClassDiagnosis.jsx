import { useContext, useEffect, useState } from "react";
import { FaStethoscope } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import EmptyState from "../../components/ui/EmptyState.jsx";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import { fetchTeacherClasses } from "../../features/adaptive/adaptiveApi.js";
import { fetchClassFeynman, fetchClassMistakeTypes } from "../../features/diagnosis/diagnosisApi.js";
import { mistakeType, scoreBand, summarizeCounts, TYPE_ORDER } from "../../features/diagnosis/mistakeTypes.js";

// [s06] Teacher: the mistake types and the "explain it to me" attempts of one of the teacher's classes.

export default function ClassDiagnosis() {
    const { user } = useContext(UserContext);
    const [classes, setClasses] = useState(null);
    const [classId, setClassId] = useState("");
    const [summary, setSummary] = useState(null);
    const [explanations, setExplanations] = useState([]);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!user?.userId) return;
        fetchTeacherClasses(user.userId).then((res) => {
            if (res.success) {
                setClasses(res.data);
                if (res.data.length) setClassId(String(res.data[0].id));
            } else {
                setClasses([]);
                setError(res.errorCode);
            }
        });
    }, [user?.userId]);

    useEffect(() => {
        if (!classId) return;
        let cancelled = false;
        Promise.all([fetchClassMistakeTypes(classId), fetchClassFeynman(classId)]).then(([m, f]) => {
            if (cancelled) return;
            setSummary(m.success ? m.data : null);
            setExplanations(f.success ? f.data : []);
            setError(!m.success ? m.errorCode : !f.success ? f.errorCode : "");
        });
        return () => {
            cancelled = true;
        };
    }, [classId]);

    return (
        <div className="page page--wide space-y-4 sm:space-y-5" dir="rtl">
            <section className="glass p-6 sm:p-8 reveal space-y-4">
                <div className="flex items-center gap-3">
                    <span className="icon-badge icon-badge--amber"><FaStethoscope aria-hidden="true" /></span>
                    <div>
                        <span className="eyebrow">Class diagnosis · אבחון כיתתי</span>
                        <h1 className="display text-3xl sm:text-4xl mt-1">סוגי טעויות והסברים</h1>
                    </div>
                </div>
                {classes && classes.length > 0 && (
                    <div className="field-wrap max-w-xs">
                        <label className="label" htmlFor="diagnosis-class">כיתה</label>
                        <select id="diagnosis-class" className="field" value={classId} onChange={(e) => setClassId(e.target.value)}>
                            {classes.map((c) => (
                                <option key={c.id} value={c.id}>{c.gradeName ? `${c.gradeName} · ` : ""}{c.name}</option>
                            ))}
                        </select>
                    </div>
                )}
            </section>

            {error && <p className="notice notice--danger">{error}</p>}
            {classes === null && <SkeletonList rows={3} />}
            {classes && classes.length === 0 && (
                <EmptyState title="אין כיתות" body="רק כיתות שאתם מלמדים מופיעות כאן." />
            )}

            {summary && (
                <section className="glass p-6 sm:p-8 space-y-4 reveal" style={{ "--i": 1 }}>
                    <h2 className="title text-xl">סוגי טעויות בכיתה</h2>
                    <div className="flex flex-wrap gap-2">
                        {summarizeCounts(summary.totals).map((row) => (
                            <span key={row.type} className={`chip ${row.chip}`}>
                                {row.label}: <span className="num">{row.count}</span> (<span className="num">{row.percent}%</span>)
                            </span>
                        ))}
                    </div>
                    {summary.students.length === 0 ? (
                        <EmptyState title="אין תלמידים בכיתה" />
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="data-table">
                                <thead>
                                    <tr>
                                        <th scope="col">תלמיד</th>
                                        {TYPE_ORDER.map((t) => <th key={t} scope="col">{mistakeType(t).label}</th>)}
                                    </tr>
                                </thead>
                                <tbody>
                                    {summary.students.map((s) => (
                                        <tr key={s.userId}>
                                            <th scope="row">{s.name}</th>
                                            {TYPE_ORDER.map((t) => <td key={t} className="num">{s.counts[t] ?? 0}</td>)}
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </section>
            )}

            {classId && summary && (
                <section className="glass p-6 sm:p-8 space-y-3 reveal" style={{ "--i": 2 }}>
                    <h2 className="title text-xl">הסברים בשיטת פיינמן</h2>
                    {explanations.length === 0 ? (
                        <EmptyState title="עוד אין הסברים" body="הסברים שתלמידי הכיתה ישלחו במסך 'תסביר לי' יופיעו כאן." />
                    ) : (
                        explanations.map((e) => {
                            const band = scoreBand(e.score);
                            return (
                                <article key={e.id} className="row-card !block space-y-2">
                                    <div className="flex flex-wrap items-center gap-2">
                                        <strong>{e.studentName}</strong>
                                        <span className={`chip ${band.chip}`}>{band.label}</span>
                                        <span className="chip num">{e.score ?? "—"}/100</span>
                                    </div>
                                    <p className="text-sm muted">{e.promptText}</p>
                                    <p className="glass--sunk rounded-xl p-3 text-sm whitespace-pre-wrap">{e.explanationText}</p>
                                    {e.gaps?.length > 0 && (
                                        <ul className="list-disc ps-5 text-sm space-y-1">
                                            {e.gaps.map((g) => <li key={g}>{g}</li>)}
                                        </ul>
                                    )}
                                </article>
                            );
                        })
                    )}
                </section>
            )}
        </div>
    );
}
