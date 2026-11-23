import { useEffect, useState } from "react";
import { FaPenFancy } from "react-icons/fa";
import FormShell from "../../components/ui/FormShell.jsx";
import { aiGet, aiPost } from "../../components/ai/aiApi.js";
import { AI_OPEN_QUESTION_ANSWERS, AI_OPEN_QUESTIONS } from "../../constants/pages.constants.js";

// Student: open questions the school's teachers published. The teacher checks the answers (with AI help).
export default function OpenQuestions() {
    const [questions, setQuestions] = useState([]);
    const [answers, setAnswers] = useState({});
    const [drafts, setDrafts] = useState({});
    const [message, setMessage] = useState({ text: "", ok: true });

    useEffect(() => {
        let cancelled = false;
        Promise.all([aiGet(AI_OPEN_QUESTIONS), aiGet(AI_OPEN_QUESTION_ANSWERS)]).then(([qRes, aRes]) => {
            if (cancelled) return;
            if (qRes.success) setQuestions(qRes.data);
            else setMessage({ text: qRes.errorCode, ok: false });
            if (aRes.success) setAnswers(Object.fromEntries(aRes.data.map((a) => [a.rubricId, a])));
        });
        return () => {
            cancelled = true;
        };
    }, []);

    const submit = async (question) => {
        const res = await aiPost(AI_OPEN_QUESTION_ANSWERS, { rubricId: question.id, answerText: drafts[question.id] || "" });
        setMessage({ text: res.errorCode || "", ok: res.success });
        if (res.success) {
            setAnswers((prev) => ({ ...prev, [question.id]: res.data }));
            setDrafts((prev) => ({ ...prev, [question.id]: "" }));
        }
    };

    return (
        <FormShell eyebrow="Open questions · שאלות פתוחות" title="שאלות פתוחות מהמורים" icon={<FaPenFancy />} width="medium">
            {message.text && <p className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</p>}
            {questions.length === 0 && !message.text && <p className="faint text-sm">אין כרגע שאלות פתוחות.</p>}
            {questions.map((q) => {
                const answer = answers[q.id];
                const approved = answer?.status === "APPROVED";
                return (
                    <article key={q.id} className="row-card !block space-y-2">
                        <div className="flex flex-wrap items-center gap-2">
                            <strong>{q.title}</strong>
                            <span className="chip num">{q.maxScore} נק'</span>
                            {answer && (
                                <span className={`chip ${approved ? "chip--mint" : "chip--amber"}`}>
                                    {approved ? `ציון: ${answer.finalScore}/${q.maxScore}` : "נשלח – ממתין לבדיקה"}
                                </span>
                            )}
                        </div>
                        <p className="whitespace-pre-wrap text-sm">{q.questionText}</p>
                        {answer && <p className="glass--sunk rounded-xl p-3 text-sm whitespace-pre-wrap">{answer.answerText}</p>}
                        {approved && answer.teacherComment && <p className="notice text-sm">הערת המורה: {answer.teacherComment}</p>}
                        {!approved && (
                            <>
                                <textarea
                                    className="field"
                                    maxLength={4000}
                                    placeholder={answer ? "אפשר לשלוח תשובה מעודכנת עד שהמורה בודק" : "כתבו את התשובה שלכם"}
                                    value={drafts[q.id] || ""}
                                    onChange={(e) => setDrafts({ ...drafts, [q.id]: e.target.value })}
                                />
                                <button className="btn btn-primary btn-sm" disabled={!(drafts[q.id] || "").trim()} onClick={() => submit(q)}>
                                    {answer ? "שלח שוב" : "שלח תשובה"}
                                </button>
                            </>
                        )}
                    </article>
                );
            })}
        </FormShell>
    );
}
