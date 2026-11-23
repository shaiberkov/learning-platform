// [s05] Choose what to summarize: an upcoming test, topics from the mistakes notebook, or both.
import { formatTestStart, toggleTopic } from "./studySummaryUtils.js";

export default function SummaryPicker({ options, value, onChange, idPrefix }) {
    const tests = options?.tests || [];
    const topics = options?.topics || [];
    const selectId = `${idPrefix}-test`;

    return (
        <div className="space-y-4">
            <div className="field-wrap">
                <label htmlFor={selectId} className="label">מבחן קרוב</label>
                <select
                    id={selectId}
                    className="field"
                    value={value.testId}
                    onChange={(e) => onChange({ ...value, testId: e.target.value })}
                >
                    <option value="">{tests.length ? "בלי מבחן – רק נושאים" : "אין מבחנים קרובים"}</option>
                    {tests.map((t) => (
                        <option key={t.testId} value={t.testId}>
                            {(t.title || t.topic || "מבחן")} · {formatTestStart(t.startTime)}
                        </option>
                    ))}
                </select>
            </div>

            <fieldset className="space-y-2">
                <legend className="label">נושאים (מתוך מחברת הטעויות)</legend>
                {topics.length === 0 ? (
                    <p className="faint text-sm">אין עדיין נושאים עם טעויות. אפשר לבחור מבחן.</p>
                ) : (
                    <div className="flex flex-wrap gap-2">
                        {topics.map((topic) => {
                            const active = value.topics.includes(topic);
                            return (
                                <button
                                    key={topic}
                                    type="button"
                                    aria-pressed={active}
                                    onClick={() => onChange({ ...value, topics: toggleTopic(value.topics, topic) })}
                                    className={`chip ${active ? "chip--mint" : ""}`}
                                >
                                    {topic}
                                </button>
                            );
                        })}
                    </div>
                )}
                <p className="faint text-xs">בלי בחירה ניצור סיכום על הנושאים שבהם טעית הכי הרבה.</p>
            </fieldset>
        </div>
    );
}
