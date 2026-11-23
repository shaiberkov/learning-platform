import { useContext, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaFrown, FaGrimace, FaGrinBeam, FaHeart, FaSmile, FaTired, FaUserShield } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import { apiGet, apiPost, errorMessage } from "../../lib/api.js";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import EmptyState from "../../components/ui/EmptyState.jsx";
import BreathingExercise from "../../features/wellbeing/BreathingExercise.jsx";
import {
    WELLBEING_BASE_PATH, CHECKIN_STATUS, CHECKIN_SUBMIT, CHECKIN_CONSENT,
} from "../../constants/pages.constants.js";
import { MOODS, moodLabel, shortDate } from "../../features/wellbeing/wellbeing.js";

// [s10] Emotional check-in before an exam, with a breathing exercise or a coping tip, and the
// student's explicit opt-in for an alert to the school staff on a repeated distress pattern.

const MOOD_ICONS = { GREAT: FaGrinBeam, OK: FaSmile, NERVOUS: FaGrimace, STRESSED: FaFrown, OVERWHELMED: FaTired };

function formatDateTime(value) {
    if (!value) return "";
    const d = new Date(value);
    return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleString("he-IL", { dateStyle: "short", timeStyle: "short" });
}

function PrivacyAndConsent({ consent, onChange, busy }) {
    return (
        <div className="glass p-6 space-y-4 reveal" style={{ "--i": 3 }}>
            <div className="flex items-center gap-3">
                <span className="icon-badge" aria-hidden="true"><FaUserShield /></span>
                <h2 className="title text-xl">פרטיות והסכמה</h2>
            </div>
            <div className="space-y-2 text-sm muted leading-relaxed">
                <p><strong className="text-ink">מה נשמר:</strong> התשובה שבחרת לשאלה "איך אני מרגיש/ה?", התאריך והמבחן הקרוב. לא נשמר טקסט חופשי.</p>
                <p><strong className="text-ink">מי רואה:</strong> רק את/ה. המורים לא רואים את הצ'ק-אינים שלך.</p>
                <p>
                    <strong className="text-ink">מתי הצוות מקבל הודעה:</strong> רק אם הסכמת כאן, ורק אם סימנת לחץ או מצוקה
                    ב-3 מתוך 5 הצ'ק-אינים האחרונים. ההודעה נשלחת למנהל/ת בית הספר (שמפנה ליועצ/ת), ואומרת רק שכדאי לשוחח איתך.
                    התשובות עצמן לא נשלחות. לכל היותר הודעה אחת בשבועיים.
                </p>
                <p><strong className="text-ink">ביטול:</strong> אפשר לבטל את ההסכמה בכל רגע, והיא נשמרת עם תאריך ושעה.</p>
            </div>
            <label className="flex items-start gap-3 cursor-pointer">
                <input type="checkbox" className="mt-1" checked={!!consent?.consented} disabled={busy}
                       onChange={(e) => onChange(e.target.checked)} />
                <span className="text-ink text-sm">
                    אני מסכים/ה שאם אסמן מצוקה לאורך זמן, הצוות בבית הספר יקבל הודעה ויוכל לפנות אליי.
                </span>
            </label>
            {consent?.consented && consent.consentedAt && (
                <p className="faint text-xs">הסכמת ב-<span className="num">{formatDateTime(consent.consentedAt)}</span></p>
            )}
            {!consent?.consented && consent?.revokedAt && (
                <p className="faint text-xs">ההסכמה בוטלה ב-<span className="num">{formatDateTime(consent.revokedAt)}</span></p>
            )}
        </div>
    );
}

