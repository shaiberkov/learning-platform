import {useEffect, useState} from "react";
import axios from "axios";
import {FaDatabase, FaExclamationTriangle, FaHeartbeat, FaRobot, FaSyncAlt} from "react-icons/fa";
import {SYSTEM_ADMIN_BASE_PATH, SYSTEM_HEALTH} from "../../constants/pages.constants.js";

function formatUptime(seconds) {
    if (seconds == null) return "—";
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    return `${days} ימים, ${hours} שעות, ${minutes} דקות`;
}

function formatTime(value) {
    return value ? new Date(value).toLocaleString("he-IL") : "—";
}

function Stat({label, value, tone = ""}) {
    return (
        <div className="flex items-center justify-between gap-3 py-2 border-b border-dashed border-line last:border-0">
            <span className="text-ink-2 text-sm">{label}</span>
            <span className={`num font-semibold ${tone}`}>{value}</span>
        </div>
    );
}

function Card({icon, tone, title, children, index}) {
    return (
        <div className="glass p-5 reveal" style={{"--i": index}}>
            <div className="flex items-center gap-3 mb-3">
                <span className={`icon-badge ${tone}`}>{icon}</span>
                <h3 className="title text-lg">{title}</h3>
            </div>
            {children}
        </div>
    );
}

export default function SystemHealth() {
    const [health, setHealth] = useState(null);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const load = async () => {
        setLoading(true);
        setError("");
        try {
            const response = await axios.get(`${SYSTEM_ADMIN_BASE_PATH}${SYSTEM_HEALTH}`);
            if (response.data.success) {
                setHealth(response.data.data);
            } else {
                setError(response.data.errorCode || "לא ניתן לטעון את מצב המערכת");
            }
        } catch (e) {
            setError(e.response?.data?.errorCode || "אירעה שגיאה בחיבור לשרת");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        load();
    }, []);

    const db = health?.database || {};
    const requests = health?.requests || {};
    const ai = health?.ai || {};
    const memory = health?.memory || {};

    return (
        <div className="page page--wide" dir="rtl">
            <div className="flex flex-wrap items-end justify-between gap-3 mb-5 reveal">
                <div>
                    <span className="eyebrow">System · מצב המערכת</span>
                    <h2 className="display text-3xl mt-1">בריאות המערכת</h2>
                </div>
                <button onClick={load} disabled={loading} className="btn btn-ghost">
                    <FaSyncAlt/> {loading ? "טוען..." : "רענון"}
                </button>
            </div>

            {error && <p className="notice notice--danger mb-4">{error}</p>}

            {health && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-5">
                    <Card icon={<FaHeartbeat/>} tone="icon-badge--mint" title="שרת" index={1}>
                        <Stat label="זמן פעילות" value={formatUptime(health.uptimeSeconds)}/>
                        <Stat label="עלה בתאריך" value={formatTime(health.startedAt)}/>
                        <Stat label="משתמשים מחוברים" value={health.activeUsers}/>
                        <Stat label="זיכרון בשימוש" value={`${memory.usedMb} / ${memory.maxMb} MB`}/>
                        <Stat label="רשומות ביומן ב-24 השעות האחרונות" value={health.auditEntriesLast24h ?? "—"}/>
                    </Card>

                    <Card icon={<FaDatabase/>} tone={db.up ? "icon-badge--mint" : "icon-badge--danger"} title="מסד נתונים" index={2}>
                        <Stat label="מצב" value={db.up ? "תקין" : "לא זמין"} tone={db.up ? "text-mint" : "text-danger"}/>
                        <Stat label="זמן תגובה" value={db.latencyMs != null ? `${db.latencyMs} ms` : "—"}/>
                        <Stat label="סוג" value={db.product || db.error || "—"}/>
                    </Card>

                    <Card icon={<FaExclamationTriangle/>} tone={requests.serverErrors > 0 ? "icon-badge--danger" : "icon-badge--amber"} title="בקשות ושגיאות" index={3}>
                        <Stat label="סך הבקשות מאז העלייה" value={requests.total}/>
                        <Stat label="שגיאות שרת (5xx)" value={requests.serverErrors} tone={requests.serverErrors > 0 ? "text-danger" : ""}/>
                        <Stat label="שגיאות לקוח (4xx)" value={requests.clientErrors}/>
                        <Stat label="בקשות שנחסמו (401/403)" value={requests.denied}/>
                        <Stat label="שגיאת שרת אחרונה" value={requests.lastServerErrorPath ? `${requests.lastServerErrorPath} · ${formatTime(requests.lastServerErrorAt)}` : "—"}/>
                    </Card>

                    <Card icon={<FaRobot/>} tone={ai.configured ? "icon-badge--mint" : "icon-badge--amber"} title="שימוש ב-AI" index={4}>
                        <Stat label="מפתח API מוגדר" value={ai.configured ? "כן" : "לא"} tone={ai.configured ? "text-mint" : "text-danger"}/>
                        <Stat label="קריאות מאז העלייה" value={ai.requests}/>
                        <Stat label="קריאות שנכשלו" value={ai.failures} tone={ai.failures > 0 ? "text-danger" : ""}/>
                        <Stat label="קריאה אחרונה" value={formatTime(ai.lastRequestAt)}/>
                    </Card>
                </div>
            )}

            <p className="text-ink-3 text-xs mt-4">המונים נשמרים בזיכרון השרת ומתאפסים בכל הפעלה מחדש.</p>
        </div>
    );
}
