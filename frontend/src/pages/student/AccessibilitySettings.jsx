import { useContext, useState } from "react";
import { FaUniversalAccess, FaClock } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import { apiPost, errorText } from "../../Utils/authApi.js";
import { applyDyslexiaFont, canSpeak, speak, useAccommodations } from "../../Utils/accessibility.js";
import { ACCOMMODATION_BASE_PATH, ACCOMMODATION_SET_PREFERENCES } from "../../constants/pages.constants.js";

// The student's own accessibility choices. Extra exam time is set by the teacher and shown here.
export default function AccessibilitySettings() {
    const { user } = useContext(UserContext);
    const { accommodations, setAccommodations, loaded } = useAccommodations(user);
    const [message, setMessage] = useState(null);

    const save = async (next) => {
        setAccommodations(next);
        applyDyslexiaFont(next.dyslexiaFont);
        try {
            const res = await apiPost(`${ACCOMMODATION_BASE_PATH}${ACCOMMODATION_SET_PREFERENCES}`, {
                studentId: user.userId, readAloud: next.readAloud, dyslexiaFont: next.dyslexiaFont,
            });
            setMessage({ ok: res.success, text: res.errorCode });
        } catch (err) {
            setMessage({ ok: false, text: errorText(err) });
        }
    };

    return (
        <div className="page page--narrow" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-6 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge"><FaUniversalAccess /></span>
                    <div>
                        <span className="eyebrow">Accessibility · נגישות</span>
                        <h2 className="title text-2xl sm:text-3xl">התאמות למידה</h2>
                    </div>
                </div>

                <label className="row-card flex items-center justify-between gap-4 cursor-pointer">
                    <span>
                        <span className="block font-semibold text-ink">גופן ידידותי לדיסלקציה</span>
                        <span className="muted text-sm">גופן פשוט, מרווחים גדולים בין אותיות, מילים ושורות</span>
                    </span>
                    <input type="checkbox" className="w-5 h-5 accent-[var(--accent)]" disabled={!loaded}
                           checked={accommodations.dyslexiaFont}
                           onChange={(e) => save({ ...accommodations, dyslexiaFont: e.target.checked })} />
                </label>

                <label className="row-card flex items-center justify-between gap-4 cursor-pointer">
                    <span>
                        <span className="block font-semibold text-ink">הקראת שאלות בקול</span>
                        <span className="muted text-sm">כפתור רמקול ליד כל שאלה במבחן ובשיעורים</span>
                    </span>
                    <input type="checkbox" className="w-5 h-5 accent-[var(--accent)]" disabled={!loaded}
                           checked={accommodations.readAloud}
                           onChange={(e) => save({ ...accommodations, readAloud: e.target.checked })} />
                </label>
                {accommodations.readAloud && (
                    canSpeak() ? (
                        <button type="button" className="btn btn-ghost btn-sm" onClick={() => speak("שלום! כך תשמע הקראת השאלות. שלוש ועוד ארבע שווה שבע.")}>
                            בדיקת הקראה
                        </button>
                    ) : (
                        <div className="notice notice--danger">הדפדפן הזה לא תומך בהקראה. נסה דפדפן Chrome או Edge.</div>
                    )
                )}

                <div className="row-card flex items-center gap-4">
                    <span className="icon-badge icon-badge--mint"><FaClock /></span>
                    <span>
                        <span className="block font-semibold text-ink">תוספת זמן במבחנים</span>
                        <span className="muted text-sm">
                            {accommodations.extraTimePercent > 0
                                ? <>המורה קבע/ה לך <span className="num">{accommodations.extraTimePercent}%</span> זמן נוסף. התוספת מחושבת אוטומטית בכל מבחן.</>
                                : "לא הוגדרה תוספת זמן. המורה יכול/ה להגדיר אותה עבורך."}
                        </span>
                    </span>
                </div>

                {message && <div className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</div>}
            </div>
        </div>
    );
}
