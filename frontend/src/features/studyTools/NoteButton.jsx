import { useCallback, useEffect, useId, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaRegStickyNote } from "react-icons/fa";
import { errorMessage } from "../../lib/api.js";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import { createNote, fetchNotes } from "./studyToolsApi.js";
import { NOTE_KINDS } from "./studyToolsUtils.js";

// [s04] "Note" button for a question, a lesson, a mistakes-notebook item or a flashcard. Opens a dialog
// to write a note, save the selected text as a highlight, or bookmark the target, and lists the
// notes the student already wrote on it. target: { targetType, targetId, targetTitle, subject, topic, subTopic }.
export default function NoteButton({ target, className = "btn btn-ghost btn-sm" }) {
    const [open, setOpen] = useState(false);
    const [kind, setKind] = useState("NOTE");
    const [body, setBody] = useState("");
    const [quote, setQuote] = useState("");
    const [message, setMessage] = useState(null);
    const buttonRef = useRef(null);
    const textRef = useRef(null);
    const titleId = useId();
    const queryClient = useQueryClient();
    const enabled = open && target?.targetId != null;

    const existing = useQuery({
        queryKey: ["studyNotes", "target", target?.targetType, target?.targetId],
        queryFn: () => fetchNotes({ targetType: target.targetType, targetId: target.targetId }),
        enabled,
    });

    const save = useMutation({
        mutationFn: () => createNote({ ...target, kind, body, quote: kind === "HIGHLIGHT" ? quote : null }),
        onSuccess: (res) => {
            setMessage({ ok: true, text: res.errorCode || "נשמר במחברת" });
            setBody("");
            queryClient.invalidateQueries({ queryKey: ["studyNotes"] });
        },
        onError: (err) => setMessage({ ok: false, text: errorMessage(err, "לא הצלחנו לשמור. נסו שוב.") }),
    });

    const openDialog = () => {
        // Text the student selected on the page becomes a highlight.
        const selected = typeof window !== "undefined" ? String(window.getSelection?.() || "").trim().slice(0, 1000) : "";
        setQuote(selected);
        setKind(selected ? "HIGHLIGHT" : "NOTE");
        setMessage(null);
        setOpen(true);
    };

    const close = useCallback(() => {
        setOpen(false);
        buttonRef.current?.focus();
    }, []);

    useEffect(() => {
        if (!open) return undefined;
        textRef.current?.focus();
        const onKey = (e) => { if (e.key === "Escape") close(); };
        window.addEventListener("keydown", onKey);
        return () => window.removeEventListener("keydown", onKey);
    }, [open, close]);

    if (target?.targetId == null) return null;

    const canSave = kind === "BOOKMARK" || (kind === "NOTE" ? body.trim() : quote.trim());

    return (
        <>
            <button ref={buttonRef} type="button" className={className} onClick={openDialog}
                    aria-haspopup="dialog" aria-label="הוספת הערה למחברת האישית">
                <FaRegStickyNote aria-hidden="true" /> הערה
            </button>

            {open && (
                <div className="modal-backdrop" onMouseDown={(e) => { if (e.target === e.currentTarget) close(); }}>
                    <div className="modal space-y-4 text-right" dir="rtl" role="dialog" aria-modal="true" aria-labelledby={titleId}>
                        <div>
                            <span className="eyebrow">המחברת האישית</span>
                            <h2 id={titleId} className="title text-xl mt-1">הערה על {target.targetTitle || "הפריט"}</h2>
                            {target.topic && <span className="chip mt-2">{target.topic}</span>}
                        </div>

                        <div className="grid grid-cols-3 gap-2" role="radiogroup" aria-label="סוג">
                            {Object.entries(NOTE_KINDS).map(([value, label]) => (
                                <button key={value} type="button" role="radio" aria-checked={kind === value}
                                        className={`choice ${kind === value ? "is-active" : ""}`} onClick={() => setKind(value)}>
                                    {label}
                                </button>
                            ))}
                        </div>

                        {kind === "HIGHLIGHT" && (
                            <div className="field-wrap">
                                <label className="label" htmlFor={`${titleId}-quote`}>הקטע המודגש</label>
                                <textarea id={`${titleId}-quote`} className="field" maxLength={1000} value={quote}
                                          onChange={(e) => setQuote(e.target.value)}
                                          placeholder="סמנו טקסט בעמוד לפני הלחיצה, או הדביקו אותו כאן" />
                            </div>
                        )}

                        <div className="field-wrap">
                            <label className="label" htmlFor={`${titleId}-body`}>
                                {kind === "NOTE" ? "ההערה שלי" : "הערה (לא חובה)"}
                            </label>
                            <textarea id={`${titleId}-body`} ref={textRef} className="field" maxLength={2000} value={body}
                                      onChange={(e) => setBody(e.target.value)}
                                      placeholder="למשל: פה טעיתי כי שכחתי להפוך סימן" />
                        </div>

                        {message && (
                            <p role="status" className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</p>
                        )}

                        <div className="flex flex-wrap justify-between items-center gap-2">
                            <Link to="/study-notebook" className="link text-sm">לכל המחברת</Link>
                            <div className="flex gap-2">
                                <button type="button" className="btn btn-primary" disabled={!canSave || save.isPending}
                                        onClick={() => save.mutate()}>
                                    שמירה
                                </button>
                                <button type="button" className="btn btn-ghost" onClick={close}>סגירה</button>
                            </div>
                        </div>

                        <section aria-label="הערות קודמות" className="space-y-2 pt-2 border-t border-dashed border-line">
                            <h3 className="faint text-xs">כבר במחברת</h3>
                            {existing.isLoading ? (
                                <SkeletonList rows={1} />
                            ) : (existing.data || []).length === 0 ? (
                                <p className="muted text-sm">עוד אין הערות על הפריט הזה.</p>
                            ) : (
                                <ul className="space-y-2 max-h-48 overflow-y-auto">
                                    {existing.data.map((n) => (
                                        <li key={n.id} className="row-card text-sm space-y-1">
                                            <span className="chip">{NOTE_KINDS[n.kind]}</span>
                                            {n.quote && <p className="text-ink-2 italic">"{n.quote}"</p>}
                                            {n.body && <p className="text-ink whitespace-pre-line">{n.body}</p>}
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </section>
                    </div>
                </div>
            )}
        </>
    );
}
