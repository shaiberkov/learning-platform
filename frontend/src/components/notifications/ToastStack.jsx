import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useNotificationContext } from "../../context/NotificationContext.jsx";

// Live pop-ups for incoming notifications (shown only when the user keeps in-app alerts on).
export default function ToastStack() {
    const { toasts, dismissToast } = useNotificationContext();
    const navigate = useNavigate();
    const { t, i18n } = useTranslation();

    return (
        <div className="toast-stack" role="region" aria-live="polite" aria-label={t("notifications.title")} dir={i18n.dir()}>
            {toasts.map((toast) => (
                <div key={toast.id} className="glass glass--strong p-4 flex items-start gap-3 shadow-[var(--shadow-2)]"
                     style={{ animation: "rise .5s var(--ease-out-expo) both" }}>
                    <span aria-hidden="true" className="icon-badge !w-9 !h-9 !rounded-xl shrink-0">
                        <span className="w-2.5 h-2.5 rotate-45 rounded-[3px] bg-current" />
                    </span>
                    <button
                        type="button"
                        className="flex-1 text-start min-w-0"
                        onClick={() => {
                            dismissToast(toast.id);
                            if (toast.link) navigate(toast.link);
                        }}
                    >
                        <p className="title text-sm">
                            {toast.missedCount ? t("notifications.missed", { count: toast.missedCount }) : toast.title}
                        </p>
                        {toast.body && <p className="muted text-xs mt-1 line-clamp-2 break-words">{toast.body}</p>}
                    </button>
                    <button
                        type="button"
                        onClick={() => dismissToast(toast.id)}
                        className="grid place-items-center w-8 h-8 rounded-lg text-ink-3 hover:text-ink"
                        aria-label={t("common.close")}
                    >
                        <span aria-hidden="true">✕</span>
                    </button>
                </div>
            ))}
        </div>
    );
}
