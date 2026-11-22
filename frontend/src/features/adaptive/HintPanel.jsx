import { useState } from "react";
import { FaLightbulb, FaListOl } from "react-icons/fa";
import { fetchPracticeHint } from "./adaptiveApi.js";

const STEP_LABELS = ["רמז 1", "רמז 2", "פתרון מלא"];
const POINTS_AFTER = [7, 4, 0];

/**
 * Hint 1 → hint 2 → full solution for one practice question. Steps open in order and each
 * one lowers the points the answer can earn. Render it with key={questionId} so it resets.
 */
function HintPanel({ userId, questionId }) {
    const [steps, setSteps] = useState([]);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);

    const opened = steps.length;
    const nextStep = opened + 1;

    const open = async () => {
        if (!questionId || nextStep > 3) return;
        setLoading(true);
        const res = await fetchPracticeHint(userId, questionId, nextStep);
        setLoading(false);
        if (res.success) {
            setSteps((s) => [...s, res.data]);
            setError(null);
        } else {
            setError(res.errorCode);
        }
    };

    const available = opened === 0 ? 10 : steps[opened - 1].pointsAvailable;

    return (
        <div className="glass--sunk p-4 sm:p-5 space-y-3">
            <div className="flex items-center justify-between gap-2">
                <span className="eyebrow flex items-center gap-2"><FaLightbulb /> עזרה מודרכת</span>
                <span className="text-xs text-ink-2">
                    ניקוד אפשרי: <span className="num font-semibold text-ink">{available}</span>/10
                </span>
            </div>

            {steps.map((s) => (
                <div key={s.step} className="row-card !py-3 space-y-1.5">
                    <span className="label !text-xs">{STEP_LABELS[s.step - 1]}</span>
                    {s.text && <p className="text-sm text-ink leading-relaxed">{s.text}</p>}
                    {s.solutionSteps && (
                        <ol className="space-y-1 text-sm text-ink list-decimal pr-5">
                            {s.solutionSteps.map((line, i) => <li key={i}>{line}</li>)}
                            <li>
                                התשובה: <span className="num font-semibold" dir="ltr">{s.answer}</span>
                            </li>
                        </ol>
                    )}
                </div>
            ))}

            {error && <p className="notice notice--danger !text-sm">{error}</p>}

            {nextStep <= 3 && (
                <button type="button" onClick={open} disabled={loading || !questionId} className="btn btn-ghost btn-sm">
                    {nextStep === 3 ? <FaListOl /> : <FaLightbulb />}
                    הצג {STEP_LABELS[nextStep - 1]}
                    <span className="faint text-xs">(הניקוד יורד ל-<span className="num">{POINTS_AFTER[nextStep - 1]}</span>)</span>
                </button>
            )}
        </div>
    );
}

export default HintPanel;
