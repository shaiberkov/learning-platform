import { useContext, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { FaArrowRight, FaCheck, FaPuzzlePiece, FaRedo } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import OrbitLoader from "../../components/ui/OrbitLoader.jsx";
import ReadAloudButton from "../../components/ReadAloudButton.jsx";
import { apiGet, apiPost, errorText } from "../../Utils/authApi.js";
import {
    LESSON_CONTENT_BASE_PATH, LESSON_CONTENT_GET, LESSON_CONTENT_UPDATE_PROGRESS,
} from "../../constants/pages.constants.js";

const STEPS = ["learn", "practice", "review"];
const STEP_LABEL = { learn: "1 · למידה", practice: "2 · תרגול", review: "3 · חזרה" };

// One lesson page in three steps: learn (text, video, examples) -> practice on the same topic
// -> review. Each finished step is saved and gives XP once.
export default function LessonView() {
    const { lessonId } = useParams();
    const { user } = useContext(UserContext);
    const navigate = useNavigate();
    const [lesson, setLesson] = useState(null);
    const [step, setStep] = useState("learn");
    const [error, setError] = useState("");

    useEffect(() => {
        if (!user?.userId) return;
        apiGet(`${LESSON_CONTENT_BASE_PATH}${LESSON_CONTENT_GET}`, { lessonId, userId: user.userId })
            .then((res) => {
                if (!res.success) return setError(res.errorCode);
                setLesson(res.data);
                setStep(res.data.reviewed || res.data.practiced ? "review" : res.data.learned ? "practice" : "learn");
            })
            .catch((err) => setError(errorText(err)));
    }, [lessonId, user?.userId]);

    const markStage = async (stage) => {
        try {
            const res = await apiPost(`${LESSON_CONTENT_BASE_PATH}${LESSON_CONTENT_UPDATE_PROGRESS}`,
                { studentId: user.userId, lessonId, stage });
            if (res.success) setLesson(res.data);
            else setError(res.errorCode);
            return res.success;
        } catch (err) {
            setError(errorText(err));
            return false;
        }
    };

    const practicePath = lesson?.subTopic
        ? `/exercises/${lesson.subject}/${lesson.topic}/${lesson.subTopic}`
        : "/question-practice-selector";

    if (!lesson && !error) return <OrbitLoader title="טוען שיעור..." />;
    if (!lesson) return <div className="page page--medium" dir="rtl"><div className="notice notice--danger">{error}</div></div>;

    const fullText = [lesson.title, lesson.body, ...lesson.examples].filter(Boolean).join(". ");

    return (
        <div className="page page--medium space-y-5" dir="rtl">
            <Link to="/lessons" className="link inline-flex items-center gap-2"><FaArrowRight /> לכל השיעורים</Link>

            <div className="glass p-6 sm:p-8 space-y-4 reveal">
                <span className="eyebrow">{lesson.subject} · {lesson.topic}{lesson.subTopic ? ` · ${lesson.subTopic}` : ""}</span>
                <div className="flex items-start justify-between gap-3">
                    <h1 className="display text-3xl sm:text-4xl">{lesson.title}</h1>
                    <ReadAloudButton text={fullText} label="הקרא את השיעור" />
                </div>
                <div className="grid grid-cols-3 gap-2" role="tablist">
                    {STEPS.map((s) => {
                        const done = s === "learn" ? lesson.learned : s === "practice" ? lesson.practiced : lesson.reviewed;
                        return (
                            <button key={s} role="tab" aria-selected={step === s}
                                    className={`choice ${step === s ? "is-active" : ""}`} onClick={() => setStep(s)}>
                                {done && <FaCheck className="inline ml-1 text-mint" />}{STEP_LABEL[s]}
                            </button>
                        );
                    })}
                </div>
                {error && <div className="notice notice--danger">{error}</div>}
            </div>

            {step === "learn" && (
                <div className="glass p-6 sm:p-8 space-y-6 reveal">
                    {lesson.body && <div className="prose-lp whitespace-pre-line text-ink leading-relaxed">{lesson.body}</div>}
                    {lesson.videoEmbedUrl && (
                        <div className="aspect-video w-full overflow-hidden rounded-2xl border border-line">
                            <iframe
                                src={lesson.videoEmbedUrl}
                                title={lesson.title}
                                className="w-full h-full"
                                allow="accelerometer; encrypted-media; gyroscope; picture-in-picture"
                                allowFullScreen
                                sandbox="allow-scripts allow-same-origin allow-presentation"
                            />
                        </div>
                    )}
                    {lesson.examples.length > 0 && (
                        <div className="space-y-3">
                            <h3 className="title text-xl">דוגמאות פתורות</h3>
                            {lesson.examples.map((ex, i) => (
                                <div key={i} className="row-card flex items-start justify-between gap-3">
                                    <p className="whitespace-pre-line text-ink">{ex}</p>
                                    <ReadAloudButton text={ex} />
                                </div>
                            ))}
                        </div>
                    )}
                    <button className="btn btn-primary" onClick={async () => { if (await markStage("LEARNED")) setStep("practice"); }}>
                        <FaCheck /> קראתי והבנתי – לתרגול
                    </button>
                </div>
            )}

            {step === "practice" && (
                <div className="glass p-6 sm:p-8 space-y-4 reveal">
                    <h3 className="title text-2xl">תרגול על אותו נושא</h3>
                    <p className="muted">
                        {lesson.subTopic
                            ? `התרגול יפתח שאלות ב"${lesson.subTopic}" ברמה שמתאימה לך. אחרי כמה שאלות, חזור לכאן לחזרה.`
                            : "בחר תרגול בנושא השיעור. אחרי כמה שאלות, חזור לכאן לחזרה."}
                    </p>
                    <button className="btn btn-primary" onClick={async () => { await markStage("PRACTICED"); navigate(practicePath); }}>
                        <FaPuzzlePiece /> לתרגול
                    </button>
                    <button className="btn btn-ghost" onClick={() => setStep("review")}>כבר תרגלתי – לחזרה</button>
                </div>
            )}

            {step === "review" && (
                <div className="glass p-6 sm:p-8 space-y-4 reveal">
                    <h3 className="title text-2xl">חזרה</h3>
                    <p className="muted">עבור שוב על הדוגמאות ונסה לפתור כל אחת בעצמך לפני שאתה קורא את הפתרון.</p>
                    {lesson.examples.length === 0 && <p className="muted">אין דוגמאות בשיעור הזה. עבור שוב על שלב הלמידה.</p>}
                    {lesson.examples.map((ex, i) => (
                        <details key={i} className="row-card">
                            <summary className="cursor-pointer text-ink font-semibold">דוגמה {i + 1}</summary>
                            <p className="whitespace-pre-line text-ink mt-2">{ex}</p>
                        </details>
                    ))}
                    {lesson.reviewed ? (
                        <div className="notice notice--mint">סיימת את השיעור: למידה, תרגול וחזרה. כל הכבוד!</div>
                    ) : (
                        <button className="btn btn-mint" onClick={() => markStage("REVIEWED")}><FaRedo /> סיימתי חזרה</button>
                    )}
                </div>
            )}
        </div>
    );
}
