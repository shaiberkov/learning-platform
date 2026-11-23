import { Link } from "react-router-dom";
import ProgressRing from "../../components/ui/ProgressRing.jsx";
import MathText from "../../components/exam/MathText.jsx";
import { formatScore } from "./examSim.js";

const STATUS = {
    CORRECT: { mark: "✓", cls: "text-mint", label: "נכון" },
    PARTIAL: { mark: "½", cls: "text-amber", label: "ניקוד חלקי" },
    WRONG: { mark: "✕", cls: "text-danger", label: "שגוי" },
};

// A graded simulation: the score against the prediction, then every section with the rubric per question.
export default function SimResult({ result, message }) {
    const predicted = result.predictedScore !== null && result.predictedScore !== undefined;
    const inRange = predicted && Math.abs(result.score - result.predictedScore) <= (result.predictedMargin ?? 0);

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            {message && <div className="notice" role="status">{message}</div>}
            <div className="grid grid-cols-1 md:grid-cols-12 gap-5">
                <div className="md:col-span-5 glass p-8 flex flex-col items-center gap-4 reveal">
                    <span className="eyebrow">ציון לפי מחוון</span>
                    <ProgressRing value={result.score} size={180} stroke={14}>
                        <span className="grid">
                            <span className="num text-5xl font-semibold text-ink">{result.score}</span>
                            <span className="faint text-xs mt-1">ציון</span>
                        </span>
                    </ProgressRing>
                    <p className="title text-lg text-center">{result.title}</p>
                    {result.autoSubmitted && <span className="chip chip--amber">הוגש אוטומטית בתום הזמן</span>}
                    {predicted && (
                        <p className="muted text-sm text-center">
                            התחזית לפני המבחן: <span className="num" dir="ltr">{result.predictedScore} ± {result.predictedMargin}</span>
                            {" · "}
                            {inRange ? "הציון בטווח הצפוי" : result.score > result.predictedScore ? "מעל התחזית!" : "מתחת לתחזית"}
                        </p>
                    )}
                    <Link to="/exam-simulations" className="btn btn-ghost">חזרה למאגר הבחינות</Link>
                </div>

                <div className="md:col-span-7 space-y-4">
                    {result.sections.map((section, si) => (
                        <section key={si} className="glass p-5 sm:p-6 space-y-3 reveal" style={{ "--i": si + 1 }}
                                 aria-label={section.title}>
                            <header className="flex flex-wrap items-center justify-between gap-2">
                                <h2 className="title text-lg">{section.title}</h2>
                                <span className="chip num">{formatScore(section.earned)}/{formatScore(section.possible)} נק׳</span>
                            </header>
                            {section.questionsToCount && (
                                <p className="faint text-xs">נספרות {section.questionsToCount} השאלות הטובות ביותר בפרק</p>
                            )}
                            <ul className="space-y-2">
                                {section.questions.map((q) => {
                                    const s = STATUS[q.status] || STATUS.WRONG;
                                    return (
                                        <li key={q.questionId} className={`row-card space-y-2 ${q.counted ? "" : "opacity-60"}`}>
                                            <div className="flex items-start gap-3">
                                                <span className={`${s.cls} font-semibold`} aria-label={s.label}>{s.mark}</span>
                                                <span className="num text-xs text-ink-3 pt-0.5">{q.number}.</span>
                                                <span className="flex-1 text-ink text-sm whitespace-pre-line"><MathText text={q.text} /></span>
                                                <span className="num text-xs faint">{formatScore(q.pointsEarned)}/{q.points}</span>
                                            </div>
                                            <dl className="grid sm:grid-cols-2 gap-x-4 gap-y-1 text-xs">
                                                <div><dt className="faint inline">התשובה שלך: </dt><dd className="inline num" dir="ltr">{q.yourAnswer || "—"}</dd></div>
                                                <div><dt className="faint inline">תשובה נכונה: </dt><dd className="inline num" dir="ltr">{q.expectedAnswer}</dd></div>
                                            </dl>
                                            {q.rubric && <p className="muted text-xs"><span className="font-medium">מחוון: </span>{q.rubric}</p>}
                                            {!q.counted && <p className="faint text-xs">השאלה לא נספרה בציון (נבחרו התשובות הטובות יותר בפרק)</p>}
                                        </li>
                                    );
                                })}
                            </ul>
                        </section>
                    ))}
                </div>
            </div>
        </div>
    );
}
