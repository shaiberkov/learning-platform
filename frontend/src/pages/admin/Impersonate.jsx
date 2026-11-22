import {useState} from "react";
import axios from "axios";
import Cookies from "universal-cookie";
import {FaIdCard, FaUserSecret} from "react-icons/fa";
import FormShell from "../../components/ui/FormShell.jsx";
import {IMPERSONATION_BASE_PATH, START_IMPERSONATION} from "../../constants/pages.constants.js";

const DASHBOARD_BY_ROLE = {
    SCHOOLMANAGER: "/school-mananger-dashboard",
    TEACHER: "/teacher-dashboard",
    STUDENT: "/student-dashboard",
};

// Support tool: act as another user. The server issues a 30-minute token and audits start and stop.
function Impersonate() {
    const [targetUserId, setTargetUserId] = useState("");
    const [message, setMessage] = useState("");
    const [busy, setBusy] = useState(false);

    const start = async () => {
        setMessage("");
        setBusy(true);
        try {
            const response = await axios.post(
                `${IMPERSONATION_BASE_PATH}${START_IMPERSONATION}`,
                null,
                {params: {targetUserId: targetUserId.trim()}}
            );
            if (response.data.success && response.data.data?.token) {
                new Cookies().set("token", response.data.data.token, {path: "/"});
                window.location.assign(DASHBOARD_BY_ROLE[response.data.data.role] || "/");
                return;
            }
            setMessage(response.data.errorCode || "לא ניתן להתחיל התחזות");
        } catch (error) {
            setMessage(error.response?.data?.errorCode || "שגיאה בעת שליחת הבקשה");
        } finally {
            setBusy(false);
        }
    };

    return (
        <FormShell eyebrow="Support · תמיכה" title="התחזות למשתמש" icon={<FaUserSecret/>} tone="icon-badge--amber">
            <p className="text-sm text-ink-2 leading-relaxed">
                ההתחזות מאפשרת לראות את המערכת בדיוק כפי שהמשתמש רואה אותה, לצורך תמיכה.
                היא נמשכת עד 30 דקות, מוצג באנר לאורך כל הזמן, ותחילתה וסיומה נרשמים ביומן הפעולות.
            </p>
            <div className="field-wrap">
                <FaIdCard/>
                <input
                    type="text"
                    value={targetUserId}
                    onChange={(e) => setTargetUserId(e.target.value)}
                    placeholder="תעודת זהות של המשתמש"
                    className="field num"
                />
            </div>
            <button
                onClick={start}
                disabled={busy || targetUserId.trim().length === 0}
                className="btn btn-primary btn-block"
            >
                {busy ? "מתחיל..." : "התחל התחזות"}
            </button>
            {message && <p className="notice notice--danger">{message}</p>}
        </FormShell>
    );
}

export default Impersonate;
