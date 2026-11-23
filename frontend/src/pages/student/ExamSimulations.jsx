import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { FiClock, FiLayers, FiPlay, FiRotateCcw, FiTarget } from "react-icons/fi";
import { FaGraduationCap } from "react-icons/fa";
import EmptyState from "../../components/ui/EmptyState.jsx";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import { errorMessage } from "../../lib/api.js";
import { fetchPrediction, fetchSimExams } from "../../features/examsim/examSimApi.js";

// The expected score for one exam, from the student's mastery and past simulations.
function PredictionLine({ examId }) {
    const { data, isLoading, isError } = useQuery({
        queryKey: ["examSimPrediction", examId],
        queryFn: () => fetchPrediction(examId),
    });
    if (isLoading) return <p className="faint text-sm" role="status">מחשב תחזית ציון...</p>;
    if (isError || !data) return null;
    return (
        <div className="notice !py-2.5 space-y-1">
            <p className="flex items-center gap-2 font-medium text-ink">
                <FiTarget aria-hidden="true" />
                <span>
                    לפי הביצועים שלך, הציון הצפוי הוא{" "}
                    <span className="num" dir="ltr">{data.expectedScore} ± {data.margin}</span>
                </span>
            </p>
            <p className="faint text-xs">{data.basis}</p>
        </div>
    );
}

// The past-exams bank: full-condition simulations with a real clock, the original structure and a rubric.
export default function ExamSimulations() {
    const { data: exams, isLoading, isError, error } = useQuery({
        queryKey: ["examSimExams"],
        queryFn: fetchSimExams,
    });

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 reveal flex items-center gap-3">
                <span className="icon-badge"><FaGraduationCap aria-hidden="true" /></span>
                <div>
                    <span className="eyebrow">Exam simulation · סימולציית בגרות</span>
                    <h1 className="display text-3xl sm:text-4xl mt-1">מבחן בתנאים מלאים</h1>
                    <p className="muted text-sm mt-2 max-w-2xl">
                        זמן אמיתי שנמדד בשרת, אותו מבנה של פרקים ושאלות, וציון לפי מחוון עם ניקוד חלקי.
                        לפני כל מבחן מוצגת תחזית ציון לפי רמת השליטה שלך והסימולציות הקודמות.
                    </p>
                </div>
            </div>

            {isLoading && <SkeletonList rows={3} />}
            {isError && <p className="notice notice--danger">{errorMessage(error, "טעינת מאגר הבחינות נכשלה")}</p>}
            {!isLoading && !isError && (!exams || exams.length === 0) && (
                <div className="glass">
                    <EmptyState title="מאגר הבחינות ריק" body="עדיין לא נוספו בחינות לסימולציה. נסו שוב מאוחר יותר." />
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-5">
                {(exams || []).map((exam, i) => (
                    <section key={exam.id} className="glass p-5 sm:p-6 space-y-4 reveal" style={{ "--i": Math.min(i, 8) }}
                             aria-labelledby={`sim-exam-${exam.id}`}>
                        <header className="space-y-1">
                            <div className="flex flex-wrap items-center gap-2">
                                {exam.examYear && <span className="chip num">{exam.examYear}</span>}
                                {exam.sessionLabel && <span className="chip">{exam.sessionLabel}</span>}
                                {exam.subject && <span className="chip">{exam.subject}</span>}
                            </div>
                            <h2 id={`sim-exam-${exam.id}`} className="title text-xl">{exam.title}</h2>
                            {exam.description && <p className="muted text-sm leading-relaxed">{exam.description}</p>}
                        </header>

                        <ul className="flex flex-wrap gap-4 text-sm text-ink-2">
                            <li className="flex items-center gap-1.5"><FiClock aria-hidden="true" /> <span className="num">{exam.durationMinutes}</span> דקות</li>
                            <li className="flex items-center gap-1.5"><FiLayers aria-hidden="true" /> <span className="num">{exam.sectionCount}</span> פרקים · <span className="num">{exam.questionCount}</span> שאלות</li>
                            {exam.attemptsCount > 0 && (
                                <li>
                                    ציון אחרון <span className="num font-semibold text-ink">{exam.lastScore}</span>
                                    {" · "}הטוב ביותר <span className="num font-semibold text-mint">{exam.bestScore}</span>
                                </li>
                            )}
                        </ul>

                        <PredictionLine examId={exam.id} />

                        <div className="flex flex-wrap gap-2">
                            <Link to={`/exam-sim/${exam.id}`} className="btn btn-primary">
                                {exam.inProgressAttemptId ? <><FiRotateCcw aria-hidden="true" /> המשך סימולציה</> : <><FiPlay aria-hidden="true" /> התחל סימולציה</>}
                            </Link>
                            {exam.lastAttemptId && (
                                <Link to={`/exam-sim/result/${exam.lastAttemptId}`} className="btn btn-ghost">
                                    לתוצאות האחרונות
                                </Link>
                            )}
                        </div>
                    </section>
                ))}
            </div>
        </div>
    );
}
