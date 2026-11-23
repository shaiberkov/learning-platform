import { useContext, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { UserContext } from "../../context/UserContext.jsx";
import { useNotificationContext } from "../../context/NotificationContext.jsx";
import {
    ATTACHMENT,
    MESSAGE_BASE_PATH,
    MESSAGE_MARK_READ,
    READ_RECEIPTS,
    RECEIVED_MESSAGES_PAGE,
    REPLY,
    SENT_MESSAGES,
    THREAD,
} from "../../constants/pages.constants.js";
import { apiGet, apiPost, apiPostForm, downloadFile, errorMessage } from "../../lib/api.js";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import EmptyState from "../../components/ui/EmptyState.jsx";

const PAGE_SIZE = 10;
const MAX_FILE = 5 * 1024 * 1024;
const SENDER_ROLES = ["SYSTEM_ADMIN", "SCHOOLMANAGER", "TEACHER"];

function formatDate(value, lng) {
    return value ? new Date(value).toLocaleString(lng, { dateStyle: "short", timeStyle: "short" }) : "";
}

function AttachmentButton({ userId, attachmentId, name }) {
    const { t } = useTranslation();
    if (!attachmentId) return null;
    return (
        <button
            type="button"
            className="chip hover:!text-accent"
            onClick={() => downloadFile(`${MESSAGE_BASE_PATH}${ATTACHMENT}`, { userId, attachmentId }, name)}
        >
            <span aria-hidden="true">📎</span> {name || t("messages.attachment")}
            <span className="sr-only"> — {t("common.download")}</span>
        </button>
    );
}

function Pager({ page, totalPages, onChange }) {
    const { t } = useTranslation();
    if (!totalPages || totalPages <= 1) return null;
    return (
        <nav className="flex items-center justify-between gap-2 pt-3" aria-label={t("common.pageOf", { page: page + 1, total: totalPages })}>
            <button type="button" className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => onChange(page - 1)}>
                {t("common.previousPage")}
            </button>
            <span className="faint text-xs num">{t("common.pageOf", { page: page + 1, total: totalPages })}</span>
            <button type="button" className="btn btn-ghost btn-sm" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
                {t("common.nextPage")}
            </button>
        </nav>
    );
}

function ThreadPanel({ userId, threadId, replyToId, onClose }) {
    const { t, i18n } = useTranslation();
    const queryClient = useQueryClient();
    const [content, setContent] = useState("");
    const [file, setFile] = useState(null);
    const [error, setError] = useState("");
    const headingRef = useRef(null);

    useEffect(() => { headingRef.current?.focus(); }, [threadId]);

    const thread = useQuery({
        queryKey: ["thread", userId, threadId],
        queryFn: () => apiGet(`${MESSAGE_BASE_PATH}${THREAD}`, { userId, threadId }),
        enabled: !!threadId,
    });

    const reply = useMutation({
        mutationFn: () => apiPostForm(`${MESSAGE_BASE_PATH}${REPLY}`, { userId, messageId: lastIncomingId(), content, file }),
        onSuccess: () => {
            setContent("");
            setFile(null);
            queryClient.invalidateQueries({ queryKey: ["thread", userId, threadId] });
            queryClient.invalidateQueries({ queryKey: ["inbox", userId] });
            queryClient.invalidateQueries({ queryKey: ["sent", userId] });
        },
        onError: (e) => setError(errorMessage(e, t("common.error"))),
    });

    // Reply to the newest message from the other side, so the answer reaches that person.
    function lastIncomingId() {
        const rows = thread.data || [];
        const incoming = [...rows].reverse().find((m) => !m.mine);
        return incoming?.id ?? replyToId ?? rows[rows.length - 1]?.id;
    }

    const submit = (e) => {
        e.preventDefault();
        setError("");
        if (file && file.size > MAX_FILE) {
            setError(t("messages.fileTooLarge"));
            return;
        }
        if (content.trim()) reply.mutate();
    };

    return (
        <section className="glass p-5 sm:p-6 flex flex-col gap-4" aria-labelledby="thread-title">
            <div className="flex items-center justify-between">
                <h2 id="thread-title" ref={headingRef} tabIndex={-1} className="title text-xl outline-none">{t("messages.thread")}</h2>
                <button type="button" className="btn btn-ghost btn-sm" onClick={onClose}>{t("common.close")}</button>
            </div>
            {thread.isLoading && <SkeletonList rows={2} />}
            {thread.isError && <p className="notice notice--danger">{errorMessage(thread.error, t("common.error"))}</p>}
            <ol className="space-y-3 max-h-[26rem] overflow-y-auto">
                {(thread.data || []).map((m) => (
                    <li key={m.id} className={`row-card ${m.mine ? "!bg-accent-soft" : ""}`}>
                        <div className="flex justify-between gap-2 mb-1">
                            <span className="chip">{m.mine ? t("messages.you") : m.senderName}</span>
                            <span className="num faint text-[0.7rem]" dir="ltr">{formatDate(m.sentAt, i18n.language)}</span>
                        </div>
                        <p className="title text-sm">{m.title}</p>
                        <p className="muted text-sm mt-1 whitespace-pre-wrap break-words">{m.content}</p>
                        <div className="mt-2"><AttachmentButton userId={userId} attachmentId={m.attachmentId} name={m.attachmentName} /></div>
                    </li>
                ))}
            </ol>
            <form onSubmit={submit} className="space-y-3 border-t border-dashed border-line pt-4">
                <label htmlFor="reply-content" className="label">{t("messages.reply")}</label>
                <textarea id="reply-content" className="field min-h-[6rem] resize-y" value={content}
                          placeholder={t("messages.replyPlaceholder")} onChange={(e) => setContent(e.target.value)} />
                <div className="flex flex-wrap items-center justify-between gap-2">
                    <label className="btn btn-ghost btn-sm cursor-pointer">
                        <span aria-hidden="true">📎</span> {file ? file.name : t("messages.attach")}
                        <input type="file" className="sr-only" onChange={(e) => setFile(e.target.files?.[0] || null)} />
                    </label>
                    <button type="submit" className="btn btn-primary btn-sm" disabled={!content.trim() || reply.isPending}>
                        {t("messages.send")}
                    </button>
                </div>
                {error && <p className="notice notice--danger" role="alert">{error}</p>}
            </form>
        </section>
    );
}

