import { useEffect, useRef, useState } from "react";
import { FaChalkboardTeacher, FaMicrophone, FaStop } from "react-icons/fa";
import EmptyState from "../../components/ui/EmptyState.jsx";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import { transcribeAudio } from "../../components/ai/aiApi.js";
import { fetchFeynmanHistory, fetchFeynmanPrompts, submitFeynmanExplanation } from "../../features/diagnosis/diagnosisApi.js";
import { scoreBand } from "../../features/diagnosis/mistakeTypes.js";

// [s06] "Explain it to me" (the Feynman technique): the student explains an idea in their own words,
// typed or recorded; the server checks whether it shows understanding and lists the gaps.

const MAX_CHARS = 1200;
const MAX_RECORDING_MS = 60000;

const formatDateTime = (iso) => (iso ? new Date(iso).toLocaleString("he-IL", { dateStyle: "short", timeStyle: "short" }) : "");

function Result({ attempt }) {
    const band = scoreBand(attempt.score);
    return (
        <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
                <span className={`chip ${band.chip}`}>{band.label}</span>
                <span className="chip num">{attempt.score ?? "—"}/100</span>
                <span className="chip">{attempt.evaluatedBy === "AI" ? "נבדק ב-AI" : "נבדק לפי רעיונות מפתח"}</span>
                {attempt.inputMode === "VOICE" && <span className="chip">הוקלט</span>}
            </div>
            {attempt.feedback && <p className="text-sm">{attempt.feedback}</p>}
            {attempt.gaps?.length > 0 && (
                <div>
                    <p className="text-sm font-medium">חורים בהסבר:</p>
                    <ul className="list-disc ps-5 text-sm space-y-1">
                        {attempt.gaps.map((gap) => <li key={gap}>{gap}</li>)}
                    </ul>
                </div>
            )}
        </div>
    );
}

