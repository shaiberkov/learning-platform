import { useCallback, useEffect, useState } from "react";
import { FaCheckDouble } from "react-icons/fa";
import FormShell from "../../components/ui/FormShell.jsx";
import AiPolicyNotice from "../../components/ai/AiPolicyNotice.jsx";
import { staffGet, staffPost } from "../../components/ai/aiApi.js";
import { AI_GRADING_ANSWERS, AI_RUBRICS } from "../../constants/pages.constants.js";

const STATUS_LABEL = { SUBMITTED: "ממתין", SUGGESTED: "יש הצעת AI", APPROVED: "אושר" };
const EMPTY_RUBRIC = { title: "", questionText: "", rubric: "", maxScore: 10, published: true };

// Teacher: open questions with a rubric; the AI suggests a score and reasoning, the teacher approves in one click.
export default function AiGrading() {
    const [rubrics, setRubrics] = useState([]);
    const [selected, setSelected] = useState(null);
    const [answers, setAnswers] = useState([]);
    const [form, setForm] = useState(EMPTY_RUBRIC);
    const [showForm, setShowForm] = useState(false);
    const [paper, setPaper] = useState({ studentName: "", answerText: "" });
    const [overrides, setOverrides] = useState({});
    const [busyId, setBusyId] = useState(null);
    const [message, setMessage] = useState({ text: "", ok: true });

    const say = (res) => setMessage({ text: res.errorCode || "", ok: res.success });

    const loadRubrics = useCallback(async () => {
        const res = await staffGet(AI_RUBRICS);
        if (res.success) setRubrics(res.data);
        else say(res);
    }, []);

    const loadAnswers = useCallback(async (rubric) => {
        const res = await staffGet(`${AI_RUBRICS}/${rubric.id}/answers`);
        if (res.success) setAnswers(res.data);
        else say(res);
    }, []);

    useEffect(() => {
        loadRubrics();
    }, [loadRubrics]);

    useEffect(() => {
        if (selected) loadAnswers(selected);
    }, [selected, loadAnswers]);

    const replaceAnswer = (updated) => setAnswers((prev) => prev.map((a) => (a.id === updated.id ? updated : a)));

    const createRubric = async () => {
        const res = await staffPost(AI_RUBRICS, { ...form, maxScore: Number(form.maxScore) });
        say(res);
        if (res.success) {
            setForm(EMPTY_RUBRIC);
            setShowForm(false);
            setRubrics((prev) => [res.data, ...prev]);
            setSelected(res.data);
        }
    };

    const addPaperAnswer = async () => {
        const res = await staffPost(`${AI_RUBRICS}/${selected.id}/answers`, paper);
        say(res);
        if (res.success) {
            setPaper({ studentName: "", answerText: "" });
            setAnswers((prev) => [...prev, res.data]);
        }
    };

    const suggest = async (answer) => {
        setBusyId(answer.id);
        const res = await staffPost(`${AI_GRADING_ANSWERS}/${answer.id}/suggest`);
        setBusyId(null);
        if (res.success) replaceAnswer(res.data);
        else say(res);
    };

    const suggestAll = async () => {
        for (const answer of answers.filter((a) => a.status === "SUBMITTED")) {
            await suggest(answer);
        }
    };

    const approve = async (answer) => {
        const override = overrides[answer.id];
        const body = override === undefined || override === "" ? {} : { finalScore: Number(override) };
        const res = await staffPost(`${AI_GRADING_ANSWERS}/${answer.id}/approve`, body);
        if (res.success) replaceAnswer(res.data);
        say(res);
    };

    return (
        <FormShell eyebrow="AI · בדיקה לפי מחוון" title="בדיקת שאלות פתוחות" icon={<FaCheckDouble />} width="wide">
            <AiPolicyNotice audience="staff" />

            <div className="flex flex-wrap items-center gap-2">
                <select
                    className="field max-w-md"
                    value={selected?.id ?? ""}
                    onChange={(e) => setSelected(rubrics.find((r) => String(r.id) === e.target.value) || null)}
                >
                    <option value="">בחרו שאלה פתוחה...</option>
                    {rubrics.map((r) => (
                        <option key={r.id} value={r.id}>{r.title}{r.published ? "" : " (לא פורסמה)"}</option>
                    ))}
                </select>
                <button className="btn btn-ghost btn-sm" onClick={() => setShowForm((v) => !v)}>
                    {showForm ? "ביטול" : "שאלה חדשה"}
                </button>
            </div>

            {message.text && <p className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</p>}

            {showForm && (
                <section className="glass--sunk rounded-2xl p-4 space-y-3">
                    <label className="label">כותרת</label>
                    <input className="field" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
                    <label className="label">השאלה (התלמידים יראו אותה)</label>
                    <textarea className="field" value={form.questionText}
                              onChange={(e) => setForm({ ...form, questionText: e.target.value })} />
                    <label className="label">מחוון (רק המורה וה-AI רואים)</label>
                    <textarea className="field min-h-[7rem]" value={form.rubric}
                              placeholder={"למשל:\n4 נק' – בניית משוואה נכונה\n4 נק' – פתרון נכון\n2 נק' – בדיקה והסבר"}
                              onChange={(e) => setForm({ ...form, rubric: e.target.value })} />
                    <div className="flex flex-wrap items-center gap-4">
                        <label className="label !mb-0" htmlFor="rubric-max">ציון מקסימלי</label>
                        <input id="rubric-max" type="number" min={1} max={100} className="field num max-w-[7rem]"
                               value={form.maxScore} onChange={(e) => setForm({ ...form, maxScore: e.target.value })} />
                        <label className="inline-flex items-center gap-2 text-sm">
                            <input type="checkbox" checked={form.published}
                                   onChange={(e) => setForm({ ...form, published: e.target.checked })} />
                            פרסם לתלמידי בית הספר
                        </label>
                    </div>
                    <button className="btn btn-primary" onClick={createRubric}>שמור שאלה</button>
                </section>
            )}

            {selected && (
                <section className="space-y-4">
                    <div className="glass--sunk rounded-2xl p-4 space-y-2 text-sm">
                        <p><strong>שאלה:</strong> {selected.questionText}</p>
                        <p className="whitespace-pre-wrap"><strong>מחוון:</strong> {selected.rubric}</p>
                        <p className="num">ציון מקסימלי: {selected.maxScore}</p>
                    </div>

                    <div className="flex items-center justify-between">
                        <h3 className="title text-xl">תשובות <span className="num faint">({answers.length})</span></h3>
                        <button className="btn btn-ghost btn-sm" onClick={suggestAll}
                                disabled={busyId !== null || !answers.some((a) => a.status === "SUBMITTED")}>
                            הצע ציון לכל הממתינות
                        </button>
                    </div>

                    {answers.length === 0 && <p className="faint text-sm">עדיין אין תשובות לשאלה הזו.</p>}
                    {answers.map((a) => (
                        <article key={a.id} className="row-card !block space-y-2">
                            <div className="flex flex-wrap items-center gap-2">
                                <strong>{a.studentName || "תלמיד/ה"}</strong>
                                <span className={`chip ${a.status === "APPROVED" ? "chip--mint" : a.status === "SUGGESTED" ? "chip--amber" : ""}`}>
                                    {STATUS_LABEL[a.status]}
                                </span>
                                {a.finalScore !== null && a.finalScore !== undefined && (
                                    <span className="num">ציון סופי: {a.finalScore}/{a.maxScore}</span>
                                )}
                            </div>
                            <p className="whitespace-pre-wrap text-sm">{a.answerText}</p>

                            {a.aiScore !== null && a.aiScore !== undefined && (
                                <div className="notice text-sm space-y-1">
                                    <p className="num font-semibold">הצעת AI: {a.aiScore}/{a.maxScore}</p>
                                    <p className="whitespace-pre-wrap">{a.aiReasoning}</p>
                                </div>
                            )}

                            {a.status !== "APPROVED" && (
                                <div className="flex flex-wrap items-center gap-2">
                                    {a.status === "SUBMITTED" && (
                                        <button className="btn btn-ghost btn-sm" disabled={busyId === a.id} onClick={() => suggest(a)}>
                                            {busyId === a.id ? "בודק..." : "הצע ציון AI"}
                                        </button>
                                    )}
                                    <input type="number" min={0} max={a.maxScore} className="field num !min-h-[2.5rem] max-w-[6rem]"
                                           placeholder={a.aiScore ?? "ציון"}
                                           value={overrides[a.id] ?? ""}
                                           onChange={(e) => setOverrides({ ...overrides, [a.id]: e.target.value })} />
                                    <button className="btn btn-mint btn-sm" onClick={() => approve(a)}
                                            disabled={(a.aiScore === null || a.aiScore === undefined) && !overrides[a.id]}>
                                        {overrides[a.id] ? `אשר ${overrides[a.id]}` : a.aiScore !== null && a.aiScore !== undefined ? `אשר ${a.aiScore}` : "אשר"}
                                    </button>
                                </div>
                            )}
                        </article>
                    ))}

                    <div className="glass--sunk rounded-2xl p-4 space-y-2">
                        <span className="eyebrow">הוספת תשובה מדף</span>
                        <input className="field" placeholder="שם התלמיד/ה" value={paper.studentName}
                               onChange={(e) => setPaper({ ...paper, studentName: e.target.value })} />
                        <textarea className="field" placeholder="תוכן התשובה" value={paper.answerText}
                                  onChange={(e) => setPaper({ ...paper, answerText: e.target.value })} />
                        <button className="btn btn-ghost btn-sm" disabled={!paper.answerText.trim()} onClick={addPaperAnswer}>
                            הוסף תשובה
                        </button>
                    </div>
                </section>
            )}
        </FormShell>
    );
}
