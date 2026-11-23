import { useState } from "react";
import { FiArrowUp, FiArrowDown } from "react-icons/fi";
import MathInput from "./MathInput.jsx";
import MathText from "./MathText.jsx";

// Answer control for one exam question. `value` and `onChange` carry the answer string the
// server grades: a JSON array of option ids for MULTI_SELECT / MATCHING / ORDERING,
// LaTeX or text for NUMERIC / SHORT_OPEN / TEXT.
const parse = (value, fallback) => {
    try {
        const parsed = JSON.parse(value);
        return Array.isArray(parsed) ? parsed : fallback;
    } catch {
        return fallback;
    }
};

function MultiSelect({ question, value, onChange }) {
    const selected = parse(value, []);
    const toggle = (id) => {
        const next = selected.includes(id) ? selected.filter((x) => x !== id) : [...selected, id];
        onChange(next.length ? JSON.stringify(next) : "");
    };
    return (
        <div className="grid gap-2">
            <span className="faint text-xs">ניתן לבחור יותר מתשובה אחת</span>
            {question.options.map((o) => (
                <button
                    type="button"
                    key={o.id}
                    onClick={() => toggle(o.id)}
                    className={`choice text-right ${selected.includes(o.id) ? "is-active" : ""}`}
                >
                    <MathText text={o.text} />
                </button>
            ))}
        </div>
    );
}

function Matching({ question, value, onChange }) {
    const size = question.matchLeft.length;
    const current = parse(value, Array(size).fill(null));
    const choose = (leftId, rightId) => {
        const next = [...current];
        while (next.length < size) next.push(null);
        next[leftId] = rightId === "" ? null : Number(rightId);
        onChange(next.every((x) => x === null) ? "" : JSON.stringify(next));
    };
    return (
        <div className="grid gap-3">
            {question.matchLeft.map((left) => (
                <div key={left.id} className="grid sm:grid-cols-2 gap-2 items-center">
                    <span className="text-ink"><MathText text={left.text} /></span>
                    <select
                        className="field"
                        value={current[left.id] ?? ""}
                        onChange={(e) => choose(left.id, e.target.value)}
                    >
                        <option value="">-- בחר התאמה --</option>
                        {question.matchRight.map((right) => (
                            <option key={right.id} value={right.id}>{right.text}</option>
                        ))}
                    </select>
                </div>
            ))}
        </div>
    );
}

function Ordering({ question, value, onChange }) {
    const byId = Object.fromEntries(question.options.map((o) => [o.id, o]));
    const order = parse(value, question.options.map((o) => o.id)).filter((id) => byId[id] !== undefined);
    const move = (index, delta) => {
        const target = index + delta;
        if (target < 0 || target >= order.length) return;
        const next = [...order];
        [next[index], next[target]] = [next[target], next[index]];
        onChange(JSON.stringify(next));
    };
    return (
        <div className="grid gap-2">
            <span className="faint text-xs">סדרו את הפריטים בעזרת החיצים{value ? "" : " (טרם סודר)"}</span>
            {order.map((id, i) => (
                <div key={id} className="row-card flex items-center gap-2 !py-2">
                    <span className="num text-xs text-ink-3 w-6">{i + 1}.</span>
                    <span className="flex-1 text-ink"><MathText text={byId[id].text} /></span>
                    <button type="button" className="btn btn-ghost btn-sm" aria-label="הזז למעלה" onClick={() => move(i, -1)} disabled={i === 0}>
                        <FiArrowUp />
                    </button>
                    <button type="button" className="btn btn-ghost btn-sm" aria-label="הזז למטה" onClick={() => move(i, 1)} disabled={i === order.length - 1}>
                        <FiArrowDown />
                    </button>
                </div>
            ))}
        </div>
    );
}

function ShortOpen({ value, onChange }) {
    const [formula, setFormula] = useState(false);
    return (
        <div className="grid gap-2">
            <div className="flex gap-2">
                <button type="button" className={`choice !py-1 !px-3 text-xs ${!formula ? "is-active" : ""}`} onClick={() => setFormula(false)}>טקסט</button>
                <button type="button" className={`choice !py-1 !px-3 text-xs ${formula ? "is-active" : ""}`} onClick={() => setFormula(true)}>נוסחה</button>
            </div>
            {formula ? (
                <MathInput value={value} onChange={onChange} />
            ) : (
                <input type="text" className="field" value={value || ""} onChange={(e) => onChange(e.target.value)} />
            )}
        </div>
    );
}

export default function QuestionAnswer({ question, value, onChange }) {
    switch (question.kind) {
        case "MULTI_SELECT":
            return <MultiSelect question={question} value={value} onChange={onChange} />;
        case "MATCHING":
            return <Matching question={question} value={value} onChange={onChange} />;
        case "ORDERING":
            return <Ordering question={question} value={value} onChange={onChange} />;
        case "NUMERIC":
            return (
                <div className="grid gap-1">
                    <span className="faint text-xs">אפשר להקליד מספר, שבר או נוסחה</span>
                    <MathInput value={value} onChange={onChange} />
                </div>
            );
        case "SHORT_OPEN":
            return <ShortOpen value={value} onChange={onChange} />;
        default:
            return (
                <input
                    type="text"
                    dir="ltr"
                    className="field num"
                    value={value || ""}
                    onChange={(e) => onChange(e.target.value)}
                />
            );
    }
}