function Receipts({ userId, batchId }) {
    const { t, i18n } = useTranslation();
    const receipts = useQuery({
        queryKey: ["receipts", userId, batchId],
        queryFn: () => apiGet(`${MESSAGE_BASE_PATH}${READ_RECEIPTS}`, { userId, batchId }),
    });
    if (receipts.isLoading) return <SkeletonList rows={1} />;
    if (receipts.isError) return <p className="notice notice--danger">{errorMessage(receipts.error, t("common.error"))}</p>;
    return (
        <table className="data-table w-full text-sm mt-2">
            <caption className="sr-only">{t("messages.receipts")}</caption>
            <thead>
                <tr><th scope="col">{t("roles.STUDENT")}</th><th scope="col">{t("messages.read")}</th></tr>
            </thead>
            <tbody>
                {receipts.data.map((r, i) => (
                    <tr key={i}>
                        <td>{r.recipientName}</td>
                        <td className="num" dir="ltr">{r.readAt ? formatDate(r.readAt, i18n.language) : <span className="faint">{t("messages.notRead")}</span>}</td>
                    </tr>
                ))}
            </tbody>
        </table>
    );
}

export default function MessagesPage() {
    const { user } = useContext(UserContext);
    const { revision } = useNotificationContext();
    const { t, i18n } = useTranslation();
    const queryClient = useQueryClient();
    const canSend = SENDER_ROLES.includes(user?.role);
    const [tab, setTab] = useState("inbox");
    const [page, setPage] = useState(0);
    const [openThread, setOpenThread] = useState(null);
    const [openReceipts, setOpenReceipts] = useState(null);
    const userId = user?.userId;

    const inbox = useQuery({
        queryKey: ["inbox", userId, page, revision],
        queryFn: () => apiGet(`${MESSAGE_BASE_PATH}${RECEIVED_MESSAGES_PAGE}`, { userId, page, size: PAGE_SIZE }),
        enabled: !!userId && tab === "inbox",
        placeholderData: (prev) => prev,
    });

    const sent = useQuery({
        queryKey: ["sent", userId],
        queryFn: () => apiGet(`${MESSAGE_BASE_PATH}${SENT_MESSAGES}`, { userId, size: 50 }),
        enabled: !!userId && tab === "sent" && canSend,
    });

    const markRead = useMutation({
        mutationFn: (messageId) => apiPost(`${MESSAGE_BASE_PATH}${MESSAGE_MARK_READ}`, { userId, messageId }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["inbox", userId] }),
    });

    if (!user) return null;

    const open = (m) => {
        if (!m.readAt) markRead.mutate(m.id);
        setOpenThread({ threadId: m.threadId, replyToId: m.id });
    };

    const tabs = [{ id: "inbox", label: t("messages.inbox") }, ...(canSend ? [{ id: "sent", label: t("messages.sent") }] : [])];

    return (
        <div className="page page--wide space-y-5" dir={i18n.dir()}>
            <header className="glass p-6 sm:p-8 flex flex-wrap items-end justify-between gap-4">
                <div>
                    <span className="eyebrow">{t("messages.eyebrow")}</span>
                    <h1 className="display text-3xl sm:text-4xl mt-2">{t("messages.title")}</h1>
                </div>
                {canSend && <Link to="/send-message" className="btn btn-primary">{t("messages.newMessage")}</Link>}
            </header>

            <div role="tablist" aria-label={t("messages.title")} className="flex gap-2">
                {tabs.map((tb) => (
                    <button key={tb.id} id={`tab-${tb.id}`} role="tab" type="button" aria-selected={tab === tb.id}
                            aria-controls={`panel-${tb.id}`}
                            className={`choice ${tab === tb.id ? "is-active" : ""}`}
                            onClick={() => { setTab(tb.id); setPage(0); }}>
                        {tb.label}
                    </button>
                ))}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-5 items-start">
                <section id={`panel-${tab}`} role="tabpanel" aria-labelledby={`tab-${tab}`} className="glass p-5 sm:p-6">
                    {tab === "inbox" && (
                        <>
                            {inbox.isLoading && <SkeletonList rows={4} />}
                            {inbox.isError && <p className="notice notice--danger">{errorMessage(inbox.error, t("common.error"))}</p>}
                            {inbox.data && inbox.data.items.length === 0 && <EmptyState body={t("messages.empty")} />}
                            <ul className="space-y-3">
                                {(inbox.data?.items || []).map((m) => (
                                    <li key={m.id}>
                                        <button type="button" onClick={() => open(m)}
                                                className={`row-card w-full text-start ${m.readAt ? "" : "border-accent"}`}>
                                            <span className="flex justify-between items-center gap-3 mb-2">
                                                <span className="chip">{m.senderName}</span>
                                                <span className="num faint text-[0.7rem]" dir="ltr">{formatDate(m.sentAt, i18n.language)}</span>
                                            </span>
                                            <span className="flex items-center gap-2">
                                                {!m.readAt && <span className="w-2 h-2 rounded-full bg-accent" aria-hidden="true" />}
                                                <span className="title text-base">{m.title}</span>
                                                <span className="sr-only">{m.readAt ? t("messages.read") : t("messages.unread")}</span>
                                            </span>
                                            <span className="block muted text-sm mt-1 line-clamp-2 break-words">{m.content}</span>
                                            {m.attachmentId && <span className="block faint text-xs mt-2">📎 {m.attachmentName}</span>}
                                        </button>
                                    </li>
                                ))}
                            </ul>
                            <Pager page={page} totalPages={inbox.data?.totalPages} onChange={setPage} />
                        </>
                    )}

                    {tab === "sent" && (
                        <>
                            {sent.isLoading && <SkeletonList rows={4} />}
                            {sent.isError && <p className="notice notice--danger">{errorMessage(sent.error, t("common.error"))}</p>}
                            {sent.data && sent.data.length === 0 && (
                                <EmptyState body={t("messages.emptySent")} actionLabel={t("messages.newMessage")} actionTo="/send-message" />
                            )}
                            <ul className="space-y-3">
                                {(sent.data || []).map((m) => (
                                    <li key={m.batchId} className="row-card">
                                        <div className="flex justify-between items-center gap-3 mb-2">
                                            <span className={`chip ${m.pending ? "" : "!text-mint"}`}>
                                                {m.pending
                                                    ? t("messages.scheduled", { date: formatDate(m.scheduledFor, i18n.language) })
                                                    : t("messages.readBy", { read: m.readCount, total: m.recipientsCount })}
                                            </span>
                                            <span className="num faint text-[0.7rem]" dir="ltr">{formatDate(m.sentAt, i18n.language)}</span>
                                        </div>
                                        <p className="title text-base">{m.title}</p>
                                        <p className="muted text-sm mt-1 line-clamp-2 break-words">{m.content}</p>
                                        <div className="flex flex-wrap gap-2 mt-3">
                                            {!m.pending && (
                                                <button type="button" className="btn btn-ghost btn-sm" aria-expanded={openReceipts === m.batchId}
                                                        onClick={() => setOpenReceipts(openReceipts === m.batchId ? null : m.batchId)}>
                                                    {t("messages.receipts")}
                                                </button>
                                            )}
                                            {!m.pending && (
                                                <button type="button" className="btn btn-ghost btn-sm" onClick={() => setOpenThread({ threadId: m.threadId })}>
                                                    {t("messages.openThread")}
                                                </button>
                                            )}
                                            <AttachmentButton userId={userId} attachmentId={m.attachmentId} />
                                        </div>
                                        {openReceipts === m.batchId && <Receipts userId={userId} batchId={m.batchId} />}
                                    </li>
                                ))}
                            </ul>
                        </>
                    )}
                </section>

                {openThread?.threadId && (
                    <ThreadPanel userId={userId} threadId={openThread.threadId} replyToId={openThread.replyToId}
                                 onClose={() => setOpenThread(null)} />
                )}
            </div>
        </div>
    );
}
