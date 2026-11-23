import { useEffect, useId, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaCheckCircle, FaComments, FaPaperPlane, FaSearch, FaTrash } from "react-icons/fa";
import EmptyState from "../../components/ui/EmptyState.jsx";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import { errorMessage } from "../../lib/api.js";
import { forumApi } from "./classCommunityApi.js";
import { formatWhen, similarQuery } from "./classCommunityUtils.js";

// The class forum, shared by the student page (ask, answer) and the teacher page (moderate, approve).
// New answers show up by polling (TanStack Query refetchInterval) against the server.

const LIST_POLL_MS = 20000;
const THREAD_POLL_MS = 10000;

function useDebounced(value, delay = 400) {
    const [debounced, setDebounced] = useState(value);
    useEffect(() => {
        const id = setTimeout(() => setDebounced(value), delay);
        return () => clearTimeout(id);
    }, [value, delay]);
    return debounced;
}

function ThreadRow({ thread, active, onOpen, index }) {
    return (
        <li>
            <button
                type="button"
                onClick={() => onOpen(thread.id)}
                className={`row-card w-full text-start space-y-1 reveal ${active ? "choice is-active" : ""}`}
                style={{ "--i": Math.min(index, 8) }}
                aria-current={active ? "true" : undefined}
            >
                <div className="flex flex-wrap items-center gap-2">
                    {thread.topic && <span className="chip">{thread.topic}</span>}
                    {thread.resolved && <span className="chip chip--mint">✓ יש תשובה מאושרת</span>}
                    {thread.matchScore != null && <span className="chip chip--amber num">{thread.matchScore}% דומה</span>}
                </div>
                <p className="title text-base">{thread.title}</p>
                <p className="faint text-xs">
                    {thread.authorName} · <span className="num">{thread.answerCount}</span> תשובות · {formatWhen(thread.lastActivityAt)}
                </p>
            </button>
        </li>
    );
}

function NewThreadForm({ classId, onCreated, onOpen }) {
    const [form, setForm] = useState({ title: "", topic: "", exerciseText: "", body: "" });
    const ids = useId();
    const query = useDebounced(similarQuery(form.title, form.exerciseText));
    const similar = useQuery({
        queryKey: ["forum-similar", classId ?? "mine", query],
        queryFn: () => forumApi.similar(classId, query),
        enabled: Boolean(query),
    });
    const queryClient = useQueryClient();
    const create = useMutation({
        mutationFn: () => forumApi.createThread(form),
        onSuccess: (res) => {
            setForm({ title: "", topic: "", exerciseText: "", body: "" });
            queryClient.invalidateQueries({ queryKey: ["forum-threads"] });
            onCreated(res.data);
        },
    });
    const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

    return (
        <form
            className="glass p-5 sm:p-6 space-y-3"
            onSubmit={(e) => { e.preventDefault(); create.mutate(); }}
            aria-labelledby={`${ids}-h`}
        >
            <h3 id={`${ids}-h`} className="title text-lg">שאלה חדשה לכיתה</h3>
            <div className="field-wrap">
                <label className="label" htmlFor={`${ids}-title`}>על מה נתקעת?</label>
                <input id={`${ids}-title`} className="field" maxLength={200} value={form.title} onChange={set("title")} required />
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
                <div className="field-wrap">
                    <label className="label" htmlFor={`${ids}-topic`}>נושא (לא חובה)</label>
                    <input id={`${ids}-topic`} className="field" maxLength={100} value={form.topic} onChange={set("topic")} placeholder="למשל: שברים" />
                </div>
                <div className="field-wrap">
                    <label className="label" htmlFor={`${ids}-ex`}>התרגיל (לא חובה)</label>
                    <input id={`${ids}-ex`} className="field num" maxLength={1000} value={form.exerciseText} onChange={set("exerciseText")} dir="auto" />
                </div>
            </div>

            {query && (similar.data?.length ?? 0) > 0 && (
                <div className="notice space-y-2" role="status" aria-live="polite">
                    <p className="font-semibold">אולי כבר שאלו את זה? כדאי להציץ בדיונים האלה:</p>
                    <ul className="space-y-1">
                        {similar.data.map((t) => (
                            <li key={t.id}>
                                <button type="button" className="btn btn-ghost btn-sm" onClick={() => onOpen(t.id)}>
                                    {t.resolved && <FaCheckCircle aria-label="יש תשובה מאושרת" />} {t.title}
                                </button>
                            </li>
                        ))}
                    </ul>
                </div>
            )}

            <div className="field-wrap">
                <label className="label" htmlFor={`${ids}-body`}>מה ניסית עד עכשיו? (לא חובה)</label>
                <textarea id={`${ids}-body`} className="field min-h-[6rem]" maxLength={4000} value={form.body} onChange={set("body")} />
            </div>
            {create.isError && <div className="notice notice--danger">{errorMessage(create.error, "לא ניתן לפרסם את השאלה")}</div>}
            <button className="btn btn-primary" disabled={create.isPending || !form.title.trim()}>
                <FaPaperPlane aria-hidden="true" /> פרסום בפורום הכיתה
            </button>
        </form>
    );
}

