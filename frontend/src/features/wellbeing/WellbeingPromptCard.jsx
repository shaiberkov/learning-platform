import { useContext } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { FaFeatherAlt, FaHeart } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import { apiGet } from "../../lib/api.js";
import { WELLBEING_BASE_PATH, WELLBEING_PROMPT } from "../../constants/pages.constants.js";

// [s10] Dashboard card: the Sunday reflection (until written) and a check-in before an upcoming exam.
export default function WellbeingPromptCard() {
    const { user } = useContext(UserContext);
    const { data } = useQuery({
        queryKey: ["wellbeing-prompt", user?.userId],
        queryFn: () => apiGet(`${WELLBEING_BASE_PATH}${WELLBEING_PROMPT}`, undefined),
        enabled: !!user?.userId,
        refetchInterval: 5 * 60 * 1000,
    });

    if (!data) return null;
    const showExam = data.upcomingExam && !data.checkedInToday;
    if (!data.reflectionDue && !showExam) return null;

    return (
        <div className="glass p-5 space-y-3 reveal" style={{ "--i": 10 }} dir="rtl">
            {data.reflectionDue && (
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                        <span className="icon-badge" aria-hidden="true"><FaFeatherAlt /></span>
                        <div>
                            <p className="title text-base">{data.sunday ? "יום ראשון: זמן לרפלקציה" : "עוד לא כתבת רפלקציה השבוע"}</p>
                            <p className="muted text-sm">מה הלך טוב? במה נתקעת? מה היעד לשבוע?</p>
                        </div>
                    </div>
                    <Link to="/reflection" className="btn btn-primary btn-sm">לכתיבה</Link>
                </div>
            )}
            {showExam && (
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                        <span className="icon-badge icon-badge--mint" aria-hidden="true"><FaHeart /></span>
                        <div>
                            <p className="title text-base">
                                מבחן ב{data.upcomingExam.subject}{" "}
                                {data.upcomingExam.daysLeft === 0 ? "היום" : <>בעוד <span className="num">{data.upcomingExam.daysLeft}</span> ימים</>}
                            </p>
                            <p className="muted text-sm">איך את/ה מרגיש/ה לקראתו? דקה של צ'ק-אין.</p>
                        </div>
                    </div>
                    <Link to="/exam-checkin" className="btn btn-mint btn-sm">לצ'ק-אין</Link>
                </div>
            )}
        </div>
    );
}