export default function ExplainIt() {
    const [prompts, setPrompts] = useState(null);
    const [history, setHistory] = useState([]);
    const [promptKey, setPromptKey] = useState("");
    const [text, setText] = useState("");
    const [usedVoice, setUsedVoice] = useState(false);
    const [notice, setNotice] = useState({ text: "", ok: true });
    const [result, setResult] = useState(null);
    const [sending, setSending] = useState(false);
    const [recording, setRecording] = useState(false);
    const [transcribing, setTranscribing] = useState(false);
    const recorderRef = useRef(null);

    useEffect(() => {
        let cancelled = false;
        Promise.all([fetchFeynmanPrompts(), fetchFeynmanHistory()]).then(([p, h]) => {
            if (cancelled) return;
            if (p.success) {
                setPrompts(p.data);
                if (p.data.length) setPromptKey(p.data[0].key);
            } else {
                setPrompts([]);
                setNotice({ text: p.errorCode, ok: false });
            }
            if (h.success) setHistory(h.data);
        });
        return () => {
            cancelled = true;
            if (recorderRef.current?.state === "recording") recorderRef.current.stop();
        };
    }, []);

    const prompt = prompts?.find((p) => p.key === promptKey);

    const startRecording = async () => {
        if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === "undefined") {
            setNotice({ text: "הדפדפן לא תומך בהקלטה. אפשר להקליד את ההסבר.", ok: false });
            return;
        }
        let stream;
        try {
            stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        } catch {
            setNotice({ text: "צריך לאשר גישה למיקרופון בדפדפן.", ok: false });
            return;
        }
        const recorder = new MediaRecorder(stream);
        const chunks = [];
        recorder.ondataavailable = (e) => e.data.size && chunks.push(e.data);
        recorder.onstop = async () => {
            stream.getTracks().forEach((t) => t.stop());
            setRecording(false);
            if (!chunks.length) return;
            setTranscribing(true);
            const res = await transcribeAudio(new Blob(chunks, { type: recorder.mimeType || "audio/webm" }));
            setTranscribing(false);
            if (res.success) {
                setText((prev) => (prev ? `${prev} ${res.data}` : res.data).slice(0, MAX_CHARS));
                setUsedVoice(true);
            } else {
                setNotice({ text: res.errorCode, ok: false });
            }
        };
        recorderRef.current = recorder;
        recorder.start();
        setRecording(true);
        setNotice({ text: "", ok: true });
        setTimeout(() => recorder.state === "recording" && recorder.stop(), MAX_RECORDING_MS);
    };

    const submit = async (e) => {
        e.preventDefault();
        if (!promptKey || !text.trim()) return;
        setSending(true);
        const res = await submitFeynmanExplanation(promptKey, text, usedVoice ? "VOICE" : "TEXT");
        setSending(false);
        // errorCode on a successful answer explains a fallback (for example: the AI is not configured).
        setNotice({ text: res.errorCode || "", ok: res.success });
        if (res.success) {
            setResult(res.data);
            setHistory((prev) => [res.data, ...prev]);
            setText("");
            setUsedVoice(false);
        }
    };

    return (
        <div className="page page--wide space-y-4 sm:space-y-5" dir="rtl">
            <section className="glass p-6 sm:p-8 reveal space-y-3">
                <div className="flex items-center gap-3">
                    <span className="icon-badge icon-badge--mint"><FaChalkboardTeacher aria-hidden="true" /></span>
                    <div>
                        <span className="eyebrow">Explain it · שיטת פיינמן</span>
                        <h1 className="display text-3xl sm:text-4xl mt-1">תסביר לי</h1>
                    </div>
                </div>
                <p className="muted leading-relaxed">
                    הדרך הטובה ביותר לבדוק אם הבנתם משהו היא להסביר אותו במילים שלכם, כאילו אתם מלמדים חבר. כתבו או הקליטו
                    הסבר, ונבדוק אם הוא מראה הבנה אמיתית ואיפה יש חורים.
                </p>
            </section>

            {notice.text && <p role="status" className={`notice ${notice.ok ? "" : "notice--danger"}`}>{notice.text}</p>}

            {prompts === null ? (
                <SkeletonList rows={2} />
            ) : (
                <form onSubmit={submit} className="glass p-6 sm:p-8 space-y-4 reveal" style={{ "--i": 1 }}>
                    <div className="field-wrap">
                        <label className="label" htmlFor="feynman-prompt">על מה להסביר</label>
                        <select id="feynman-prompt" className="field" value={promptKey} onChange={(e) => setPromptKey(e.target.value)}>
                            {prompts.map((p) => (
                                <option key={p.key} value={p.key}>{p.topic} · {p.prompt}</option>
                            ))}
                        </select>
                    </div>
                    {prompt && <p className="title text-xl">{prompt.prompt}</p>}
                    <div className="field-wrap">
                        <label className="label" htmlFor="feynman-text">ההסבר שלי</label>
                        <textarea
                            id="feynman-text"
                            className="field min-h-40"
                            maxLength={MAX_CHARS}
                            placeholder="הסבירו במילים שלכם למה זה עובד, לא רק מה עושים"
                            value={text}
                            onChange={(e) => setText(e.target.value)}
                        />
                        <span className="num text-xs text-ink-3">{text.length}/{MAX_CHARS}</span>
                    </div>
                    <div className="flex flex-wrap gap-2">
                        <button type="submit" className="btn btn-primary" disabled={!text.trim() || sending || recording || transcribing}>
                            {sending ? "בודק..." : "בדוק את ההסבר"}
                        </button>
                        <button
                            type="button"
                            className="btn btn-ghost"
                            onClick={() => (recording ? recorderRef.current?.stop() : startRecording())}
                            disabled={transcribing || sending}
                            aria-pressed={recording}
                        >
                            {recording ? <><FaStop aria-hidden="true" /> עצור הקלטה</> : <><FaMicrophone aria-hidden="true" /> הקלט הסבר</>}
                        </button>
                        {transcribing && <span role="status" className="faint text-sm self-center">ממיר את ההקלטה לטקסט...</span>}
                    </div>
                    {result && (
                        <div className="glass--sunk rounded-xl p-4" aria-live="polite">
                            <Result attempt={result} />
                        </div>
                    )}
                </form>
            )}

            <section className="glass p-6 sm:p-8 space-y-3 reveal" style={{ "--i": 2 }}>
                <h2 className="title text-xl">ההסברים הקודמים שלי</h2>
                {history.length === 0 ? (
                    <EmptyState title="עוד אין הסברים" body="כל הסבר שתשלחו יישמר כאן עם הציון והחורים שנמצאו." />
                ) : (
                    history.map((h) => (
                        <article key={h.id} className="row-card !block space-y-2">
                            <div className="flex flex-wrap items-center justify-between gap-2">
                                <strong>{h.promptText}</strong>
                                <span className="num text-xs text-ink-3">{formatDateTime(h.createdAt)}</span>
                            </div>
                            <p className="glass--sunk rounded-xl p-3 text-sm whitespace-pre-wrap">{h.explanationText}</p>
                            <Result attempt={h} />
                        </article>
                    ))
                )}
            </section>
        </div>
    );
}
