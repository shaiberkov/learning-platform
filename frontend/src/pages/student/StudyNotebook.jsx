import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaBookmark, FaPen, FaRegStickyNote, FaTrash } from "react-icons/fa";
import { errorMessage } from "../../lib/api.js";
import EmptyState from "../../components/ui/EmptyState.jsx";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import { deleteNote, fetchNoteTopics, fetchNotes, updateNote } from "../../features/studyTools/studyToolsApi.js";
import {
    NOTE_KINDS, TARGET_LABELS, groupNotesByTarget, targetLink,
} from "../../features/studyTools/studyToolsUtils.js";
import { formatDateTime } from "../../Utils/authApi.js";

// [s04] The personal study notebook: every note, highlight and bookmark the student attached to a
// question, a lesson, a mistakes-notebook item or a flashcard, filtered by topic (before a test,
// open one topic and read everything in one place) and by kind.

function NoteItem({ note }) {
    const queryClient = useQueryClient();
    const [editing, setEditing] = useState(false);
    const [body, setBody] = useState(note.body || "");
    const [error, setError] = useState(null);
    const refresh = () => queryClient.invalidateQueries({ queryKey: ["studyNotes"] });

    const save = useMutation({
        mutationFn: () => updateNote(note.id, { ...note, body }),
        onSuccess: () => { setEditing(false); setError(null); refresh(); },
        onError: (err) => setError(errorMessage(err, "השמירה נכשלה")),
    });
    const remove = useMutation({
        mutationFn: () => deleteNote(note.id),
        onSuccess: refresh,
        onError: (err) => setError(errorMessage(err, "המחיקה נכשלה")),
    });

    return (
        <li className="row-card space-y-2">
            <div className="flex flex-wrap items-center justify-between gap-2">
                <span className={`chip ${note.kind === "HIGHLIGHT" ? "chip--amber" : note.kind === "BOOKMARK" ? "chip--mint" : ""}`}>
                    {note.kind === "BOOKMARK" ? <FaBookmark aria-hidden="true" /> : <FaRegStickyNote aria-hidden="true" />}
                    {NOTE_KINDS[note.kind]}
                </span>
                <span className="faint text-xs num">{formatDateTime(note.updatedAt || note.createdAt)}</span>
            </div>
            {note.quote && <blockquote className="glass--sunk p-3 text-ink-2 whitespace-pre-line">{note.quote}</blockquote>}
            {editing ? (
                <div className="space-y-2">
                    <label className="label" htmlFor={`note-edit-${note.id}`}>עריכת ההערה</label>
                    <textarea id={`note-edit-${note.id}`} className="field" maxLength={2000} value={body}
                              onChange={(e) => setBody(e.target.value)} />
                    <div className="flex gap-2">
                        <button type="button" className="btn btn-primary btn-sm" disabled={save.isPending}
                                onClick={() => save.mutate()}>שמירה</button>
                        <button type="button" className="btn btn-ghost btn-sm"
                                onClick={() => { setEditing(false); setBody(note.body || ""); }}>ביטול</button>
                    </div>
                </div>
            ) : (
                note.body && <p className="text-ink whitespace-pre-line break-words">{note.body}</p>
            )}
            {error && <p className="notice notice--danger" role="status">{error}</p>}
            {!editing && (
                <div className="flex gap-2">
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => setEditing(true)}
                            aria-label="עריכת ההערה"><FaPen aria-hidden="true" /> עריכה</button>
                    <button type="button" className="btn btn-ghost btn-sm" disabled={remove.isPending}
                            aria-label="מחיקת ההערה"
                            onClick={() => { if (window.confirm("למחוק את ההערה?")) remove.mutate(); }}>
                        <FaTrash aria-hidden="true" /> מחיקה
                    </button>
                </div>
            )}
        </li>
    );
}

