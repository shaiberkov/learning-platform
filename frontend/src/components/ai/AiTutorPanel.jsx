import { useEffect, useRef, useState } from "react";
import { FaCamera, FaMicrophone, FaStop, FaTimes, FaTrashAlt } from "react-icons/fa";
import { AI_STATUS, AI_TUTOR_HISTORY } from "../../constants/pages.constants.js";
import { aiDelete, aiGet, resizeImage, streamTutor, transcribeAudio } from "./aiApi.js";
import AiPolicyNotice from "./AiPolicyNotice.jsx";

const SpeechRecognitionImpl =
    typeof window !== "undefined" ? window.SpeechRecognition || window.webkitSpeechRecognition : undefined;
const MAX_RECORDING_MS = 30000;

/**
 * The Socratic AI tutor. With a questionId it knows the practice question on screen and the
 * student's current answer (studentAnswer); without one it is a free chat, for a photographed or
 * spoken exercise. Answers stream in word by word.
 */
export default function AiTutorPanel({ questionId, studentAnswer, showPolicy = true }) {
    const [status, setStatus] = useState(null);
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState("");
    const [image, setImage] = useState(null);
    const [streaming, setStreaming] = useState(false);
    const [notice, setNotice] = useState("");
    const [listening, setListening] = useState(false);
    const [recording, setRecording] = useState(false);
    const [transcribing, setTranscribing] = useState(false);
    const [preferServerVoice, setPreferServerVoice] = useState(!SpeechRecognitionImpl);

    const endRef = useRef(null);
    const fileRef = useRef(null);
    const abortRef = useRef(null);
    const recognitionRef = useRef(null);
    const recorderRef = useRef(null);

    useEffect(() => {
        let cancelled = false;
        (async () => {
            const [statusRes, historyRes] = await Promise.all([
                aiGet(AI_STATUS),
                aiGet(AI_TUTOR_HISTORY, questionId ? { questionId } : undefined),
            ]);
            if (cancelled) return;
            if (statusRes.success) setStatus(statusRes.data);
            else setNotice(statusRes.errorCode);
            setMessages(historyRes.success ? historyRes.data.map((m) => ({ role: m.role, content: m.content })) : []);
        })();
        return () => {
            cancelled = true;
        };
    }, [questionId]);

    useEffect(() => () => {
        abortRef.current?.abort();
        recognitionRef.current?.abort();
        if (recorderRef.current?.state === "recording") recorderRef.current.stop();
    }, []);

    useEffect(() => {
        endRef.current?.scrollIntoView({ behavior: "auto", block: "end" });
    }, [messages]);

    const locked = status && (!status.available || status.examLocked);
    const remaining = status ? Math.max(0, status.dailyLimit - status.usedToday) : null;

    const updateLast = (fn) =>
        setMessages((prev) => {
            const next = [...prev];
            next[next.length - 1] = fn(next[next.length - 1]);
            return next;
        });

    const send = async () => {
        const text = input.trim();
        if (streaming || locked || (!text && !image)) return;
        setNotice("");
        setMessages((prev) => [
            ...prev,
            { role: "user", content: text || "📷 צירפתי תמונה של תרגיל", image },
            { role: "assistant", content: "", pending: true },
        ]);
        setInput("");
        const imageDataUrl = image;
        setImage(null);
        setStreaming(true);
        const controller = new AbortController();
        abortRef.current = controller;

        await streamTutor(
            { questionId: questionId ?? null, studentAnswer: studentAnswer ?? "", message: text, imageDataUrl },
            {
                signal: controller.signal,
                onDelta: (delta) => updateLast((m) => ({ ...m, content: m.content + delta, pending: false })),
                onReplace: (full) => updateLast((m) => ({ ...m, content: full, pending: false })),
                onDone: (left) =>
                    setStatus((s) => (s ? { ...s, usedToday: Math.max(0, s.dailyLimit - left) } : s)),
                onError: (message) => updateLast((m) => ({ ...m, content: message, pending: false, error: true })),
            }
        );
        updateLast((m) => (m.pending ? { ...m, content: "לא התקבלה תשובה. נסו שוב.", pending: false, error: true } : m));
        setStreaming(false);
    };

    const onKeyDown = (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            send();
        }
    };

    const pickPhoto = async (e) => {
        const file = e.target.files?.[0];
        e.target.value = "";
        if (!file) return;
        try {
            setImage(await resizeImage(file));
        } catch {
            setNotice("לא הצלחנו לקרוא את התמונה. נסו תמונה אחרת.");
        }
    };

    // Voice: the browser's Hebrew speech recognition when it exists, otherwise record and let the server transcribe.
    const startBrowserVoice = () => {
        const recognition = new SpeechRecognitionImpl();
        recognition.lang = "he-IL";
        recognition.interimResults = true;
        recognition.continuous = false;
        const before = input ? `${input} ` : "";
        recognition.onresult = (event) => {
            const transcript = Array.from(event.results).map((r) => r[0].transcript).join("");
            setInput(before + transcript);
        };
        recognition.onerror = (event) => {
            if (event.error === "not-allowed") {
                setNotice("צריך לאשר גישה למיקרופון בדפדפן.");
            } else if (event.error !== "no-speech" && event.error !== "aborted") {
                setPreferServerVoice(true);
                setNotice("זיהוי הדיבור בדפדפן לא זמין. לחצו שוב על המיקרופון כדי להקליט.");
            }
        };
        recognition.onend = () => setListening(false);
        recognitionRef.current = recognition;
        recognition.start();
        setListening(true);
    };

    const startServerVoice = async () => {
        if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === "undefined") {
            setNotice("הדפדפן לא תומך בהקלטה. אפשר להקליד את השאלה.");
            return;
        }
        let stream;
        try {
            stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        } catch {
            setNotice("צריך לאשר גישה למיקרופון בדפדפן.");
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
            if (res.success) setInput((prev) => (prev ? `${prev} ${res.data}` : res.data));
            else setNotice(res.errorCode);
        };
        recorderRef.current = recorder;
        recorder.start();
        setRecording(true);
        setTimeout(() => recorder.state === "recording" && recorder.stop(), MAX_RECORDING_MS);
    };

    const toggleVoice = () => {
        setNotice("");
        if (listening) return recognitionRef.current?.stop();
        if (recording) return recorderRef.current?.stop();
        if (preferServerVoice) startServerVoice();
        else startBrowserVoice();
    };

    const clearHistory = async () => {
        const res = await aiDelete(AI_TUTOR_HISTORY);
        if (res.success) setMessages([]);
        setNotice(res.errorCode || "");
    };

    return (
        <div className="flex flex-col h-full min-h-0" dir="rtl">
            <div className="flex-1 min-h-0 overflow-y-auto p-4 space-y-3 flex flex-col">
                {showPolicy && <AiPolicyNotice retentionDays={status?.retentionDays ?? 30} />}

                {status?.examLocked && (
                    <p className="notice notice--danger">יש לך מבחן פעיל כרגע, ולכן עוזר ה-AI כבוי עד סוף המבחן.</p>
                )}
                {status && !status.available && (
                    <p className="notice">עוזר ה-AI עדיין לא הופעל בשרת. פנו למנהל המערכת.</p>
                )}

                {messages.length === 0 && !locked && (
                    <p className="faint text-sm">
                        {questionId
                            ? "תקועים? כתבו מה ניסיתם, ואעזור לכם להתקדם צעד אחד – בלי לגלות את התשובה."
                            : "צלמו תרגיל, הקליטו אותו או כתבו אותו – ונפתור אותו יחד, צעד אחר צעד."}
                    </p>
                )}

                {messages.map((m, i) => (
                    <div
                        key={i}
                        className={`max-w-[85%] px-4 py-2.5 text-sm leading-relaxed whitespace-pre-wrap ${
                            m.role === "user"
                                ? "self-start rounded-[18px] rounded-br-md bg-[linear-gradient(135deg,var(--accent),var(--accent-2))] text-[var(--accent-ink)]"
                                : `self-end rounded-[18px] rounded-bl-md border text-ink ${
                                      m.error ? "border-danger bg-surface-sunk" : "bg-surface-sunk border-line"
                                  }`
                        }`}
                    >
                        <strong className="block num text-[0.62rem] uppercase tracking-[0.14em] opacity-70 mb-1">
                            {m.role === "user" ? "אני" : "מורה-עזר AI"}
                        </strong>
                        {m.image && <img src={m.image} alt="התרגיל שצולם" className="rounded-xl mb-2 max-h-40" />}
                        {m.pending ? (
                            <span className="dot-pulse"><span /><span /><span /></span>
                        ) : (
                            m.content
                        )}
                    </div>
                ))}
                <div ref={endRef} />
            </div>

            {notice && <p className="notice mx-3 mb-2 text-sm">{notice}</p>}

            {image && (
                <div className="mx-3 mb-2 flex items-center gap-2">
                    <img src={image} alt="תצוגה מקדימה" className="h-14 rounded-lg border border-line" />
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => setImage(null)} title="הסר תמונה">
                        <FaTimes />
                    </button>
                </div>
            )}

            <div className="p-3 border-t border-dashed border-line flex items-end gap-2">
                <textarea
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={onKeyDown}
                    rows={1}
                    maxLength={1200}
                    disabled={locked || streaming}
                    placeholder={transcribing ? "ממיר הקלטה לטקסט..." : "כתבו הודעה..."}
                    className="field !min-h-[2.75rem] !py-2 text-sm resize-none"
                />
                <button
                    type="button"
                    onClick={toggleVoice}
                    disabled={locked || streaming || transcribing}
                    className={`btn btn-sm !min-h-[2.75rem] ${listening || recording ? "btn-danger" : "btn-ghost"}`}
                    title={listening || recording ? "עצור" : "דברו בעברית"}
                    aria-label={listening || recording ? "עצור הקלטה" : "הקלטת קול"}
                >
                    {listening || recording ? <FaStop /> : <FaMicrophone />}
                </button>
                <button
                    type="button"
                    onClick={() => fileRef.current?.click()}
                    disabled={locked || streaming}
                    className="btn btn-ghost btn-sm !min-h-[2.75rem]"
                    title="צלמו תרגיל"
                    aria-label="צילום תרגיל"
                >
                    <FaCamera />
                </button>
                <input ref={fileRef} type="file" accept="image/*" capture="environment" className="hidden" onChange={pickPhoto} />
                <button
                    type="button"
                    onClick={send}
                    disabled={locked || streaming || (!input.trim() && !image)}
                    className="btn btn-primary btn-sm !min-h-[2.75rem]"
                >
                    שלח
                </button>
            </div>

            <div className="px-4 pb-3 flex items-center justify-between text-xs faint">
                <span className="num">{remaining !== null ? `נותרו היום: ${remaining}/${status.dailyLimit}` : ""}</span>
                <button type="button" onClick={clearHistory} className="inline-flex items-center gap-1 hover:text-danger">
                    <FaTrashAlt /> מחק שיחות
                </button>
            </div>
        </div>
    );
}
