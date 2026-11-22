import {useContext, useState} from "react";
import axios from "axios";
import Cookies from "universal-cookie";
import {FaUserSecret} from "react-icons/fa";
import {UserContext} from "../context/UserContext.jsx";
import {IMPERSONATION_BASE_PATH, STOP_IMPERSONATION} from "../constants/pages.constants.js";

const ROLE_LABELS = {
    SCHOOLMANAGER: "מנהל בית ספר",
    TEACHER: "מורה",
    STUDENT: "תלמיד",
};

// Shown on every screen while a system admin is acting as another user for support.
export default function ImpersonationBanner() {
    const {user} = useContext(UserContext);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");

    if (!user?.impersonatorId) return null;

    const stop = async () => {
        setBusy(true);
        setError("");
        const cookies = new Cookies();
        try {
            const response = await axios.post(`${IMPERSONATION_BASE_PATH}${STOP_IMPERSONATION}`);
            if (response.data.success && response.data.data?.token) {
                cookies.set("token", response.data.data.token, {path: "/"});
                window.location.assign("/system-admin-dashboard");
                return;
            }
            setError(response.data.errorCode || "לא ניתן לסיים את ההתחזות");
        } catch {
            // The impersonation token may have expired: sign out instead of staying in the user's session.
            cookies.remove("token", {path: "/"});
            window.location.assign("/login");
        } finally {
            setBusy(false);
        }
    };

    return (
        <div
            dir="rtl"
            role="status"
            className="fixed bottom-4 inset-x-4 z-[60] mx-auto max-w-3xl glass border-2 border-[var(--amber)] px-4 py-3 flex flex-wrap items-center gap-3"
        >
            <span className="icon-badge icon-badge--amber"><FaUserSecret/></span>
            <div className="flex-1 min-w-0 text-sm">
                <div className="font-semibold text-ink">
                    מצב התחזות: את/ה צופה במערכת בתור {user.username} ({ROLE_LABELS[user.role] || user.role})
                </div>
                <div className="text-ink-3">
                    הוחל על ידי {user.impersonatorUsername || user.impersonatorId}. כל פעולה נרשמת ביומן הפעולות.
                </div>
                {error && <div className="text-danger mt-1">{error}</div>}
            </div>
            <button onClick={stop} disabled={busy} className="btn btn-danger">
                {busy ? "מסיים..." : "סיום התחזות"}
            </button>
        </div>
    );
}
