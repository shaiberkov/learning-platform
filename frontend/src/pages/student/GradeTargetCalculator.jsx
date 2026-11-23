import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { FiTarget } from "react-icons/fi";
import EmptyState from "../../components/ui/EmptyState.jsx";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import { errorMessage } from "../../lib/api.js";
import { fetchGradeTarget } from "../../features/examsim/examSimApi.js";
import { difficultyLabel, formatScore, parseTarget, targetStatusInfo } from "../../features/examsim/examSim.js";

const TONE_CHIP = { mint: "chip--mint", danger: "chip--danger", accent: "", neutral: "" };
const TONE_NOTICE = { mint: "notice--mint", danger: "notice--danger", accent: "", neutral: "" };

function StatusChip({ status }) {
    const info = targetStatusInfo(status);
    return <span className={`chip ${TONE_CHIP[info.tone]}`}>{info.label}</span>;
}

function SubjectCard({ plan, index }) {
    const info = targetStatusInfo(plan.status);
    const weights = Object.entries(plan.weights || {}).filter(([, w]) => w > 0);
    return (
        <section className="glass p-5 sm:p-6 space-y-4 reveal" style={{ "--i": Math.min(index + 1, 8) }}
                 aria-labelledby={`target-subject-${index}`}>
            <header className="flex flex-wrap items-start justify-between gap-2">
                <div>
                    <h2 id={`target-subject-${index}`} className="title text-lg">{plan.title}</h2>
                    <p className="faint text-xs">המורה: {plan.teacherName}</p>
                </div>
                <StatusChip status={plan.status} />
            </header>

            <dl className="grid grid-cols-3 gap-3 text-center">
                <div className="row-card !p-3">
                    <dt className="faint text-xs">ציון נוכחי</dt>
                    <dd className="num text-xl font-semibold text-ink">{formatScore(plan.currentGrade)}</dd>
                </div>
                <div className="row-card !p-3">
                    <dt className="faint text-xs">מינימום אפשרי</dt>
                    <dd className="num text-xl font-semibold text-ink">{formatScore(plan.minPossible)}</dd>
                </div>
                <div className="row-card !p-3">
                    <dt className="faint text-xs">מקסימום אפשרי</dt>
                    <dd className="num text-xl font-semibold text-ink">{formatScore(plan.maxPossible)}</dd>
                </div>
            </dl>

            <p className={`notice ${TONE_NOTICE[info.tone]}`}>{plan.message}</p>

            {plan.remaining.length > 0 && (
                <div className="space-y-2">
                    <h3 className="text-sm font-medium text-ink-2">מה נשאר</h3>
                    <table className="data-table w-full text-sm">
                        <thead>
                            <tr>
                                <th scope="col">מטלה</th>
                                <th scope="col">סוג</th>
                                <th scope="col">תאריך</th>
                                <th scope="col">צריך לפחות</th>
                            </tr>
                        </thead>
                        <tbody>
                            {plan.remaining.map((item, i) => (
                                <tr key={i}>
                                    <td>{item.title}</td>
                                    <td>{item.categoryLabel}</td>
                                    <td className="num">{item.date || "—"}</td>
                                    <td className="num font-semibold">
                                        {item.requiredScore === null || item.requiredScore === undefined
                                            ? "—"
                                            : `${formatScore(item.requiredScore)} / ${formatScore(item.maxScore)}`}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {weights.length > 0 && (
                <p className="faint text-xs">
                    משקלות שהמורה הגדיר:{" "}
                    {weights.map(([cat, w]) => `${plan.categoryLabels?.[cat] ?? cat} ${w}%`).join(" · ")}
                </p>
            )}
        </section>
    );
}

// "What do I need?": the student enters a target and sees the score needed in every remaining
// assignment, per subject and for the certificate average, from the teachers' gradebooks and weights.
export default function GradeTargetCalculator() {
    const [input, setInput] = useState("90");
    const [inputError, setInputError] = useState(null);
    const [target, setTarget] = useState(null);

    const { data, isFetching, isError, error } = useQuery({
        queryKey: ["gradeTarget", target],
        queryFn: () => fetchGradeTarget(target),
        enabled: target !== null,
    });

    const onSubmit = (e) => {
        e.preventDefault();
        const parsed = parseTarget(input);
        if (parsed.error) {
            setInputError(parsed.error);
            return;
        }
        setInputError(null);
        setTarget(parsed.value);
    };

    const overall = data?.overall;
    const overallInfo = overall ? targetStatusInfo(overall.status) : null;
    const required = overall?.requiredPercent;

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 reveal space-y-5">
                <div className="flex items-center gap-3">
                    <span className="icon-badge"><FiTarget aria-hidden="true" /></span>
                    <div>
                        <span className="eyebrow">What do I need? · מה אני צריך</span>
                        <h1 className="display text-3xl sm:text-4xl mt-1">כמה אני צריך במבחנים שנשארו?</h1>
                        <p className="muted text-sm mt-2 max-w-2xl">
                            הזינו את הציון שאתם רוצים בתעודה. החישוב משתמש ביומן הציונים ובמשקלות שהמורים הגדירו,
                            ומראה איזה ציון צריך בכל מטלה שנשארה.
                        </p>
                    </div>
                </div>
                <form onSubmit={onSubmit} className="flex flex-wrap items-end gap-3" noValidate>
                    <div className="field-wrap">
                        <label htmlFor="grade-target-input" className="label">ציון היעד בתעודה</label>
                        <input
                            id="grade-target-input"
                            className="field num w-32"
                            inputMode="decimal"
                            dir="ltr"
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            aria-invalid={inputError ? "true" : "false"}
                            aria-describedby={inputError ? "grade-target-error" : undefined}
                        />
                        {inputError && <p id="grade-target-error" className="field-error">{inputError}</p>}
                    </div>
                    <button type="submit" className="btn btn-primary">חשב</button>
                </form>
            </div>

            {target === null && (
                <div className="glass">
                    <EmptyState title="הזינו יעד כדי להתחיל" body="לדוגמה: 90. החישוב מתעדכן לפי הציונים שהמורים הזינו עד היום." />
                </div>
            )}
            {target !== null && isFetching && <SkeletonList rows={2} />}
            {target !== null && !isFetching && isError && (
                <p className="notice notice--danger" role="alert">{errorMessage(error, "החישוב נכשל, נסו שוב")}</p>
            )}

            {target !== null && !isFetching && data && (
                <>
                    {overall && (
                        <section className="glass p-6 sm:p-7 reveal space-y-3" aria-labelledby="grade-target-overall">
                            <div className="flex flex-wrap items-center justify-between gap-2">
                                <h2 id="grade-target-overall" className="title text-xl">ממוצע התעודה</h2>
                                <span className={`chip ${TONE_CHIP[overallInfo.tone]}`}>{overallInfo.label}</span>
                            </div>
                            <p className="text-ink">
                                ממוצע נוכחי <span className="num font-semibold">{formatScore(overall.currentAverage)}</span>
                                {" · "}טווח אפשרי <span className="num" dir="ltr">{formatScore(overall.minPossible)}–{formatScore(overall.maxPossible)}</span>
                            </p>
                            {required !== null && required !== undefined && overall.status === "POSSIBLE" && (
                                <p className="flex items-center gap-2">
                                    <span className="num text-4xl font-semibold text-accent">{formatScore(required)}</span>
                                    <span className="muted text-sm">ממוצע נדרש בכל המטלות שנשארו</span>
                                    <span className="chip">{difficultyLabel(required)}</span>
                                </p>
                            )}
                            <p className={`notice ${TONE_NOTICE[overallInfo.tone]}`} role="status">{overall.message}</p>
                        </section>
                    )}
                    {data.subjects.length === 0 && (
                        <div className="glass">
                            <EmptyState title="אין עדיין יומן ציונים" body="למורים של הכיתה שלך עדיין אין ציונים או מטלות ביומן." />
                        </div>
                    )}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-5">
                        {data.subjects.map((plan, i) => <SubjectCard key={plan.key} plan={plan} index={i} />)}
                    </div>
                </>
            )}
        </div>
    );
}
