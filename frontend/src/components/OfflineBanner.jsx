import { useContext, useEffect } from "react";
import { useTranslation } from "react-i18next";
import useOnlineStatus from "../offline/useOnlineStatus.js";
import { syncAnswers } from "../offline/practiceStore.js";
import { UserContext } from "../context/UserContext.jsx";
import { useNotificationContext } from "../context/NotificationContext.jsx";
import { submitQueuedAnswer } from "../offline/submitQueuedAnswer.js";

// Shows an offline notice and, when the connection returns, syncs the practice answers
// that were queued offline (also once at start-up, for answers left from a closed tab).
export default function OfflineBanner() {
    const online = useOnlineStatus();
    const { user } = useContext(UserContext);
    const { pushToast } = useNotificationContext();
    const { t } = useTranslation();

    useEffect(() => {
        if (!online || user?.role !== "STUDENT") return;
        syncAnswers(submitQueuedAnswer).then(({ sent, correct }) => {
            if (sent > 0) pushToast({ title: t("offline.synced", { count: sent, correct }) });
        }).catch(() => {});
    }, [online, user?.role, pushToast, t]);

    if (online) return null;
    return <div className="offline-banner" role="status">{t("offline.banner")}</div>;
}