function ThreadView({ threadId, moderator, onDeleted }) {
    const queryClient = useQueryClient();
    const [reply, setReply] = useState("");
    const ids = useId();
    const thread = useQuery({
        queryKey: ["forum-thread", threadId],
        queryFn: () => forumApi.thread(threadId),
        refetchInterval: THREAD_POLL_MS,
    });
    const refresh = () => {
        queryClient.invalidateQueries({ queryKey: ["forum-thread", threadId] });
        queryClient.invalidateQueries({ queryKey: ["forum-threads"] });
    };
    const send = useMutation({
        mutationFn: () => forumApi.reply(threadId, reply),
        onSuccess: () => { setReply(""); refresh(); },
    });
    const approve = useMutation({ mutationFn: ({ id, approved }) => forumApi.approve(id, approved), onSuccess: refresh });
    const removePost = useMutation({ mutationFn: (id) => forumApi.deletePost(id), onSuccess: refresh });
    const removeThread = useMutation({
        mutationFn: () => forumApi.deleteThread(threadId),
        onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["forum-threads"] }); onDeleted(); },
    });
    const actionError = [send, approve, removePost, removeThread].find((m) => m.isError)?.error;

    if (thread.isLoading) return <div className="glass p-5"><SkeletonList rows={3} /></div>;
    if (thread.isError) return <div className="notice notice--danger">{errorMessage(thread.error, "הדיון לא נמצא")}</div>;
    const t = thread.data;
    // Approved answers first, then by time.
    const posts = [...(t.posts ?? [])].sort((a, b) => Number(b.approved) - Number(a.approved));

    return (
        <article className="glass p-5 sm:p-6 space-y-4" aria-labelledby={`${ids}-t`}>
            <header className="space-y-1">
                <div className="flex flex-wrap items-center gap-2">
                    {t.topic && <span className="chip">{t.topic}</span>}
                    {t.resolved && <span className="chip chip--mint">✓ יש תשובה מאושרת</span>}
                </div>
                <h3 id={`${ids}-t`} className="title text-xl">{t.title}</h3>
                <p className="faint text-xs">{t.authorName} · {formatWhen(t.createdAt)}</p>
            </header>
            {t.exerciseText && <pre className="row-card num whitespace-pre-wrap" dir="auto">{t.exerciseText}</pre>}
            {t.body && <p className="whitespace-pre-line">{t.body}</p>}
            {moderator && (
                <button type="button" className="btn btn-danger btn-sm" disabled={removeThread.isPending}
                        onClick={() => window.confirm("למחוק את הדיון וכל התשובות בו?") && removeThread.mutate()}>
                    <FaTrash aria-hidden="true" /> מחיקת הדיון
                </button>
            )}

            <section className="space-y-3" aria-label="תשובות">
                {posts.length === 0 && <EmptyState title="עוד אין תשובות" body="אפשר להיות הראשונים לעזור." />}
                {posts.map((p) => (
                    <div key={p.id} className={`row-card space-y-2 ${p.approved ? "choice is-active" : ""}`}>
                        <div className="flex flex-wrap items-center gap-2">
                            <span className="font-semibold">{p.authorName}</span>
                            {p.authorRole === "TEACHER" && <span className="chip chip--amber">מורה</span>}
                            {p.approved && <span className="chip chip--mint">✓ תשובה מאושרת{p.approvedByName ? ` · ${p.approvedByName}` : ""}</span>}
                            <span className="faint text-xs">{formatWhen(p.createdAt)}</span>
                        </div>
                        <p className="whitespace-pre-line">{p.body}</p>
                        {moderator && (
                            <div className="flex flex-wrap gap-2">
                                <button type="button" className={`btn btn-sm ${p.approved ? "btn-ghost" : "btn-mint"}`}
                                        disabled={approve.isPending}
                                        onClick={() => approve.mutate({ id: p.id, approved: !p.approved })}>
                                    <FaCheckCircle aria-hidden="true" /> {p.approved ? "ביטול האישור" : "סימון כתשובה מאושרת"}
                                </button>
                                <button type="button" className="btn btn-ghost btn-sm" disabled={removePost.isPending}
                                        aria-label={`מחיקת התשובה של ${p.authorName}`}
                                        onClick={() => window.confirm("למחוק את התשובה?") && removePost.mutate(p.id)}>
                                    <FaTrash aria-hidden="true" /> מחיקה
                                </button>
                            </div>
                        )}
                    </div>
                ))}
            </section>

            <form className="space-y-2" onSubmit={(e) => { e.preventDefault(); send.mutate(); }}>
                <label className="label" htmlFor={`${ids}-reply`}>{moderator ? "תשובת המורה" : "התשובה שלך"}</label>
                <textarea id={`${ids}-reply`} className="field min-h-[5rem]" maxLength={4000} value={reply} onChange={(e) => setReply(e.target.value)} />
                <button className="btn btn-primary btn-sm" disabled={send.isPending || !reply.trim()}>
                    <FaPaperPlane aria-hidden="true" /> שליחת תשובה
                </button>
            </form>
            {actionError && <div className="notice notice--danger">{errorMessage(actionError, "הפעולה נכשלה")}</div>}
        </article>
    );
}

