import { useContext, useEffect, useState } from "react";
import { FaLightbulb, FaPlus, FaTrash } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import OrbitLoader from "../../components/ui/OrbitLoader.jsx";
import { fetchHintCatalog, fetchHintTemplate, MATH_SUBJECT, saveHintTemplate } from "../../features/adaptive/adaptiveApi.js";

function HintEditor() {
    const { user } = useContext(UserContext);
    const [catalog, setCatalog] = useState([]);
    const [subTopic, setSubTopic] = useState(null);
    const [form, setForm] = useState(null);
    const [message, setMessage] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    const loadCatalog = async () => {
        const res = await fetchHintCatalog();
        if (res.success) setCatalog(res.data);
        else setMessage({ ok: false, text: res.errorCode });
        setLoading(false);
    };

    useEffect(() => {
        loadCatalog();
    }, []);

    const choose = async (name) => {
        setSubTopic(name);
        setMessage(null);
        setForm(null);
        const res = await fetchHintTemplate(MATH_SUBJECT, name);
        if (res.success) {
            setForm({ ...res.data, solutionSteps: res.data.solutionSteps.length ? res.data.solutionSteps : [""] });
        } else {
            setMessage({ ok: false, text: res.errorCode });
        }
    };

    const setStep = (i, value) =>
        setForm((f) => ({ ...f, solutionSteps: f.solutionSteps.map((s, j) => (j === i ? value : s)) }));

    const save = async (e) => {
        e.preventDefault();
        setSaving(true);
        const res = await saveHintTemplate(user.userId, {
            subject: MATH_SUBJECT,
            subTopic,
            hint1: form.hint1,
            hint2: form.hint2,
            solutionSteps: form.solutionSteps,
        });
        setSaving(false);
        setMessage({ ok: res.success, text: res.errorCode || (res.success ? "נשמר" : "השמירה נכשלה") });
        if (res.success) {
            setForm((f) => ({ ...f, custom: true }));
            loadCatalog();
        }
    };

    if (loading) {
        return <OrbitLoader title="טוען נושאים..." subtitle="מכינים את עורך הרמזים" />;
    }

    return (
        <div className="page page--wide" dir="rtl">
            <div className="grid lg:grid-cols-[20rem_1fr] gap-4 sm:gap-5 items-start">
                <aside className="glass p-5 space-y-4 reveal">
                    <div className="flex items-center gap-3">
                        <span className="icon-badge icon-badge--amber"><FaLightbulb /></span>
                        <div>
                            <span className="eyebrow">Hints · רמזים</span>
                            <h1 className="title text-xl mt-0.5">רמזים ופתרון מודרך</h1>
                        </div>
                    </div>
                    <p className="faint text-xs leading-relaxed">
                        לכל סוג תרגיל: רמז 1, רמז 2 ופתרון צעד-אחר-צעד. התלמיד פותח אותם לפי הסדר, וכל שלב מוריד ניקוד
                        (10 → 7 → 4 → 0). בסוף הפתרון המלא מוצגת התשובה של השאלה עצמה.
                    </p>
                    {catalog.map((topic) => (
                        <div key={topic.topic} className="space-y-1.5">
                            <span className="label !text-xs">{topic.topic}</span>
                            {topic.subTopics.map((s) => (
                                <button
                                    key={s.subTopic}
                                    onClick={() => choose(s.subTopic)}
                                    className={`choice !min-h-0 !py-2 w-full !justify-between text-sm ${subTopic === s.subTopic ? "is-active" : ""}`}
                                >
                                    <span>{s.subTopic}</span>
                                    {s.custom && <span className="chip !text-[0.65rem]">נערך</span>}
                                </button>
                            ))}
                        </div>
                    ))}
                </aside>

                <section className="glass p-6 sm:p-8 reveal" style={{ "--i": 1 }}>
                    {!subTopic && <p className="muted">בחר סוג תרגיל מהרשימה כדי לערוך את הרמזים שלו.</p>}
                    {subTopic && !form && !message && <p className="muted">טוען...</p>}
                    {message && <p className={`notice mb-4 ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</p>}
                    {form && (
                        <form onSubmit={save} className="space-y-5">
                            <div className="flex flex-wrap items-center gap-2">
                                <h2 className="title text-2xl">{form.subTopic}</h2>
                                <span className="chip">{form.topic}</span>
                                <span className="chip">{form.custom ? `נערך ${form.updatedAt || ""}` : "ברירת מחדל"}</span>
                            </div>

                            <label className="block space-y-1.5">
                                <span className="label">רמז 1 (כיוון כללי)</span>
                                <textarea
                                    className="field min-h-[5rem]"
                                    maxLength={1000}
                                    value={form.hint1}
                                    onChange={(e) => setForm({ ...form, hint1: e.target.value })}
                                />
                            </label>
                            <label className="block space-y-1.5">
                                <span className="label">רמז 2 (ממוקד יותר)</span>
                                <textarea
                                    className="field min-h-[5rem]"
                                    maxLength={1000}
                                    value={form.hint2}
                                    onChange={(e) => setForm({ ...form, hint2: e.target.value })}
                                />
                            </label>

                            <div className="space-y-2">
                                <span className="label">פתרון מלא, צעד אחר צעד</span>
                                {form.solutionSteps.map((step, i) => (
                                    <div key={i} className="flex items-center gap-2">
                                        <span className="num text-ink-3 w-6 text-center">{i + 1}</span>
                                        <input className="field" value={step} onChange={(e) => setStep(i, e.target.value)} />
                                        <button
                                            type="button"
                                            className="btn btn-ghost btn-sm"
                                            title="מחק שלב"
                                            disabled={form.solutionSteps.length === 1}
                                            onClick={() => setForm({ ...form, solutionSteps: form.solutionSteps.filter((_, j) => j !== i) })}
                                        >
                                            <FaTrash />
                                        </button>
                                    </div>
                                ))}
                                <button
                                    type="button"
                                    className="btn btn-ghost btn-sm"
                                    onClick={() => setForm({ ...form, solutionSteps: [...form.solutionSteps, ""] })}
                                >
                                    <FaPlus /> הוסף שלב
                                </button>
                                <p className="faint text-xs">השלב האחרון שהתלמיד יראה הוא התשובה של השאלה, שנוספת אוטומטית.</p>
                            </div>

                            <button type="submit" disabled={saving} className="btn btn-primary">
                                שמור רמזים
                            </button>
                        </form>
                    )}
                </section>
            </div>
        </div>
    );
}

export default HintEditor;
