import { useId, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaLightbulb, FaPlus, FaTrash } from "react-icons/fa";
import EmptyState from "../../components/ui/EmptyState.jsx";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import { errorMessage } from "../../lib/api.js";
import { peerApi } from "../../features/classCommunity/classCommunityApi.js";
import {
    MAX_DISTRACTORS, PEER_STATUS, formatWhen, successRate, toSubmitPayload, validatePeerQuestion,
} from "../../features/classCommunity/classCommunityUtils.js";

// [s02] Questions students write for their classmates: the class practice (approved questions, credited to
// their authors), the authoring form (multiple choice with distractors), and the student's own questions.

const TABS = [
    { id: "practice", label: "תרגול הכיתה" },
    { id: "write", label: "כתיבת שאלה" },
    { id: "mine", label: "השאלות שלי" },
];

function PracticeCard({ question, index }) {
    const [chosen, setChosen] = useState(null);
    const answer = useMutation({ mutationFn: (option) => peerApi.answer(question.id, option) });
    const result = answer.data?.data;
    const pick = (option) => {
        if (result) return;
        setChosen(option);
        answer.mutate(option);
    };

    return (
        <div className="glass p-5 sm:p-6 space-y-3 reveal" style={{ "--i": Math.min(index, 8) }}>
            <div className="flex flex-wrap items-center gap-2">
                {question.topic && <span className="chip">{question.topic}</span>}
                <span className="faint text-xs">נכתבה על ידי {question.mine ? "אותך" : question.authorName}</span>
            </div>
            <p className="title text-lg whitespace-pre-line" dir="auto">{question.questionText}</p>
            <div className="grid gap-2 sm:grid-cols-2" role="group" aria-label="אפשרויות תשובה">
                {question.options.map((option) => {
                    const isRight = result && option === result.correctAnswer;
                    const isWrongPick = result && option === chosen && !result.correct;
                    return (
                        <button key={option} type="button" dir="auto"
                                className={`choice ${option === chosen || isRight ? "is-active" : ""}`}
                                aria-pressed={option === chosen}
                                disabled={Boolean(result) || answer.isPending}
                                onClick={() => pick(option)}>
                            {option}{isRight ? " ✓" : ""}{isWrongPick ? " ✗" : ""}
                        </button>
                    );
                })}
            </div>
            {result && (
                <div className={`notice ${result.correct ? "notice--mint" : "notice--danger"}`} role="status">
                    <p className="font-semibold">{result.correct ? "נכון! כל הכבוד" : `לא מדויק. התשובה הנכונה: ${result.correctAnswer}`}</p>
                    {result.explanation && <p className="whitespace-pre-line">{result.explanation}</p>}
                </div>
            )}
            {answer.isError && <div className="notice notice--danger">{errorMessage(answer.error, "לא ניתן לבדוק את התשובה")}</div>}
            {result && (
                <button type="button" className="btn btn-ghost btn-sm" onClick={() => { setChosen(null); answer.reset(); }}>
                    לנסות שוב
                </button>
            )}
        </div>
    );
}

function PracticeTab() {
    const practice = useQuery({ queryKey: ["peer-practice"], queryFn: peerApi.practice });
    if (practice.isLoading) return <SkeletonList rows={3} />;
    if (practice.isError) return <div className="notice notice--danger">{errorMessage(practice.error, "לא ניתן לטעון את התרגול")}</div>;
    if (!practice.data?.length) {
        return (
            <div className="glass">
                <EmptyState icon={<FaLightbulb />} title="עוד אין שאלות של חברי הכיתה"
                            body="כתבו שאלה ראשונה – אחרי שהמורה יאשר אותה היא תופיע כאן עם השם שלכם." />
            </div>
        );
    }
    return practice.data.map((q, i) => <PracticeCard key={q.id} question={q} index={i} />);
}

const EMPTY_FORM = { questionText: "", correctAnswer: "", distractors: ["", "", ""], topic: "", explanation: "" };