export default function ClassForumBoard({ moderator = false }) {
    const [classId, setClassId] = useState(null);
    const [search, setSearch] = useState("");
    const [openId, setOpenId] = useState(null);
    const ids = useId();
    const debouncedSearch = useDebounced(search.trim());

    const classes = useQuery({ queryKey: ["forum-classes"], queryFn: forumApi.classes, enabled: moderator });
    useEffect(() => {
        if (moderator && classId == null && classes.data?.length) setClassId(classes.data[0].id);
    }, [moderator, classId, classes.data]);

    const ready = !moderator || classId != null;
    const threads = useQuery({
        queryKey: ["forum-threads", classId ?? "mine", debouncedSearch],
        queryFn: () => forumApi.threads(classId, debouncedSearch),
        enabled: ready,
        refetchInterval: LIST_POLL_MS,
    });

    return (
        <div className="grid gap-5 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
            <section className="space-y-4" aria-label="רשימת הדיונים">
                {moderator && (
                    <div className="glass p-4 field-wrap">
                        <label className="label" htmlFor={`${ids}-class`}>כיתה</label>
                        {classes.isLoading ? <SkeletonList rows={1} /> : (classes.data?.length ?? 0) === 0 ? (
                            <EmptyState title="אין כיתות" body="לא נמצאו כיתות שאתם מלמדים." />
                        ) : (
                            <select id={`${ids}-class`} className="field" value={classId ?? ""}
                                    onChange={(e) => { setClassId(Number(e.target.value)); setOpenId(null); }}>
                                {classes.data.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                            </select>
                        )}
                    </div>
                )}
                <div className="glass p-4 field-wrap">
                    <label className="label" htmlFor={`${ids}-search`}><FaSearch aria-hidden="true" /> חיפוש בדיונים</label>
                    <input id={`${ids}-search`} className="field" type="search" value={search}
                           onChange={(e) => setSearch(e.target.value)} placeholder="נושא, מילה מהשאלה או התרגיל" />
                </div>
                {threads.isLoading && ready && <SkeletonList rows={4} />}
                {threads.isError && <div className="notice notice--danger">{errorMessage(threads.error, "לא ניתן לטעון את הפורום")}</div>}
                {threads.data?.length === 0 && (
                    <div className="glass">
                        <EmptyState icon={<FaComments />} title={debouncedSearch ? "לא נמצאו דיונים מתאימים" : "הפורום עוד ריק"}
                                    body={moderator ? "כשתלמידים ישאלו, השאלות יופיעו כאן." : "נתקעת בתרגיל? שאלו את הכיתה."} />
                    </div>
                )}
                {(threads.data?.length ?? 0) > 0 && (
                    <ul className="space-y-2">
                        {threads.data.map((t, i) => (
                            <ThreadRow key={t.id} thread={t} index={i} active={t.id === openId} onOpen={setOpenId} />
                        ))}
                    </ul>
                )}
            </section>

            <section className="space-y-4" aria-label="דיון">
                {openId && (
                    <div className="flex justify-end">
                        <button type="button" className="btn btn-ghost btn-sm" onClick={() => setOpenId(null)}>
                            {moderator ? "סגירת הדיון" : "חזרה לשאלה חדשה"}
                        </button>
                    </div>
                )}
                {openId ? (
                    <ThreadView key={openId} threadId={openId} moderator={moderator} onDeleted={() => setOpenId(null)} />
                ) : moderator ? (
                    <div className="glass"><EmptyState title="בחרו דיון" body="אשרו תשובות נכונות ✓ כדי שתלמידים שייתקעו באותה שאלה יראו אותן מיד." /></div>
                ) : (
                    <NewThreadForm classId={classId} onOpen={setOpenId} onCreated={(t) => t && setOpenId(t.id)} />
                )}
            </section>
        </div>
    );
}
