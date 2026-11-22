import { useContext, useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { UserContext } from "../../context/UserContext.jsx";
import { useNotificationContext } from "../../context/NotificationContext.jsx";
import {
    MARK_ALL_READ,
    NOTIFICATION_BASE_PATH,
    NOTIFICATION_LIST,
    NOTIFICATION_MARK_READ,
} from "../../constants/pages.constants.js";
import { apiGet, apiPost } from "../../lib/api.js";
import { SkeletonList } from "../ui/Skeleton.jsx";
import EmptyState from "../ui/EmptyState.jsx";

// The bell in the navbar: unread badge, live/reconnecting state and the latest notifications.
export default function NotificationBell() {
    const { user } = useContext(UserContext);
    const { unreadCount, setUnreadCount, refreshUnread, connected, revision } = useNotificationContext();
    const [open, setOpen] = useState(false);
    const panelRef = useRef(null);
    const buttonRef = useRef(null);
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const { t, i18n } = useTranslation();

    const listKey = ["notifications", user?.userId, revision];
    const { data, isLoading, isError } = useQuery({
        queryKey: listKey,
        queryFn: () => apiGet(`${NOTIFICATION_BASE_PATH}${NOTIFICATION_LIST}`, { userId: user.userId, page: 0, size: 15 }),
        enabled: open && !!user?.userId,
    });

    const markRead = useMutation({
        mutationFn: (id) => apiPost(`${NOTIFICATION_BASE_PATH}${NOTIFICATION_MARK_READ}`, { userId: user.userId, notificationId: id }),
        onSuccess: () => {
            refreshUnread();
            queryClient.invalidateQueries({ queryKey: ["notifications", user?.userId] });
        },
    });

    const markAll = useMutation({
        mutationFn: () => apiPost(`${NOTIFICATION_BASE_PATH}${MARK_ALL_READ}`, { userId: user.userId }),
        onSuccess: () => {
            setUnreadCount(0);
            queryClient.invalidateQueries({ queryKey: ["notifications", user?.userId] });
        },
    });

    useEffect(() => {
        if (!open) return;
        const onKey = (e) => {
            if (e.key === "Escape") {
                setOpen(false);
                buttonRef.current?.focus();
            }
        };
        const onClick = (e) => {
            if (!panelRef.current?.contains(e.target) && !buttonRef.current?.contains(e.target)) setOpen(false);
        };
        document.addEventListener("keydown", onKey);
        document.addEventListener("mousedown", onClick);
        return () => {
            document.removeEventListener("keydown", onKey);
            document.removeEventListener("mousedown", onClick);
        };
    }, [open]);

    if (!user) return null;
    const items = data?.items ?? [];

    return (
        <div className="relative" data-tour="bell">
            <button
                ref={buttonRef}
                type="button"
                onClick={() => setOpen((o) => !o)}
                className="relative grid place-items-center w-11 h-11 rounded-2xl bg-surface-sunk text-ink border border-line hover:border-accent hover:text-accent transition-colors"
                aria-label={t("notifications.open", { count: unreadCount })}
                aria-expanded={open}
                aria-controls="notification-panel"
            >
                <svg aria-hidden="true" className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.4-1.4A2 2 0 0118 14.2V11a6 6 0 10-12 0v3.2a2 2 0 01-.6 1.4L4 17h5m6 0a3 3 0 11-6 0" />
                </svg>
                {unreadCount > 0 && <span className="badge-dot num" aria-hidden="true">{unreadCount > 99 ? "99+" : unreadCount}</span>}
            </button>

            {open && (
                <div
                    ref={panelRef}
                    id="notification-panel"
                    role="dialog"
                    aria-label={t("notifications.title")}
                    dir={i18n.dir()}
                    className="absolute top-[calc(100%+0.6rem)] end-0 w-[min(24rem,calc(100vw-2rem))] glass glass--strong !rounded-[22px] p-4 z-[70] shadow-[var(--shadow-2)]"
                    style={{ animation: "rise .4s var(--ease-out-expo) both" }}
                >
                    <div className="flex items-center justify-between gap-2 pb-3 mb-2 border-b border-dashed border-line">
                        <div className="grid">
                            <span className="title text-base">{t("notifications.title")}</span>
                            <span className={`text-[0.7rem] ${connected ? "text-mint" : "text-ink-3"}`}>
                                {connected ? t("notifications.live") : t("notifications.reconnecting")}
                            </span>
                        </div>
                        <button type="button" className="btn btn-ghost btn-sm" onClick={() => markAll.mutate()} disabled={unreadCount === 0}>
                            {t("notifications.markAllRead")}
                        </button>
                    </div>

                    <div className="max-h-[22rem] overflow-y-auto -mx-1 px-1">
                        {isLoading && <SkeletonList rows={3} />}
                        {isError && <p className="notice notice--danger">{t("common.error")}</p>}
                        {!isLoading && !isError && items.length === 0 && <EmptyState body={t("notifications.empty")} />}
                        <ul className="space-y-2">
                            {items.map((n) => (
                                <li key={n.id}>
                                    <button
                                        type="button"
                                        className={`w-full text-start row-card !p-3 ${n.read ? "" : "border-accent"}`}
                                        onClick={() => {
                                            if (!n.read) markRead.mutate(n.id);
                                            setOpen(false);
                                            if (n.link) navigate(n.link);
                                        }}
                                    >
                                        <span className="flex items-center gap-2">
                                            {!n.read && <span className="w-2 h-2 rounded-full bg-accent shrink-0" aria-hidden="true" />}
                                            <span className="title text-sm">{n.title}</span>
                                            {!n.read && <span className="sr-only">{t("messages.unread")}</span>}
                                        </span>
                                        {n.body && <span className="block muted text-xs mt-1 line-clamp-2 break-words">{n.body}</span>}
                                        <span className="block num faint text-[0.68rem] mt-1" dir="ltr">
                                            {new Date(n.createdAt).toLocaleString(i18n.language)}
                                        </span>
                                    </button>
                                </li>
                            ))}
                        </ul>
                    </div>

                    <div className="pt-3 mt-2 border-t border-dashed border-line">
                        <Link to="/settings" className="link text-sm" onClick={() => setOpen(false)}>
                            {t("notifications.settings")}
                        </Link>
                    </div>
                </div>
            )}
        </div>
    );
}