function WriteTab() {
    const [form, setForm] = useState(EMPTY_FORM);
    const [localError, setLocalError] = useState(null);
    const ids = useId();
    const queryClient = useQueryClient();
    const submit = useMutation({
        mutationFn: () => peerApi.submit(toSubmitPayload(form)),
        onSuccess: () => {
            setForm(EMPTY_FORM);
            queryClient.invalidateQueries({ queryKey: ["peer-mine"] });
        },
    });
    const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));
    const setDistractor = (i) => (e) => setForm((f) => ({
        ...f, distractors: f.distractors.map((d, j) => (j === i ? e.target.value : d)),
    }));
    const onSubmit = (e) => {
        e.preventDefault();
        const error = validatePeerQuestion(form);
        setLocalError(error);
        if (!error) submit.mutate();
    };

    return (
        <form className="glass p-5 sm:p-6 space-y-4" onSubmit={onSubmit} noValidate>
            <p className="muted">כתבו שאלה אמריקאית לחברי הכיתה: תשובה נכונה אחת ולפחות שתי תשובות שגויות שנראות הגיוניות. המורה יבדוק ויאשר.</p>
            <div className="field-wrap">
                <label className="label" htmlFor={`${ids}-q`}>נוסח השאלה</label>
                <textarea id={`${ids}-q`} className="field min-h-[5rem]" maxLength={2000} dir="auto"
                          value={form.questionText} onChange={set("questionText")} required />
            </div>
            <div className="field-wrap">
                <label className="label" htmlFor={`${ids}-a`}>התשובה הנכונה</label>
                <input id={`${ids}-a`} className="field" maxLength={500} dir="auto"
                       value={form.correctAnswer} onChange={set("correctAnswer")} required />
            </div>
            <fieldset className="space-y-2">
                <legend className="label">תשובות שגויות (מסיחים)</legend>
                {form.distractors.map((d, i) => (
                    <div key={i} className="flex gap-2 items-center">
                        <input className="field" maxLength={500} dir="auto" value={d} onChange={setDistractor(i)}
                               aria-label={`תשובה שגויה ${i + 1}`} />
                        {form.distractors.length > 2 && (
                            <button type="button" className="btn btn-ghost btn-sm" aria-label={`הסרת תשובה שגויה ${i + 1}`}
                                    onClick={() => setForm((f) => ({ ...f, distractors: f.distractors.filter((_, j) => j !== i) }))}>
                                <FaTrash aria-hidden="true" />
                            </button>
                        )}
                    </div>
                ))}
                {form.distractors.length < MAX_DISTRACTORS && (
                    <button type="button" className="btn btn-ghost btn-sm"
                            onClick={() => setForm((f) => ({ ...f, distractors: [...f.distractors, ""] }))}>
                        <FaPlus aria-hidden="true" /> עוד תשובה שגויה
                    </button>
                )}
            </fieldset>
            <div className="grid gap-3 sm:grid-cols-2">
                <div className="field-wrap">
                    <label className="label" htmlFor={`${ids}-t`}>נושא (לא חובה)</label>
                    <input id={`${ids}-t`} className="field" maxLength={100} value={form.topic} onChange={set("topic")} />
                </div>
                <div className="field-wrap">
                    <label className="label" htmlFor={`${ids}-e`}>הסבר לפתרון (לא חובה)</label>
                    <input id={`${ids}-e`} className="field" maxLength={2000} value={form.explanation} onChange={set("explanation")} />
                </div>
            </div>
            {(localError || submit.isError) && (
                <div className="notice notice--danger" role="alert">{localError || errorMessage(submit.error, "לא ניתן לשלוח את השאלה")}</div>
            )}
            {submit.isSuccess && <div className="notice notice--mint" role="status">{submit.data?.errorCode || "השאלה נשלחה למורה"}</div>}
            <button className="btn btn-primary" disabled={submit.isPending}>שליחה לאישור המורה</button>
        </form>
    );
}

function MineTab() {
    const mine = useQuery({ queryKey: ["peer-mine"], queryFn: peerApi.mine });
    if (mine.isLoading) return <SkeletonList rows={3} />;
    if (mine.isError) return <div className="notice notice--danger">{errorMessage(mine.error, "לא ניתן לטעון את השאלות שלך")}</div>;
    if (!mine.data?.length) {
        return <div className="glass"><EmptyState title="עוד לא כתבת שאלות" body="השאלות שתכתבו יופיעו כאן עם מצב האישור." /></div>;
    }
    return mine.data.map((q, i) => {
        const status = PEER_STATUS[q.status] ?? PEER_STATUS.PENDING;
        const rate = successRate(q.attempts, q.correctAttempts);
        return (
            <div key={q.id} className="glass p-5 space-y-2 reveal" style={{ "--i": Math.min(i, 8) }}>
                <div className="flex flex-wrap items-center gap-2">
                    <span className={`chip ${status.chip}`}>{status.label}</span>
                    {q.topic && <span className="chip">{q.topic}</span>}
                    <span className="faint text-xs">{formatWhen(q.createdAt)}</span>
                </div>
                <p className="title text-base whitespace-pre-line" dir="auto">{q.questionText}</p>
                <p className="muted text-sm">
                    תשובה נכונה: <strong dir="auto">{q.correctAnswer}</strong> · מסיחים: <span dir="auto">{q.distractors.join(" | ")}</span>
                </p>
                {q.reviewNote && <div className="notice">הערת המורה: {q.reviewNote}</div>}
                {q.status === "APPROVED" && (
                    <p className="faint text-xs">
                        חברי הכיתה ענו <span className="num">{q.attempts}</span> פעמים
                        {rate != null && <> · <span className="num">{rate}%</span> הצלחה</>}
                    </p>
                )}
            </div>
        );
    });
}

export default function PeerQuestions() {
    const [tab, setTab] = useState("practice");
    const ids = useId();

    return (
        <div className="page page--medium space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-4 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge icon-badge--amber" aria-hidden="true"><FaLightbulb /></span>
                    <div>
                        <span className="eyebrow">Peer questions · שאלות מהכיתה</span>
                        <h2 className="display text-3xl sm:text-4xl">שאלות שכתבו חברי הכיתה</h2>
                    </div>
                </div>
                <div className="flex flex-wrap gap-2" role="tablist" aria-label="שאלות מהכיתה">
                    {TABS.map((t) => (
                        <button key={t.id} type="button" role="tab" id={`${ids}-${t.id}`}
                                aria-selected={tab === t.id} aria-controls={`${ids}-panel`}
                                className={`choice ${tab === t.id ? "is-active" : ""}`} onClick={() => setTab(t.id)}>
                            {t.label}
                        </button>
                    ))}
                </div>
            </div>
            <div id={`${ids}-panel`} role="tabpanel" aria-labelledby={`${ids}-${tab}`} className="space-y-4">
                {tab === "practice" && <PracticeTab />}
                {tab === "write" && <WriteTab />}
                {tab === "mine" && <MineTab />}
            </div>
        </div>
    );
}
