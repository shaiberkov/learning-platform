import { Link } from "react-router-dom";
import { FaChartPie, FaClock, FaLightbulb } from "react-icons/fa";
import { formatSeconds, practicePath } from "./adaptiveApi.js";

const barTone = (percent) => (percent >= 80 ? "bg-mint" : percent >= 50 ? "bg-amber" : "bg-danger");

/** Post-exam feedback: by topic, time per question, anonymous class comparison, recommendation. */
function ExamFeedback({ feedback }) {
    const { topics, questions, recommendation } = feedback;
    const maxSeconds = Math.max(1, ...questions.map((q) => q.secondsSpent || 0));

    return (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 sm:gap-5" dir="rtl">
            {recommendation && (
                <div className="lg:col-span-12 glass p-5 sm:p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 reveal">
                    <div className="flex items-center gap-3">
                        <span className="icon-badge icon-badge--amber"><FaLightbulb /></span>
                        <div>
                            <span className="eyebrow">המלצה</span>
                            <p className="text-ink font-medium mt-1">{recommendation.message}</p>
                        </div>
                    </div>
                    <Link
                        to={practicePath(recommendation.subject, recommendation.topic, recommendation.subTopic)}
                        className="btn btn-primary shrink-0"
                    >
                        לתרגול {recommendation.subTopic}
                    </Link>
                </div>
            )}

            <section className="lg:col-span-6 glass p-5 sm:p-6 space-y-4 reveal" style={{ "--i": 1 }}>
                <h3 className="title text-lg flex items-center gap-2"><FaChartPie /> לפי נושא</h3>
                {feedback.classAverageScore !== null && (
                    <p className="text-sm text-ink-2">
                        הציון שלך <span className="num font-semibold text-ink">{feedback.score}</span>
                        {" · "}ממוצע הכיתה במבחן <span className="num font-semibold text-ink">{feedback.classAverageScore}</span>
                    </p>
                )}
                <ul className="space-y-4">
                    {topics.map((t) => (
                        <li key={t.subTopic} className="space-y-1.5">
                            <div className="flex items-center justify-between gap-2 text-sm">
                                <span className="font-medium text-ink">{t.subTopic}</span>
                                <span className="text-ink-2">
                                    <span className="num">{t.correct}/{t.total}</span>
                                    {t.averageSeconds !== null && <> · <span className="num">{formatSeconds(t.averageSeconds)}</span> לשאלה</>}
                                </span>
                            </div>
                            <div className="relative h-2.5 rounded-full bg-surface-sunk overflow-hidden">
                                <span className={`absolute inset-y-0 right-0 rounded-full ${barTone(t.percent)}`} style={{ width: `${t.percent}%` }} />
                            </div>
                            <div className="flex justify-between text-xs text-ink-3">
                                <span>אתה: <span className="num">{t.percent}%</span></span>
                                <span>
                                    {t.classAveragePercent !== null
                                        ? <>ממוצע הכיתה: <span className="num">{t.classAveragePercent}%</span></>
                                        : "אין עדיין מספיק נתוני כיתה להשוואה"}
                                </span>
                            </div>
                        </li>
                    ))}
                </ul>
                <p className="faint text-xs">ההשוואה לכיתה אנונימית ומוצגת רק כשלפחות 3 תלמידים נוספים ענו על הנושא.</p>
            </section>

            <section className="lg:col-span-6 glass p-5 sm:p-6 space-y-4 reveal" style={{ "--i": 2 }}>
                <h3 className="title text-lg flex items-center gap-2">
                    <FaClock /> זמן לכל שאלה
                    {feedback.totalSeconds !== null && (
                        <span className="chip !text-xs">סה״כ <span className="num">{formatSeconds(feedback.totalSeconds)}</span></span>
                    )}
                </h3>
                <ul className="space-y-2">
                    {questions.map((q, i) => (
                        <li key={q.questionId} className="space-y-1">
                            <div className="flex items-center justify-between gap-2 text-sm">
                                <span className="flex items-center gap-2 min-w-0">
                                    <span className={q.correct ? "text-mint" : "text-danger"}>{q.correct ? "✓" : "✕"}</span>
                                    <span className="num text-ink-3">Q{String(i + 1).padStart(2, "0")}</span>
                                    <span className="num truncate" dir="ltr">{q.questionText}</span>
                                </span>
                                <span className="num text-ink-2 whitespace-nowrap">{formatSeconds(q.secondsSpent)}</span>
                            </div>
                            <div className="h-1.5 rounded-full bg-surface-sunk overflow-hidden">
                                <span
                                    className={`block h-full rounded-full ${q.correct ? "bg-mint" : "bg-danger"}`}
                                    style={{ width: `${((q.secondsSpent || 0) / maxSeconds) * 100}%` }}
                                />
                            </div>
                            {!q.correct && (
                                <p className="text-xs text-ink-3">
                                    ענית <span className="num" dir="ltr">{q.givenAnswer || "—"}</span>
                                    {" · "}התשובה <span className="num" dir="ltr">{q.correctAnswer}</span>
                                </p>
                            )}
                        </li>
                    ))}
                </ul>
                <p className="faint text-xs">שאלות שטעית בהן נוספו למחברת הטעויות לחזרה.</p>
            </section>
        </div>
    );
}

export default ExamFeedback;