export default function ExamCheckin() {
    const { user } = useContext(UserContext);
    const queryClient = useQueryClient();
    const statusKey = ["checkin-status", user?.userId];
    const [mood, setMood] = useState(null);
    const [result, setResult] = useState(null);

    const status = useQuery({
        queryKey: statusKey,
        queryFn: () => apiGet(`${WELLBEING_BASE_PATH}${CHECKIN_STATUS}`, undefined),
        enabled: !!user?.userId,
    });

    const refresh = () => {
        queryClient.invalidateQueries({ queryKey: statusKey });
        queryClient.invalidateQueries({ queryKey: ["wellbeing-prompt", user?.userId] });
    };

    const submit = useMutation({
        mutationFn: () => apiPost(`${WELLBEING_BASE_PATH}${CHECKIN_SUBMIT}`, undefined, { mood }),
        onSuccess: (res) => {
            setResult(res.data);
            refresh();
        },
    });

    const consent = useMutation({
        mutationFn: (value) => apiPost(`${WELLBEING_BASE_PATH}${CHECKIN_CONSENT}`, { consent: value }),
        onSuccess: refresh,
    });

    const data = status.data;
    const exam = data?.upcomingExam;

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-2 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge icon-badge--mint" aria-hidden="true"><FaHeart /></span>
                    <div>
                        <span className="eyebrow">Check-in · צ'ק-אין לפני מבחן</span>
                        <h1 className="display text-3xl sm:text-4xl">איך אני מרגיש/ה?</h1>
                    </div>
                </div>
                {exam ? (
                    <p className="muted text-sm">
                        מבחן ב{exam.subject} ({exam.topic}){" "}
                        {exam.daysLeft === 0 ? "היום" : <>בעוד <span className="num">{exam.daysLeft}</span> ימים</>}
                        {exam.startTime && <> · <span className="num">{exam.startTime}</span></>}
                    </p>
                ) : (
                    <p className="muted text-sm">אין מבחן בימים הקרובים, אבל תמיד אפשר לעצור לרגע ולבדוק איך את/ה מרגיש/ה.</p>
                )}
            </div>

            {status.isLoading && <SkeletonList rows={2} />}
            {status.isError && <div className="notice notice--danger">{errorMessage(status.error, "הטעינה נכשלה")}</div>}

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
                <div className="lg:col-span-7 space-y-5">
                    <div className="glass p-6 space-y-4 reveal" style={{ "--i": 1 }}>
                        <fieldset className="space-y-3">
                            <legend className="title text-xl">בחר/י את מה שהכי מתאים עכשיו</legend>
                            <div className="grid grid-cols-2 sm:grid-cols-5 gap-2">
                                {MOODS.map((m) => {
                                    const Icon = MOOD_ICONS[m.value];
                                    return (
                                        <button key={m.value} type="button" aria-pressed={mood === m.value}
                                                className={`choice flex flex-col items-center gap-2 py-4 ${mood === m.value ? "is-active" : ""}`}
                                                onClick={() => setMood(m.value)}>
                                            <Icon aria-hidden="true" className="text-2xl" />
                                            <span className="text-sm">{m.label}</span>
                                        </button>
                                    );
                                })}
                            </div>
                        </fieldset>
                        {submit.isError && <div className="notice notice--danger">{errorMessage(submit.error, submit.error?.userMessage || "השליחה נכשלה")}</div>}
                        <button type="button" className="btn btn-primary" disabled={!mood || submit.isPending} onClick={() => submit.mutate()}>
                            שליחת הצ'ק-אין
                        </button>
                    </div>

                    {result && (
                        <div className="glass p-6 space-y-4 reveal" aria-live="polite">
                            <h2 className="title text-xl">{result.suggestion.title}</h2>
                            <p className="text-ink leading-relaxed">{result.suggestion.text}</p>
                            {result.suggestion.kind === "BREATHING" && <BreathingExercise />}
                            {result.alertSent && (
                                <div className="notice notice--mint">
                                    כפי שהסכמת, הודענו לצוות בית הספר שכדאי לשוחח איתך. התשובות שלך עצמן לא נשלחו.
                                </div>
                            )}
                            {result.distressPattern && !result.consented && (
                                <div className="notice">
                                    נראה שבתקופה האחרונה קשה לך לקראת מבחנים. זה בסדר לבקש עזרה: אפשר לפנות ליועצת, למחנך/ת או להורה.
                                    אם תרצה/י, אפשר לאשר למטה שהצוות יקבל הודעה ויפנה אליך.
                                </div>
                            )}
                            {result.distressPattern && (
                                <p className="faint text-xs">
                                    אם קשה מאוד עכשיו, אפשר לדבר בכל שעה עם ער"ן: <span className="num">1201</span>.
                                </p>
                            )}
                        </div>
                    )}
                </div>

                <div className="lg:col-span-5 space-y-5">
                    {data && (
                        <PrivacyAndConsent consent={data.consent} busy={consent.isPending} onChange={(v) => consent.mutate(v)} />
                    )}
                    {consent.isError && <div className="notice notice--danger">{errorMessage(consent.error, "העדכון נכשל")}</div>}
                    {data && (
                        <div className="glass p-6 space-y-3 reveal" style={{ "--i": 4 }}>
                            <h2 className="title text-xl">הצ'ק-אינים שלי</h2>
                            {data.recent.length === 0 ? (
                                <EmptyState title="עוד אין צ'ק-אינים" body="הצ'ק-אין הראשון שלך יופיע כאן. רק את/ה רואה את הרשימה." />
                            ) : (
                                <ul className="space-y-2">
                                    {data.recent.map((c) => (
                                        <li key={c.date} className="row-card flex items-center justify-between gap-2">
                                            <span className="num faint text-xs">{shortDate(c.date)}</span>
                                            <span className="text-ink">{moodLabel(c.mood)}</span>
                                            <span className="muted text-xs truncate">{c.examLabel ?? ""}</span>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
