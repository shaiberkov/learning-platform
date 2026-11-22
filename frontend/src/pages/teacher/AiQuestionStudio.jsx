import { useEffect, useState } from "react";
import { FaMagic } from "react-icons/fa";
import FormShell from "../../components/ui/FormShell.jsx";
import AiPolicyNotice from "../../components/ai/AiPolicyNotice.jsx";
import { staffGet, staffPost, staffPut } from "../../components/ai/aiApi.js";
import {
    AI_QUESTION_BANK,
    AI_QUESTION_DRAFTS,
    AI_QUESTIONS_GENERATE,
} from "../../constants/pages.constants.js";

const DIFFICULTIES = ["קל", "בינוני", "קשה"];

// Teacher: describe questions in words, the AI drafts them, the teacher edits and approves into the bank.
export default function AiQuestionStudio() {
    const [prompt, setPrompt] = useState("");
    const [count, setCount] = useState(10);
    const [difficulty, setDifficulty] = useState("בינוני");
    const [gradeLevel, setGradeLevel] = useState("");
    const [drafts, setDrafts] = useState([]);
    const [bank, setBank] = useState([]);
    const [showBank, setShowBank] = useState(false);
    const [busy, setBusy] = useState(false);
    const [message, setMessage] = useState({ text: "", ok: true });

    const loadDrafts = async () => {
        const res = await staffGet(AI_QUESTION_DRAFTS);
        if (res.success) setDrafts(res.data);
        else setMessage({ text: res.errorCode, ok: false });
    };
    const loadBank = async () => {
        const res = await staffGet(AI_QUESTION_BANK);
        if (res.success) setBank(res.data);
    };

    useEffect(() => {
        loadDrafts();
        loadBank();
    }, []);

    const generate = async (fresh) => {
        setBusy(true);
        setMessage({ text: "", ok: true });
        const res = await staffPost(
            AI_QUESTIONS_GENERATE,
            { prompt, count: Number(count), difficulty, gradeLevel, subject: "מתמטיקה" },
            { fresh }
        );
        setBusy(false);
        setMessage({ text: res.errorCode || "", ok: res.success });
        if (res.success) loadDrafts();
    };

    const editDraft = (id, field, value) =>
        setDrafts((prev) => prev.map((d) => (d.id === id ? { ...d, [field]: value, dirty: true } : d)));

    const saveDraft = async (draft) => {
        const res = await staffPut(`${AI_QUESTION_DRAFTS}/${draft.id}`, draft);
        if (!res.success) {
            setMessage({ text: res.errorCode, ok: false });
            return false;
        }
        setDrafts((prev) => prev.map((d) => (d.id === draft.id ? res.data : d)));
        return true;
    };

    const approve = async (draft) => {
        if (draft.dirty && !(await saveDraft(draft))) return;
        const res = await staffPost(`${AI_QUESTION_DRAFTS}/${draft.id}/approve`);
        setMessage({ text: res.errorCode, ok: res.success });
        if (res.success) {
            setDrafts((prev) => prev.filter((d) => d.id !== draft.id));
            setBank((prev) => [res.data, ...prev]);
        }
    };

    const reject = async (draft) => {
        const res = await staffPost(`${AI_QUESTION_DRAFTS}/${draft.id}/reject`);
        if (res.success) setDrafts((prev) => prev.filter((d) => d.id !== draft.id));
        else setMessage({ text: res.errorCode, ok: false });
    };

    return (
        <FormShell eyebrow="AI · מחולל שאלות" title="יצירת שאלות עם AI" icon={<FaMagic />} width="wide">
            <AiPolicyNotice audience="staff" />

            <div className="space-y-3">
                <label className="label" htmlFor="ai-prompt">מה לייצר?</label>
                <textarea
                    id="ai-prompt"
                    className="field min-h-[6rem]"
                    maxLength={500}
                    value={prompt}
                    onChange={(e) => setPrompt(e.target.value)}
                    placeholder="למשל: 15 שאלות על משוואות ריבועיות, בינוני, כיתה ט, עם מקדם a שונה מ-1"
                />
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div>
                        <label className="label" htmlFor="ai-count">מספר שאלות</label>
                        <input id="ai-count" type="number" min={1} max={20} className="field num"
                               value={count} onChange={(e) => setCount(e.target.value)} />
                    </div>
                    <div>
                        <label className="label" htmlFor="ai-difficulty">רמת קושי</label>
                        <select id="ai-difficulty" className="field" value={difficulty} onChange={(e) => setDifficulty(e.target.value)}>
                            {DIFFICULTIES.map((d) => <option key={d} value={d}>{d}</option>)}
                        </select>
                    </div>
                    <div>
                        <label className="label" htmlFor="ai-grade">כיתה</label>
                        <input id="ai-grade" className="field" value={gradeLevel} placeholder="למשל: ט"
                               onChange={(e) => setGradeLevel(e.target.value)} />
                    </div>
                </div>
                <div className="flex flex-wrap gap-2">
                    <button className="btn btn-primary" disabled={busy || !prompt.trim()} onClick={() => generate(false)}>
                        {busy ? "מייצר..." : "צור טיוטות"}
                    </button>
                    <button className="btn btn-ghost" disabled={busy || !prompt.trim()} onClick={() => generate(true)}>
                        צור סט חדש
                    </button>
                </div>
                {message.text && <p className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</p>}
            </div>

            <section className="space-y-3">
                <div className="flex items-center justify-between">
                    <h3 className="title text-xl">טיוטות לאישור <span className="num faint">({drafts.length})</span></h3>
                    <button className="btn btn-ghost btn-sm" onClick={() => setShowBank((v) => !v)}>
                        {showBank ? "הסתר מאגר" : `מאגר השאלות שלי (${bank.length})`}
                    </button>
                </div>
                {drafts.length === 0 && <p className="faint text-sm">אין טיוטות שממתינות לאישור.</p>}
                {drafts.map((d) => (
                    <article key={d.id} className="row-card !block space-y-2">
                        <div className="flex flex-wrap gap-2">
                            {d.subTopic && <span className="chip">{d.subTopic}</span>}
                            {d.difficulty && <span className="chip chip--amber">{d.difficulty}</span>}
                            {d.questionText?.length > 255 && <span className="chip chip--danger">ארוכה מ-255 תווים</span>}
                        </div>
                        <label className="label">שאלה</label>
                        <textarea className="field" value={d.questionText || ""}
                                  onChange={(e) => editDraft(d.id, "questionText", e.target.value)} />
                        <label className="label">תשובה (כפי שהתלמיד יקליד)</label>
                        <input className="field num" dir="ltr" value={d.answer || ""}
                               onChange={(e) => editDraft(d.id, "answer", e.target.value)} />
                        <label className="label">הסבר / פתרון</label>
                        <textarea className="field text-sm" value={d.explanation || ""}
                                  onChange={(e) => editDraft(d.id, "explanation", e.target.value)} />
                        <div className="flex flex-wrap gap-2 pt-1">
                            <button className="btn btn-mint btn-sm" onClick={() => approve(d)}>אשר למאגר</button>
                            {d.dirty && <button className="btn btn-ghost btn-sm" onClick={() => saveDraft(d)}>שמור שינויים</button>}
                            <button className="btn btn-danger btn-sm" onClick={() => reject(d)}>דחה</button>
                        </div>
                    </article>
                ))}
            </section>

            {showBank && (
                <section className="space-y-2">
                    <h3 className="title text-xl">מאגר השאלות שלי</h3>
                    {bank.length === 0 && <p className="faint text-sm">המאגר ריק.</p>}
                    <div className="overflow-x-auto">
                        <table className="data-table">
                            <thead>
                            <tr><th>שאלה</th><th>תשובה</th><th>נושא</th><th>קושי</th></tr>
                            </thead>
                            <tbody>
                            {bank.map((q) => (
                                <tr key={q.id}>
                                    <td>{q.questionText}</td>
                                    <td className="num" dir="ltr">{q.answer}</td>
                                    <td>{q.subTopic || q.topic || "-"}</td>
                                    <td>{q.difficulty || "-"}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                </section>
            )}
        </FormShell>
    );
}