export default function StudyNotebook() {
    const [topic, setTopic] = useState("");
    const [kind, setKind] = useState("");
    const topics = useQuery({ queryKey: ["studyNotes", "topics"], queryFn: fetchNoteTopics });
    const notes = useQuery({
        queryKey: ["studyNotes", "list", topic],
        queryFn: () => fetchNotes(topic ? { topic } : {}),
    });

    const visible = (notes.data || []).filter((n) => !kind || n.kind === kind);
    const groups = groupNotesByTarget(visible);

    return (
        <div className="page page--wide space-y-4 sm:space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge icon-badge--amber"><FaRegStickyNote aria-hidden="true" /></span>
                    <div>
                        <span className="eyebrow">Study notebook · מחברת אישית</span>
                        <h1 className="display text-3xl sm:text-4xl mt-1">המחברת שלי</h1>
                    </div>
                </div>
                <p className="muted mt-4 leading-relaxed">
                    כל ההערות, ההדגשות והסימניות שכתבת על שאלות, שיעורים וכרטיסיות. לפני מבחן בחרו נושא וקראו הכול
                    במקום אחד. מוסיפים הערה בכפתור &quot;הערה&quot; בשיעור, בתרגול ובמחברת הטעויות.
                </p>
            </div>

            <div className="glass p-5 space-y-3 reveal" style={{ "--i": 1 }}>
                <div className="field-wrap">
                    <label className="label" htmlFor="notebook-topic">נושא</label>
                    <select id="notebook-topic" className="field" value={topic} onChange={(e) => setTopic(e.target.value)}>
                        <option value="">כל הנושאים</option>
                        {(topics.data || []).map((t) => (
                            <option key={t.topic} value={t.topic}>{t.topic} ({t.count})</option>
                        ))}
                    </select>
                </div>
                <div className="flex flex-wrap gap-2" role="radiogroup" aria-label="סוג">
                    {[["", "הכול"], ...Object.entries(NOTE_KINDS)].map(([value, label]) => (
                        <button key={value || "all"} type="button" role="radio" aria-checked={kind === value}
                                className={`choice ${kind === value ? "is-active" : ""}`} onClick={() => setKind(value)}>
                            {label}
                        </button>
                    ))}
                </div>
            </div>

            {notes.isLoading ? (
                <SkeletonList rows={4} />
            ) : notes.isError ? (
                <p className="notice notice--danger">{errorMessage(notes.error, "לא הצלחנו לטעון את המחברת")}</p>
            ) : groups.length === 0 ? (
                <div className="glass">
                    <EmptyState icon={<FaRegStickyNote />}
                                title={topic || kind ? "אין הערות שמתאימות לסינון" : "המחברת ריקה"}
                                body="לחצו על &quot;הערה&quot; בשיעור, בתרגול או במחברת הטעויות כדי לרשום לעצמכם מה חשוב לזכור."
                                actionLabel="לשיעורים" actionTo="/lessons" />
                </div>
            ) : (
                <div className="space-y-4">
                    {groups.map((group, i) => {
                        const link = targetLink(group);
                        return (
                            <section key={group.key} className="glass p-5 sm:p-6 space-y-3 reveal"
                                     style={{ "--i": Math.min(i + 2, 10) }} aria-label={group.title}>
                                <div className="flex flex-wrap items-start justify-between gap-2">
                                    <div className="space-y-1 min-w-0">
                                        <span className="faint text-xs">
                                            {TARGET_LABELS[group.targetType]}
                                            {group.topic ? ` · ${group.topic}` : ""}
                                            {group.subTopic ? ` · ${group.subTopic}` : ""}
                                        </span>
                                        <h2 className="title text-lg break-words">{group.title}</h2>
                                    </div>
                                    {link && <Link to={link} className="btn btn-ghost btn-sm">מעבר</Link>}
                                </div>
                                <ul className="space-y-2">
                                    {group.notes.map((note) => <NoteItem key={note.id} note={note} />)}
                                </ul>
                            </section>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
