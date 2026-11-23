import { useContext, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { FaProjectDiagram } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import OrbitLoader from "../../components/ui/OrbitLoader.jsx";
import { fetchSkillMap, practicePath, skillColor } from "../../features/adaptive/adaptiveApi.js";
import SkillLegend from "../../features/adaptive/SkillLegend.jsx";

function SkillMap() {
    const { user } = useContext(UserContext);
    const [topics, setTopics] = useState([]);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!user?.userId) return;
        (async () => {
            const res = await fetchSkillMap(user.userId);
            if (res.success) setTopics(res.data);
            else setError(res.errorCode);
            setLoading(false);
        })();
    }, [user?.userId]);

    if (loading) {
        return <OrbitLoader title="בונה את מפת המיומנויות..." subtitle="מחשבים את רמת השליטה שלך בכל נושא" />;
    }

    return (
        <div className="page page--wide space-y-4 sm:space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 reveal flex flex-col sm:flex-row sm:items-end justify-between gap-4">
                <div className="flex items-center gap-3">
                    <span className="icon-badge"><FaProjectDiagram /></span>
                    <div>
                        <span className="eyebrow">Skill map · מפת מיומנויות</span>
                        <h1 className="display text-3xl sm:text-4xl mt-1">איפה אני עומד</h1>
                        <p className="muted text-sm mt-2">
                            הדירוג מתעדכן אחרי כל תשובה בתרגול ובמבחנים. התרגול בוחר שאלות קצת מעל הרמה הנוכחית שלך.
                        </p>
                    </div>
                </div>
                <SkillLegend />
            </div>

            {error && <p className="notice notice--danger">{error}</p>}

            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 sm:gap-5">
                {topics.map((topic, i) => {
                    const c = skillColor(topic.color);
                    return (
                        <section key={topic.topic} className="glass p-5 sm:p-6 space-y-4 reveal" style={{ "--i": Math.min(i, 8) }}>
                            <header className="flex items-center justify-between gap-3">
                                <h2 className="title text-lg">{topic.topic}</h2>
                                <span className={`chip border ${c.cls}`}>
                                    {topic.averageRating !== null ? <span className="num">{topic.averageRating}</span> : c.label}
                                </span>
                            </header>
                            <div className="track !h-1.5"><span style={{ width: `${topic.percent}%` }} /></div>
                            <ul className="space-y-2">
                                {topic.subTopics.map((node) => {
                                    const nc = skillColor(node.color);
                                    return (
                                        <li key={node.subTopic}>
                                            <Link
                                                to={practicePath(topic.subject, topic.topic, node.subTopic)}
                                                className={`row-card !py-2.5 flex items-center justify-between gap-3 border ${nc.cls}`}
                                                title="לתרגול"
                                            >
                                                <span className="flex items-center gap-2 min-w-0">
                                                    <span className={`w-2.5 h-2.5 shrink-0 rotate-45 rounded-[3px] ${nc.dot}`} />
                                                    <span className="truncate text-sm font-medium">{node.subTopic}</span>
                                                </span>
                                                <span className="text-xs whitespace-nowrap">
                                                    {node.attempts > 0 ? (
                                                        <>
                                                            <span className="num">{node.rating}</span>
                                                            {" · "}
                                                            <span className="num">{node.correctCount}/{node.attempts}</span>
                                                        </>
                                                    ) : nc.label}
                                                    {" · רמה "}
                                                    <span className="num">{node.nextLevel}</span>
                                                </span>
                                            </Link>
                                        </li>
                                    );
                                })}
                            </ul>
                        </section>
                    );
                })}
            </div>
        </div>
    );
}

export default SkillMap;
