import { useContext, useEffect, useState } from "react";
import { FaAward, FaFire, FaUsers, FaChartLine } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import ProgressRing from "../../components/ui/ProgressRing.jsx";
import OrbitLoader from "../../components/ui/OrbitLoader.jsx";
import { apiGet, errorText, formatShortDate } from "../../Utils/authApi.js";
import { ENGAGEMENT_BASE_PATH, ENGAGEMENT_GET_SUMMARY } from "../../constants/pages.constants.js";

const CHALLENGE_STATUS = { ACTIVE: "פעיל", UPCOMING: "בקרוב", ENDED: "הסתיים" };

// The student's own progress: level, streak, XP this week vs. last week, badges and the class's
// shared challenges. Nothing here compares the student to classmates.
export default function Achievements() {
    const { user } = useContext(UserContext);
    const [summary, setSummary] = useState(null);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!user?.userId) return;
        apiGet(`${ENGAGEMENT_BASE_PATH}${ENGAGEMENT_GET_SUMMARY}`, { studentId: user.userId })
            .then((res) => (res.success ? setSummary(res.data) : setError(res.errorCode)))
            .catch((err) => setError(errorText(err)));
    }, [user?.userId]);

    if (error) return <div className="page page--medium" dir="rtl"><div className="notice notice--danger">{error}</div></div>;
    if (!summary) return <OrbitLoader title="טוען את ההתקדמות שלך..." />;

    const levelPercent = Math.round((summary.xpIntoLevel / summary.xpPerLevel) * 100);
    const maxDay = Math.max(1, ...summary.lastDays.map((d) => d.xp));

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 sm:gap-5">
                <div className="lg:col-span-5 glass p-6 sm:p-8 flex items-center gap-6 reveal">
                    <ProgressRing value={levelPercent} size={132} stroke={11}>
                        <span className="grid text-center">
                            <span className="num text-3xl font-semibold text-ink">{summary.level}</span>
                            <span className="faint text-xs">רמה</span>
                        </span>
                    </ProgressRing>
                    <div className="space-y-2">
                        <span className="eyebrow">Progress · ההתקדמות שלי</span>
                        <h2 className="display text-3xl sm:text-4xl">
                            <span className="num">{summary.totalXp}</span> נקודות
                        </h2>
                        <p className="muted text-sm">
                            עוד <span className="num">{summary.xpPerLevel - summary.xpIntoLevel}</span> נקודות לרמה הבאה
                        </p>
                    </div>
                </div>

                <div className="lg:col-span-3 glass tile p-6 sm:p-8 space-y-3 reveal" style={{ "--i": 1 }}>
                    <span className="icon-badge icon-badge--amber"><FaFire /></span>
                    <div className="num text-5xl font-semibold text-ink">{summary.currentStreak}</div>
                    <p className="muted text-sm">
                        {summary.currentStreak === 1 ? "יום ברצף" : "ימים ברצף"}
                        {!summary.activeToday && summary.currentStreak > 0 && " – תרגול אחד היום ישמור על הרצף"}
                    </p>
                    <p className="faint text-xs">השיא שלך: <span className="num">{summary.longestStreak}</span></p>
                </div>

                <div className="lg:col-span-4 glass p-6 sm:p-8 space-y-4 reveal" style={{ "--i": 2 }}>
                    <div className="flex items-center gap-3">
                        <span className="icon-badge icon-badge--mint"><FaChartLine /></span>
                        <div>
                            <span className="eyebrow">השבוע שלי</span>
                            <p className="text-ink font-semibold">{summary.progressMessage}</p>
                        </div>
                    </div>
                    <div className="flex items-end gap-2 h-24" dir="ltr" aria-label="נקודות בשבעת הימים האחרונים">
                        {summary.lastDays.map((d) => (
                            <div key={d.date} className="flex-1 flex flex-col items-center gap-1">
                                <div
                                    className="w-full rounded-md bg-accent"
                                    style={{ height: `${Math.max(4, (d.xp / maxDay) * 80)}px`, opacity: d.xp ? 1 : 0.25 }}
                                    title={`${formatShortDate(d.date)}: ${d.xp}`}
                                />
                                <span className="num faint text-[10px]">{formatShortDate(d.date)}</span>
                            </div>
                        ))}
                    </div>
                    <p className="faint text-xs">
                        השבוע: <span className="num">{summary.xpThisWeek}</span> · שבוע שעבר: <span className="num">{summary.xpLastWeek}</span>.
                        ההשוואה היא רק לעצמך.
                    </p>
                </div>
            </div>

            <div className="glass p-6 sm:p-8 space-y-5 reveal" style={{ "--i": 3 }}>
                <div className="flex items-center gap-3">
                    <span className="icon-badge"><FaAward /></span>
                    <h3 className="title text-2xl">התגים שלי</h3>
                    <span className="chip num">{summary.badges.length}</span>
                </div>
                {summary.badges.length === 0 ? (
                    <p className="muted">עוד אין תגים. תגים ניתנים על הישגים אמיתיים: רצף ימים, שליטה בנושא, מבחן מושלם ועוד.</p>
                ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
                        {summary.badges.map((b) => (
                            <div key={b.code} className="row-card space-y-1">
                                <div className="font-semibold text-ink">{b.title}</div>
                                <div className="muted text-sm">{b.description}</div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            <div className="glass p-6 sm:p-8 space-y-5 reveal" style={{ "--i": 4 }}>
                <div className="flex items-center gap-3">
                    <span className="icon-badge icon-badge--mint"><FaUsers /></span>
                    <h3 className="title text-2xl">אתגרי הכיתה</h3>
                </div>
                {summary.classChallenges.length === 0 ? (
                    <p className="muted">אין כרגע אתגר כיתתי. המורה יכול/ה לפתוח אתגר משותף לכל הכיתה.</p>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {summary.classChallenges.map((c) => (
                            <div key={c.id} className="row-card space-y-3">
                                <div className="flex items-center justify-between gap-2">
                                    <span className="font-semibold text-ink">{c.title}</span>
                                    <span className={`chip ${c.status === "ACTIVE" ? "chip--mint" : ""}`}>{CHALLENGE_STATUS[c.status]}</span>
                                </div>
                                <div className="track"><span style={{ width: `${c.percent}%` }} /></div>
                                <p className="faint text-xs">
                                    הכיתה צברה יחד <span className="num">{c.classXp}</span> מתוך <span className="num">{c.targetXp}</span> נקודות
                                    · <span className="num">{formatShortDate(c.startDate)}–{formatShortDate(c.endDate)}</span>
                                </p>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
