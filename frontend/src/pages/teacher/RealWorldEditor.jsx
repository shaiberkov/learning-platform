import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaGlobeAmericas, FaUndo } from "react-icons/fa";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import EmptyState from "../../components/ui/EmptyState.jsx";
import { errorMessage } from "../../lib/api.js";
import { fetchRealWorldTopics, resetRealWorldOverride, saveRealWorldOverride } from "../../features/realworld/realWorldApi.js";

const TOPICS_KEY = ["real-world-topics"];
const MAX = 1000;

// A teacher rewrites the "why does it matter to me?" card of a topic for their school,
// or goes back to the built-in text.
export default function RealWorldEditor() {
    const queryClient = useQueryClient();
    const [selected, setSelected] = useState(null);
    const [form, setForm] = useState(null);
    const [message, setMessage] = useState(null);

    const topics = useQuery({ queryKey: TOPICS_KEY, queryFn: fetchRealWorldTopics });

    const done = (ok, text) => {
        setMessage({ ok, text });
        if (ok) {
            queryClient.invalidateQueries({ queryKey: TOPICS_KEY });
            queryClient.invalidateQueries({ queryKey: ["real-world-topic"] });
        }
    };
    const save = useMutation({
        mutationFn: saveRealWorldOverride,
        onSuccess: (res) => done(true, res.errorCode || "נשמר"),
        onError: (err) => done(false, errorMessage(err, "השמירה נכשלה")),
    });
    const reset = useMutation({
        mutationFn: resetRealWorldOverride,
        onSuccess: (res) => {
            setForm({ everydayExample: res.data.everydayExample, careerLink: res.data.careerLink });
            done(true, res.errorCode);
        },
        onError: (err) => done(false, errorMessage(err, "האיפוס נכשל")),
    });

    const choose = (t) => {
        setSelected(t.topic);
        setForm({ everydayExample: t.everydayExample, careerLink: t.careerLink });
        setMessage(null);
    };

    const current = topics.data?.find((t) => t.topic === selected);

    return (
        <div className="page page--wide" dir="rtl">
            <div className="grid lg:grid-cols-[18rem_1fr] gap-4 sm:gap-5 items-start">
                <aside className="glass p-5 space-y-4 reveal">
                    <div className="flex items-center gap-3">
                        <span className="icon-badge icon-badge--mint" aria-hidden="true"><FaGlobeAmericas /></span>
                        <div>
                            <span className="eyebrow">Real world · חיבור לחיים</span>
                            <h1 className="title text-xl mt-0.5">למה זה חשוב לי?</h1>
                        </div>
                    </div>
                    <p className="faint text-xs leading-relaxed">
                        לכל נושא יש דוגמה מהחיים וחיבור למקצוע, שמוצגים לתלמידים במסכי התרגול והשיעורים.
                        אפשר לכתוב נוסח משלכם; הוא יוצג לכל התלמידים בבית הספר שלכם.
                    </p>
                    {topics.isLoading && <SkeletonList rows={3} />}
                    {topics.isError && <div className="notice notice--danger">{errorMessage(topics.error, "לא ניתן לטעון את הנושאים")}</div>}
                    <ul className="space-y-1.5" aria-label="נושאים">
                        {topics.data?.map((t) => (
                            <li key={t.topic}>
                                <button type="button" onClick={() => choose(t)}
                                        className={`choice w-full text-right flex justify-between gap-2 ${selected === t.topic ? "is-active" : ""}`}
                                        aria-pressed={selected === t.topic}>
                                    <span>{t.topic}</span>
                                    {t.customized && <span className="chip chip--mint">מותאם</span>}
                                </button>
                            </li>
                        ))}
                    </ul>
                </aside>

                <section className="glass p-6 sm:p-8 reveal">
                    {!form ? (
                        <EmptyState title="בחרו נושא" body="בחרו נושא מהרשימה כדי לראות ולערוך את ההסבר שלו." />
                    ) : (
                        <form className="space-y-5" onSubmit={(e) => { e.preventDefault(); save.mutate({ topic: selected, ...form }); }}>
                            <div>
                                <span className="eyebrow">{selected}</span>
                                <h2 className="title text-2xl mt-0.5">עריכת ההסבר</h2>
                                {current?.customized && current.updatedAt && (
                                    <p className="faint text-xs mt-1">עודכן לאחרונה: <span className="num">{current.updatedAt}</span></p>
                                )}
                            </div>
                            <div className="field-wrap">
                                <label htmlFor="rw-example" className="label">דוגמה מהחיים</label>
                                <textarea id="rw-example" className="field min-h-[7rem]" maxLength={MAX} required
                                          value={form.everydayExample}
                                          onChange={(e) => setForm((f) => ({ ...f, everydayExample: e.target.value }))} />
                                <span className="faint text-xs num">{form.everydayExample.length}/{MAX}</span>
                            </div>
                            <div className="field-wrap">
                                <label htmlFor="rw-career" className="label">חיבור למקצוע</label>
                                <textarea id="rw-career" className="field min-h-[7rem]" maxLength={MAX} required
                                          value={form.careerLink}
                                          onChange={(e) => setForm((f) => ({ ...f, careerLink: e.target.value }))} />
                                <span className="faint text-xs num">{form.careerLink.length}/{MAX}</span>
                            </div>
                            {message && (
                                <div role="status" className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</div>
                            )}
                            <div className="flex flex-wrap gap-2">
                                <button type="submit" className="btn btn-primary" disabled={save.isPending}>
                                    {save.isPending ? "שומר..." : "שמירה"}
                                </button>
                                {current?.customized && (
                                    <button type="button" className="btn btn-ghost" disabled={reset.isPending}
                                            onClick={() => reset.mutate(selected)}>
                                        <FaUndo aria-hidden="true" /> חזרה לנוסח המקורי
                                    </button>
                                )}
                            </div>
                        </form>
                    )}
                </section>
            </div>
        </div>
    );
}
