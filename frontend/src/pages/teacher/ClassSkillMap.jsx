import { useContext, useEffect, useMemo, useState } from "react";
import { FaUsers } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import OrbitLoader from "../../components/ui/OrbitLoader.jsx";
import { fetchClassSkillMap, fetchTeacherClasses, skillColor } from "../../features/adaptive/adaptiveApi.js";
import SkillLegend from "../../features/adaptive/SkillLegend.jsx";

function ClassSkillMap() {
    const { user } = useContext(UserContext);
    const [classes, setClasses] = useState([]);
    const [classId, setClassId] = useState(null);
    const [map, setMap] = useState(null);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(true);
    const [topicFilter, setTopicFilter] = useState("");

    useEffect(() => {
        if (!user?.userId) return;
        (async () => {
            const res = await fetchTeacherClasses(user.userId);
            if (res.success) {
                setClasses(res.data);
                if (res.data.length > 0) setClassId(res.data[0].id);
            } else {
                setError(res.errorCode);
            }
            setLoading(false);
        })();
    }, [user?.userId]);

    useEffect(() => {
        if (!user?.userId || !classId) return;
        (async () => {
            const res = await fetchClassSkillMap(user.userId, classId);
            if (res.success) {
                setMap(res.data);
                setError(null);
            } else {
                setMap(null);
                setError(res.errorCode);
            }
        })();
    }, [user?.userId, classId]);

    const topicNames = useMemo(
        () => (map ? [...new Set(map.subTopics.map((s) => s.topic))] : []),
        [map]
    );
    const columns = map ? map.subTopics.filter((s) => !topicFilter || s.topic === topicFilter) : [];

    if (loading) {
        return <OrbitLoader title="טוען את הכיתות שלך..." subtitle="מכינים את מפת המיומנויות של הכיתה" />;
    }

    return (
        <div className="page page--wide space-y-4 sm:space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 reveal space-y-5">
                <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
                    <div className="flex items-center gap-3">
                        <span className="icon-badge icon-badge--mint"><FaUsers /></span>
                        <div>
                            <span className="eyebrow">Class skill map · מפת כיתה</span>
                            <h1 className="display text-3xl sm:text-4xl mt-1">שליטה בנושאים לפי תלמיד</h1>
                        </div>
                    </div>
                    <SkillLegend />
                </div>

                {classes.length === 0 ? (
                    <p className="notice">לא משובצות לך כיתות עדיין.</p>
                ) : (
                    <div className="flex flex-wrap gap-2">
                        {classes.map((c) => (
                            <button
                                key={c.id}
                                onClick={() => setClassId(c.id)}
                                className={`choice !min-h-0 !py-2 ${classId === c.id ? "is-active" : ""}`}
                            >
                                {c.gradeName ? `${c.gradeName} · ` : ""}{c.name}
                            </button>
                        ))}
                    </div>
                )}

                {topicNames.length > 0 && (
                    <div className="flex flex-wrap gap-2">
                        <button
                            onClick={() => setTopicFilter("")}
                            className={`chip ${!topicFilter ? "!bg-accent-soft !text-accent" : ""}`}
                        >
                            כל הנושאים
                        </button>
                        {topicNames.map((t) => (
                            <button
                                key={t}
                                onClick={() => setTopicFilter(t)}
                                className={`chip ${topicFilter === t ? "!bg-accent-soft !text-accent" : ""}`}
                            >
                                {t}
                            </button>
                        ))}
                    </div>
                )}
            </div>

            {error && <p className="notice notice--danger">{error}</p>}

            {map && (
                <div className="glass p-4 sm:p-6 reveal" style={{ "--i": 1 }}>
                    {map.students.length === 0 ? (
                        <p className="muted">אין תלמידים בכיתה {map.className}.</p>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="data-table text-sm">
                                <thead>
                                    <tr>
                                        <th className="sticky right-0 bg-surface-strong z-10">תלמיד</th>
                                        {columns.map((col) => (
                                            <th key={col.subTopic} className="whitespace-nowrap" title={col.topic}>
                                                {col.subTopic}
                                            </th>
                                        ))}
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td className="sticky right-0 bg-surface-strong z-10 font-semibold">ממוצע כיתה</td>
                                        {columns.map((col) => {
                                            const c = skillColor(col.color);
                                            return (
                                                <td key={col.subTopic} className="text-center">
                                                    <span className={`chip border ${c.cls}`} title={`${col.studentsPracticed} תלמידים תרגלו`}>
                                                        {col.averageRating !== null ? <span className="num">{col.averageRating}</span> : "—"}
                                                    </span>
                                                </td>
                                            );
                                        })}
                                    </tr>
                                    {map.students.map((s) => (
                                        <tr key={s.userId}>
                                            <td className="sticky right-0 bg-surface-strong z-10 whitespace-nowrap">{s.name}</td>
                                            {columns.map((col) => {
                                                const cell = s.cells[col.subTopic];
                                                const c = skillColor(cell?.color);
                                                return (
                                                    <td key={col.subTopic} className="text-center">
                                                        <span
                                                            className={`inline-block min-w-[3.25rem] rounded-lg border px-2 py-1 ${c.cls}`}
                                                            title={cell ? `${cell.attempts} תשובות` : "טרם תורגל"}
                                                        >
                                                            {cell ? <span className="num">{cell.rating}</span> : "—"}
                                                        </span>
                                                    </td>
                                                );
                                            })}
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

export default ClassSkillMap;
