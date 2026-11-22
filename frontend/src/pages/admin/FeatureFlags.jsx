import {useEffect, useState} from "react";
import axios from "axios";
import {FaSchool, FaToggleOff, FaToggleOn} from "react-icons/fa";
import FormShell from "../../components/ui/FormShell.jsx";
import {
    FEATURE_FLAGS,
    GET_ALL_SCHOOLS,
    SET_FEATURE_FLAG,
    SYSTEM_ADMIN_BASE_PATH
} from "../../constants/pages.constants.js";

// Per-school feature flags: the server refuses a disabled feature for that school's users.
export default function FeatureFlags() {
    const [schools, setSchools] = useState([]);
    const [schoolCode, setSchoolCode] = useState("");
    const [flags, setFlags] = useState([]);
    const [message, setMessage] = useState("");
    const [busyFeature, setBusyFeature] = useState("");

    useEffect(() => {
        const loadSchools = async () => {
            try {
                const response = await axios.get(`${SYSTEM_ADMIN_BASE_PATH}${GET_ALL_SCHOOLS}`);
                if (response.data.success && Array.isArray(response.data.data)) {
                    setSchools(response.data.data);
                }
            } catch {
                setMessage("אירעה שגיאה בטעינת בתי הספר");
            }
        };
        loadSchools();
    }, []);

    const loadFlags = async (code) => {
        setFlags([]);
        if (!code) return;
        try {
            const response = await axios.get(`${SYSTEM_ADMIN_BASE_PATH}${FEATURE_FLAGS}`, {params: {schoolCode: code}});
            if (response.data.success) {
                setFlags(response.data.data || []);
            } else {
                setMessage(response.data.errorCode);
            }
        } catch (e) {
            setMessage(e.response?.data?.errorCode || "אירעה שגיאה בטעינת ההגדרות");
        }
    };

    const selectSchool = (code) => {
        setSchoolCode(code);
        setMessage("");
        loadFlags(code);
    };

    const toggle = async (flag) => {
        setBusyFeature(flag.feature);
        setMessage("");
        try {
            const response = await axios.post(`${SYSTEM_ADMIN_BASE_PATH}${SET_FEATURE_FLAG}`, null, {
                params: {schoolCode, feature: flag.feature, enabled: !flag.enabled},
            });
            setMessage(response.data.errorCode || "");
            if (response.data.success) {
                await loadFlags(schoolCode);
            }
        } catch (e) {
            setMessage(e.response?.data?.errorCode || "שגיאה בעת שמירת ההגדרה");
        } finally {
            setBusyFeature("");
        }
    };

    return (
        <FormShell eyebrow="Features · הגדרות" title="פיצ'רים לפי בית ספר" icon={<FaToggleOn/>} width="medium">
            <div>
                <label className="label"><FaSchool/> בית ספר</label>
                <select className="field" value={schoolCode} onChange={(e) => selectSchool(e.target.value)}>
                    <option value="">בחר/י בית ספר</option>
                    {schools.map((school) => (
                        <option key={school.schoolCode} value={school.schoolCode}>
                            {school.schoolName} ({school.schoolCode})
                        </option>
                    ))}
                </select>
            </div>

            {flags.length > 0 && (
                <ul className="space-y-3">
                    {flags.map((flag) => (
                        <li key={flag.feature} className="row-card flex items-center justify-between gap-3">
                            <div className="min-w-0">
                                <div className="font-semibold text-ink">{flag.label}</div>
                                <div className="text-ink-3 text-xs">
                                    ברירת מחדל: {flag.defaultEnabled ? "פעיל" : "כבוי"}
                                    {flag.updatedAt && ` · עודכן ${new Date(flag.updatedAt).toLocaleString("he-IL")}`}
                                </div>
                            </div>
                            <button
                                onClick={() => toggle(flag)}
                                disabled={busyFeature === flag.feature}
                                className={`btn ${flag.enabled ? "btn-mint" : "btn-ghost"}`}
                                aria-pressed={flag.enabled}
                            >
                                {flag.enabled ? <FaToggleOn/> : <FaToggleOff/>}
                                {flag.enabled ? "פעיל" : "כבוי"}
                            </button>
                        </li>
                    ))}
                </ul>
            )}

            {message && <p className="notice">{message}</p>}
        </FormShell>
    );
}
