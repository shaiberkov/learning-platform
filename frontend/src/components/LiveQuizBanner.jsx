import { Link, useLocation } from "react-router-dom";
import { FaBolt } from "react-icons/fa";
import { useNotificationContext } from "../context/NotificationContext.jsx";

// Tells a student, wherever they are, that their teacher opened or advanced a live quiz.
export default function LiveQuizBanner() {
    const { liveQuiz } = useNotificationContext();
    const location = useLocation();
    if (!liveQuiz?.active || location.pathname === "/live-quiz") return null;
    return (
        <div className="page" dir="rtl">
            <Link to="/live-quiz" className="notice notice--mint mb-4 !justify-between">
                <span className="flex items-center gap-2">
                    <FaBolt /> {liveQuiz.teacherName} פתח/ה חידון בכיתה {liveQuiz.className}
                </span>
                <span className="btn btn-mint btn-sm">להצטרפות</span>
            </Link>
        </div>
    );
}
