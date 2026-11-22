import {createContext, useCallback, useContext, useEffect, useRef, useState} from "react";
import { UserContext } from "./UserContext";
import {
    CONNECT,
    NOTIFICATION_BASE_PATH,
    NOTIFICATION_LIST,
    PREFERENCES,
    QUESTION,
    UNREAD_COUNT,
    USER_ID
} from "../constants/pages.constants.js";
import {apiGet} from "../lib/api.js";

export const NotificationContext = createContext();

// Reconnect delays after the stream drops: 2s, 4s, 8s ... capped at 60s.
const BASE_DELAY = 2000;
const MAX_DELAY = 60000;
const MAX_TOASTS = 3;
const lastSeenKey = (userId) => `lp-last-notification-${userId}`;

function readLastSeen(userId) {
    try {
        const v = Number(localStorage.getItem(lastSeenKey(userId)));
        return Number.isFinite(v) && v > 0 ? v : null;
    } catch {
        return null;
    }
}

function writeLastSeen(userId, id) {
    try {
        localStorage.setItem(lastSeenKey(userId), String(id));
    } catch {
        /* storage unavailable: missed notifications are then fetched from the latest page */
    }
}

export function NotificationProvider({ children }) {
    const [messages, setMessages] = useState([]);
    const [tests, setTests] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [connected, setConnected] = useState(false);
    const [toasts, setToasts] = useState([]);
    const [revision, setRevision] = useState(0); // bumps when the notification list changed
    const [homeworkAlerts, setHomeworkAlerts] = useState([]);
    // Live in-class quiz: the last state pushed to a student, the last results pushed to the teacher.
    const [liveQuiz, setLiveQuiz] = useState(null);
    const [liveQuizResults, setLiveQuizResults] = useState(null);
    const { user } = useContext(UserContext);
    const showPopupsRef = useRef(true);

    const dismissToast = useCallback((id) => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
    }, []);

    const pushToast = useCallback((toast) => {
        if (!showPopupsRef.current) return;
        const id = toast.id ?? `${Date.now()}-${Math.random()}`;
        setToasts((prev) => [{ ...toast, id }, ...prev.filter((t) => t.id !== id)].slice(0, MAX_TOASTS));
        setTimeout(() => dismissToast(id), 7000);
    }, [dismissToast]);

    const refreshUnread = useCallback(async () => {
        if (!user?.userId) return;
        try {
            setUnreadCount(await apiGet(`${NOTIFICATION_BASE_PATH}${UNREAD_COUNT}`, { userId: user.userId }));
        } catch {
            /* the badge keeps its last value */
        }
    }, [user?.userId]);

    useEffect(() => {
        if (!user?.userId) return;
        const userId = user.userId;
        let eventSource = null;
        let retryTimer = null;
        let attempts = 0;
        let closed = false;

        apiGet(`${NOTIFICATION_BASE_PATH}${PREFERENCES}`, { userId })
            .then((prefs) => { showPopupsRef.current = prefs?.inApp !== false; })
            .catch(() => {});
        refreshUnread();

        const noteSeen = (id) => {
            if (!id) return;
            const last = readLastSeen(userId);
            if (!last || id > last) writeLastSeen(userId, id);
        };

        // After a (re)connect: fetch what arrived while the stream was down.
        const fetchMissed = async () => {
            const lastSeen = readLastSeen(userId);
            try {
                if (lastSeen) {
                    const missed = await apiGet(`${NOTIFICATION_BASE_PATH}${NOTIFICATION_LIST}`, { userId, afterId: lastSeen, size: 50 });
                    if (missed?.length) {
                        noteSeen(missed[missed.length - 1].id);
                        pushToast({ id: `missed-${lastSeen}`, missedCount: missed.length });
                        setRevision((r) => r + 1);
                    }
                } else {
                    const page = await apiGet(`${NOTIFICATION_BASE_PATH}${NOTIFICATION_LIST}`, { userId, size: 1 });
                    noteSeen(page?.items?.[0]?.id);
                }
                refreshUnread();
            } catch {
                /* the next reconnect tries again */
            }
        };

        const connect = () => {
            if (closed) return;
            eventSource = new EventSource(
                `${NOTIFICATION_BASE_PATH}${CONNECT}${QUESTION}${USER_ID}${encodeURIComponent(userId)}`
            );

            eventSource.addEventListener("connected", () => {
                attempts = 0;
                setConnected(true);
                fetchMissed();
            });

            eventSource.addEventListener("message", (event) => {
                try {
                    const data = JSON.parse(event.data);
                    noteSeen(data.id);
                    if (data.id) {
                        setUnreadCount((c) => c + 1);
                        setRevision((r) => r + 1);
                    }
                    if (data.type === "SYSTEM_MESSAGE") {
                        setMessages(prevMessages => [data.payload.message, ...prevMessages]);
                    } else if (data.type === "NEW_HOMEWORK" || data.type === "HOMEWORK_REMINDER") {
                        setHomeworkAlerts(prev => [{ ...data.payload, type: data.type }, ...prev]);
                    } else if (data.type === "NEW_TEST") {
                        if (user.role === "STUDENT") {
                            setTests(prevTests => [data.payload.userTestStatusDTO, ...prevTests]);
                        } else {
                            console.warn("User not authorized to receive test notifications");
                        }
                    } else if (data.type === "LIVE_QUIZ_STATE") {
                        setLiveQuiz({ ...data.payload, receivedAt: Date.now() });
                    } else if (data.type === "LIVE_QUIZ_RESULTS") {
                        setLiveQuizResults({ ...data.payload, receivedAt: Date.now() });
                    }
                    if (data.title) {
                        pushToast({ id: data.id, title: data.title, body: data.body, link: data.link });
                    }
                } catch (err) {
                    console.error("Failed to parse SSE notification:", err);
                }
            });

            eventSource.onerror = () => {
                setConnected(false);
                eventSource.close();
                if (closed) return;
                const delay = Math.min(MAX_DELAY, BASE_DELAY * 2 ** attempts);
                attempts += 1;
                retryTimer = setTimeout(connect, delay);
            };
        };

        // Coming back online or to the tab reconnects at once instead of waiting for the timer.
        const reconnectNow = () => {
            if (closed || (eventSource && eventSource.readyState !== EventSource.CLOSED)) return;
            clearTimeout(retryTimer);
            attempts = 0;
            connect();
        };
        const onVisible = () => { if (document.visibilityState === "visible") reconnectNow(); };

        connect();
        window.addEventListener("online", reconnectNow);
        document.addEventListener("visibilitychange", onVisible);

        return () => {
            closed = true;
            clearTimeout(retryTimer);
            eventSource?.close();
            window.removeEventListener("online", reconnectNow);
            document.removeEventListener("visibilitychange", onVisible);
            setConnected(false);
        };
    }, [user?.userId]); // eslint-disable-line react-hooks/exhaustive-deps

    const setShowPopups = useCallback((value) => { showPopupsRef.current = value; }, []);

    return (
        <NotificationContext.Provider value={{
            messages, tests, homeworkAlerts, liveQuiz, liveQuizResults, unreadCount, setUnreadCount, refreshUnread, connected,
            toasts, dismissToast, pushToast, revision, setShowPopups,
        }}>
            {children}
        </NotificationContext.Provider>
    );
}

export function useNotificationContext() {
    return useContext(NotificationContext);
}
