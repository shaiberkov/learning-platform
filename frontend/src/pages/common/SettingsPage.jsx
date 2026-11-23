import { useContext, useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { UserContext } from "../../context/UserContext.jsx";
import { useNotificationContext } from "../../context/NotificationContext.jsx";
import {
    CALENDAR_BASE_PATH,
    CALENDAR_TOKEN,
    NOTIFICATION_BASE_PATH,
    PREFERENCES,
    PUSH_PUBLIC_KEY,
    PUSH_SUBSCRIBE,
    PUSH_UNSUBSCRIBE,
    REGENERATE_CALENDAR_TOKEN,
} from "../../constants/pages.constants.js";
import { API_BASE_URL } from "../../constants/base.constants.js";
import { apiGet, apiPost, errorMessage } from "../../lib/api.js";
import { Skeleton } from "../../components/ui/Skeleton.jsx";
import LanguageSwitcher from "../../components/LanguageSwitcher.jsx";
import { START_TOUR_EVENT } from "../../components/onboarding/OnboardingTour.jsx";
import { pendingAnswers, syncAnswers } from "../../offline/practiceStore.js";
import { submitQueuedAnswer } from "../../offline/submitQueuedAnswer.js";

function urlBase64ToUint8Array(base64) {
    const padding = "=".repeat((4 - (base64.length % 4)) % 4);
    const raw = atob((base64 + padding).replace(/-/g, "+").replace(/_/g, "/"));
    return Uint8Array.from([...raw].map((c) => c.charCodeAt(0)));
}

function withTimeout(promise, ms) {
    return Promise.race([promise, new Promise((_, reject) => setTimeout(() => reject(new Error("timeout")), ms))]);
}

const pushSupported = () => "serviceWorker" in navigator && "PushManager" in window && "Notification" in window;

function Toggle({ id, label, hint, checked, disabled, onChange }) {
    return (
        <div className="flex items-start justify-between gap-4 py-3">
            <div>
                <label htmlFor={id} className="title text-sm">{label}</label>
                {hint && <p id={`${id}-hint`} className="faint text-xs mt-1">{hint}</p>}
            </div>
            <input
                id={id}
                type="checkbox"
                role="switch"
                className="w-5 h-5 mt-0.5 accent-[var(--accent)] shrink-0"
                checked={checked}
                disabled={disabled}
                aria-describedby={hint ? `${id}-hint` : undefined}
                onChange={(e) => onChange(e.target.checked)}
            />
        </div>
    );
}

export default function SettingsPage() {
    const { user } = useContext(UserContext);
    const { setShowPopups } = useNotificationContext();
    const queryClient = useQueryClient();
    const { t, i18n } = useTranslation();
    const [status, setStatus] = useState("");
    const [copied, setCopied] = useState(false);
    const [pending, setPending] = useState(0);

    const prefsKey = ["notification-prefs", user?.userId];
    const prefs = useQuery({
        queryKey: prefsKey,
        queryFn: () => apiGet(`${NOTIFICATION_BASE_PATH}${PREFERENCES}`, { userId: user.userId }),
        enabled: !!user?.userId,
    });

    const savePrefs = useMutation({
        mutationFn: (next) => apiPost(`${NOTIFICATION_BASE_PATH}${PREFERENCES}`, { userId: user.userId, ...next }),
        onSuccess: (res) => {
            queryClient.setQueryData(prefsKey, res.data);
            setShowPopups(res.data?.inApp !== false);
            setStatus(res.errorCode || "");
        },
        onError: (e) => setStatus(errorMessage(e, t("common.error"))),
    });

    const calendar = useQuery({
        queryKey: ["calendar-feed", user?.userId],
        queryFn: () => apiGet(`${CALENDAR_BASE_PATH}${CALENDAR_TOKEN}`, { userId: user.userId }),
        enabled: false, // created on request: a feed link is only made when the user asks for it
    });
    const regenerate = useMutation({
        mutationFn: () => apiPost(`${CALENDAR_BASE_PATH}${REGENERATE_CALENDAR_TOKEN}`, { userId: user.userId }),
        onSuccess: (res) => {
            queryClient.setQueryData(["calendar-feed", user?.userId], res.data);
            setStatus(res.errorCode || "");
        },
    });

    useEffect(() => {
        if (user?.role === "STUDENT") pendingAnswers().then((a) => setPending(a.length));
    }, [user?.role]);

    if (!user) return null;
    const p = prefs.data;
    const save = (patch) => savePrefs.mutate({ inApp: p.inApp, email: p.email, push: p.push, ...patch });

    const togglePush = async (on) => {
        setStatus("");
        if (!pushSupported()) {
            setStatus(t("settings.pushUnsupported"));
            return;
        }
        try {
            const reg = await withTimeout(navigator.serviceWorker.ready, 5000);
            if (on) {
                const permission = await Notification.requestPermission();
                if (permission !== "granted") {
                    setStatus(t("settings.pushDenied"));
                    return;
                }
                const key = await apiGet(`${NOTIFICATION_BASE_PATH}${PUSH_PUBLIC_KEY}`);
                const sub = await reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: urlBase64ToUint8Array(key) });
                const res = await apiPost(`${NOTIFICATION_BASE_PATH}${PUSH_SUBSCRIBE}`, { userId: user.userId }, sub.toJSON());
                setStatus(res.errorCode || "");
                queryClient.invalidateQueries({ queryKey: prefsKey });
            } else {
                const sub = await reg.pushManager.getSubscription();
                if (sub) {
                    await apiPost(`${NOTIFICATION_BASE_PATH}${PUSH_UNSUBSCRIBE}`, { userId: user.userId, endpoint: sub.endpoint });
                    await sub.unsubscribe();
                }
                save({ push: false });
            }
        } catch (e) {
            setStatus(errorMessage(e, t("settings.pushUnsupported")));
        }
    };

    const feedUrl = calendar.data ? `${API_BASE_URL}${calendar.data}` : "";

    const copyFeed = async () => {
        try {
            await navigator.clipboard.writeText(feedUrl);
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        } catch {
            /* clipboard blocked: the URL stays selectable in the field */
        }
    };

    return (
        <div className="page page--wide space-y-5" dir={i18n.dir()}>
            <header className="glass p-6 sm:p-8">
                <span className="eyebrow">{t("settings.eyebrow")}</span>
                <h1 className="display text-3xl sm:text-4xl mt-2">{t("settings.title")}</h1>
            </header>

            {status && <p className="notice" role="status">{status}</p>}

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
                <section className="glass p-6" aria-labelledby="s-notifications">
                    <h2 id="s-notifications" className="title text-xl mb-2">{t("settings.notifications")}</h2>
                    {prefs.isLoading && <div className="space-y-3"><Skeleton className="h-10" /><Skeleton className="h-10" /><Skeleton className="h-10" /></div>}
                    {prefs.isError && <p className="notice notice--danger">{errorMessage(prefs.error, t("common.error"))}</p>}
                    {p && (
                        <div className="divide-y divide-dashed divide-line">
                            <Toggle id="pref-inapp" label={t("settings.inApp")} hint={t("settings.inAppHint")}
                                    checked={p.inApp} disabled={savePrefs.isPending} onChange={(v) => save({ inApp: v })} />
                            <Toggle id="pref-email" label={t("settings.email")} hint={t("settings.emailHint")}
                                    checked={p.email} disabled={savePrefs.isPending} onChange={(v) => save({ email: v })} />
                            <Toggle id="pref-push" label={t("settings.push")}
                                    hint={p.pushAvailable ? t("settings.pushHint") : t("settings.pushUnavailable")}
                                    checked={p.push && p.pushAvailable} disabled={!p.pushAvailable || savePrefs.isPending}
                                    onChange={togglePush} />
                        </div>
                    )}
                </section>

                <section className="glass p-6 space-y-3" aria-labelledby="s-calendar">
                    <h2 id="s-calendar" className="title text-xl">{t("settings.calendar")}</h2>
                    <p className="muted text-sm leading-relaxed">{t("settings.calendarHint")}</p>
                    {!feedUrl && (
                        <button type="button" className="btn btn-primary" onClick={() => calendar.refetch()} disabled={calendar.isFetching}>
                            {calendar.isFetching ? t("common.loading") : t("settings.calendarCreate")}
                        </button>
                    )}
                    {calendar.isError && <p className="notice notice--danger">{errorMessage(calendar.error, t("common.error"))}</p>}
                    {feedUrl && (
                        <>
                            <label htmlFor="feed-url" className="sr-only">{t("settings.calendar")}</label>
                            <input id="feed-url" readOnly value={feedUrl} dir="ltr" className="field num !text-xs" onFocus={(e) => e.target.select()} />
                            <div className="flex flex-wrap gap-2">
                                <button type="button" className="btn btn-ghost btn-sm" onClick={copyFeed} aria-live="polite">
                                    {copied ? t("common.copied") : t("common.copy")}
                                </button>
                                <a className="btn btn-ghost btn-sm" href={feedUrl} download="learning-calendar.ics">{t("settings.calendarDownload")}</a>
                                <button type="button" className="btn btn-ghost btn-sm !text-danger" onClick={() => regenerate.mutate()} disabled={regenerate.isPending}>
                                    {t("settings.calendarRegenerate")}
                                </button>
                            </div>
                            <p className="faint text-xs">{t("settings.calendarWarning")}</p>
                        </>
                    )}
                </section>

                <section className="glass p-6 space-y-3" aria-labelledby="s-language">
                    <h2 id="s-language" className="title text-xl">{t("settings.language")}</h2>
                    <LanguageSwitcher />
                </section>

                <section className="glass p-6 space-y-3" aria-labelledby="s-tour">
                    <h2 id="s-tour" className="title text-xl">{t("settings.tour")}</h2>
                    <button type="button" className="btn btn-ghost" onClick={() => window.dispatchEvent(new Event(START_TOUR_EVENT))}>
                        {t("settings.tourRestart")}
                    </button>
                    {user.role === "STUDENT" && (
                        <div className="pt-3 border-t border-dashed border-line space-y-2">
                            <h3 className="title text-base">{t("settings.offline")}</h3>
                            <p className="muted text-sm">{t("settings.offlinePending", { count: pending })}</p>
                            {pending > 0 && (
                                <button type="button" className="btn btn-ghost btn-sm" onClick={async () => {
                                    const { sent, correct } = await syncAnswers(submitQueuedAnswer);
                                    setStatus(t("offline.synced", { count: sent, correct }));
                                    setPending((await pendingAnswers()).length);
                                }}>
                                    {t("settings.offlineSyncNow")}
                                </button>
                            )}
                        </div>
                    )}
                </section>
            </div>
        </div>
    );
}
