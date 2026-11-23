import { FiPlus, FiTrash2, FiX } from "react-icons/fi";
import MathText from "./MathText.jsx";
import { KIND_LABELS } from "./examApi.js";
import { AUTHOR_KINDS, emptyQuestion } from "./examForms.js";

// Editor for one authored question. Formulas in the text go between $...$ and are previewed with KaTeX.
function ListEditor({ items, onChange, placeholder, renderExtra, min = 2 }) {
    const update = (i, value) => onChange(items.map((x, j) => (j === i ? value : x)));
    const remove = (i) => onChange(items.filter((_, j) => j !== i), i);
    return (
        <div className="grid gap-2">
            {items.map((item, i) => (
                <div key={i} className="flex items-center gap-2">
                    {renderExtra && renderExtra(i)}
                    <input className="field flex-1" value={item} placeholder={`${placeholder} ${i + 1}`} onChange={(e) => update(i, e.target.value)} />
                    <button type="button" className="btn btn-ghost btn-sm" aria-label="הסרה" disabled={items.length <= min} onClick={() => remove(i)}>
                        <FiX />
                    </button>
                </div>
            ))}
            <button type="button" className="btn btn-ghost btn-sm justify-self-start" onClick={() => onChange([...items, ""])}>
                <FiPlus /> הוספה
            </button>
        </div>
    );
}

export default function QuestionEditor({ index, question, onChange, onRemove }) {
    const set = (patch) => onChange({ ...question, ...patch });

    const toggleCorrect = (i) =>
        set({
            correctOptions: question.correctOptions.includes(i)
                ? question.correctOptions.filter((x) => x !== i)
                : [...question.correctOptions, i].sort((a, b) => a - b),
        });

    // Keep option indexes valid after removing an option.
    const removeShift = (list, removed) =>
        list.filter((x) => x !== removed).map((x) => (x > removed ? x - 1 : x));

    return (
        <div className="glass p-5 space-y-4 reveal">
            <div className="flex flex-wrap items-center gap-3">
                <span className="num text-xs text-ink-3">שאלה {index + 1}</span>
                <select className="field !w-auto" value={question.kind} onChange={(e) => onChange({ ...emptyQuestion(e.target.value), text: question.text, points: question.points })}>
                    {AUTHOR_KINDS.map((k) => <option key={k} value={k}>{KIND_LABELS[k]}</option>)}
                </select>
                <label className="flex items-center gap-2 text-sm muted">
                    ניקוד
                    <input type="number" min="1" max="100" className="field num !w-20" value={question.points} onChange={(e) => set({ points: e.target.value })} />
                </label>
                <button type="button" className="btn btn-danger btn-sm ms-auto" onClick={onRemove}><FiTrash2 /> הסרה</button>
            </div>

            <div>
                <label className="label">נוסח השאלה (נוסחאות בין סימני $, לדוגמה $\frac{'{1}{2}'}$)</label>
                <textarea rows={2} className="field resize-y" value={question.text} onChange={(e) => set({ text: e.target.value })} />
                {question.text.includes("$") && (
                    <div className="glass--sunk p-3 mt-2 text-ink"><MathText text={question.text} /></div>
                )}
            </div>

            {question.kind === "MULTI_SELECT" && (
                <div>
                    <span className="label">אפשרויות (סמנו את כל התשובות הנכונות)</span>
                    <ListEditor
                        items={question.options}
                        placeholder="אפשרות"
                        onChange={(options, removed) => set({
                            options,
                            correctOptions: removed === undefined ? question.correctOptions : removeShift(question.correctOptions, removed),
                        })}
                        renderExtra={(i) => (
                            <input type="checkbox" aria-label="תשובה נכונה" checked={question.correctOptions.includes(i)} onChange={() => toggleCorrect(i)} />
                        )}
                    />
                </div>
            )}

            {question.kind === "NUMERIC" && (
                <div className="grid sm:grid-cols-2 gap-3">
                    <div>
                        <label className="label">התשובה הנכונה</label>
                        <input className="field num" dir="ltr" value={question.numericAnswer} onChange={(e) => set({ numericAnswer: e.target.value })} />
                    </div>
                    <div>
                        <label className="label">סטייה מותרת (±)</label>
                        <input className="field num" dir="ltr" placeholder="0" value={question.tolerance} onChange={(e) => set({ tolerance: e.target.value })} />
                    </div>
                </div>
            )}

            {question.kind === "MATCHING" && (
                <div className="grid sm:grid-cols-2 gap-4">
                    <div>
                        <span className="label">פריטים</span>
                        <ListEditor
                            items={question.matchLeft}
                            placeholder="פריט"
                            onChange={(matchLeft, removed) => set({
                                matchLeft,
                                matchKey: removed === undefined ? question.matchKey : question.matchKey.filter((_, j) => j !== removed),
                            })}
                        />
                    </div>
                    <div>
                        <span className="label">התאמות</span>
                        <ListEditor
                            items={question.matchRight}
                            placeholder="התאמה"
                            onChange={(matchRight, removed) => set({
                                matchRight,
                                matchKey: removed === undefined ? question.matchKey : [],
                            })}
                        />
                    </div>
                    <div className="sm:col-span-2 grid gap-2">
                        <span className="label">המפתח: לכל פריט, ההתאמה הנכונה</span>
                        {question.matchLeft.map((left, i) => (
                            <div key={i} className="grid grid-cols-2 gap-2 items-center">
                                <span className="text-ink text-sm">{left || `פריט ${i + 1}`}</span>
                                <select className="field" value={question.matchKey[i] ?? ""} onChange={(e) => {
                                    const key = [...question.matchKey];
                                    while (key.length < question.matchLeft.length) key.push(null);
                                    key[i] = e.target.value === "" ? null : Number(e.target.value);
                                    set({ matchKey: key });
                                }}>
                                    <option value="">-- בחר --</option>
                                    {question.matchRight.map((r, j) => <option key={j} value={j}>{r || `התאמה ${j + 1}`}</option>)}
                                </select>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {question.kind === "ORDERING" && (
                <div>
                    <span className="label">הפריטים בסדר הנכון (התלמידים יראו אותם מעורבבים)</span>
                    <ListEditor items={question.options} placeholder="פריט" onChange={(options) => set({ options })} />
                </div>
            )}

            {question.kind === "SHORT_OPEN" && (
                <div>
                    <span className="label">תשובות מקובלות (טקסט או LaTeX; רווחים ואותיות גדולות לא משנים)</span>
                    <ListEditor items={question.acceptedAnswers} placeholder="תשובה" min={1} onChange={(acceptedAnswers) => set({ acceptedAnswers })} />
                </div>
            )}
        </div>
    );
}
