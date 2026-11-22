import { useContext, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { FaBookOpen } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import OrbitLoader from "../../components/ui/OrbitLoader.jsx";
import { apiGet, errorText } from "../../Utils/authApi.js";
import { LESSON_CONTENT_BASE_PATH, LESSON_CONTENT_STUDENT_LESSONS } from "../../constants/pages.constants.js";

const STAGES = [
    { key: "learned", label: "למידה" },
    { key: "practiced", label: "תרגול" },
    { key: "reviewed", label: "חזרה" },
];

// Lesson pages written by the student's teachers, with where the student is in each.
export default function Lessons() {
    const { user } = useContext(UserContext);
    const [lessons, setLessons] = useState(null);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!user?.userId) return;
        apiGet(`${LESSON_CONTENT_BASE_PATH}${LESSON_CONTENT_STUDENT_LESSONS}`, { studentId: user.userId })
            .then((res) => (res.success ? setLessons(res.data) : setError(res.errorCode)))
            .catch((err) => setError(errorText(err)));
    }, [user?.userId]);

    if (!lessons && !error) return <OrbitLoader title="טוען שיעורים..." />;

    return (
        <div className="page page--wide" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-6 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge"><FaBookOpen /></span>
                    <div>
                        <span className="eyebrow">Lessons · שיעורים</span>
                        <h2 className="display text-3xl sm:text-4xl">לומדים, מתרגלים, חוזרים</h2>
                    </div>
                </div>
                {error && <div className="notice notice--danger">{error}</div>}
                {lessons?.length === 0 && <p className="muted">המורים שלך עוד לא פרסמו שיעורים.</p>}
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    {lessons?.map((l, i) => (
                        <Link key={l.id} to={`/lessons/${l.id}`} className="glass tile p-5 space-y-3 reveal" style={{ "--i": Math.min(i, 8) }}>
                            <span className="eyebrow">{l.subject} · {l.topic}</span>
                            <h3 className="title text-xl">{l.title}</h3>
                            <div className="flex gap-2 flex-wrap">
                                {STAGES.map((s) => (
                                    <span key={s.key} className={`chip ${l[s.key] ? "chip--mint" : ""}`}>{s.label}</span>
                                ))}
                            </div>
                        </Link>
                    ))}
                </div>
            </div>
        </div>
    );
}
