import { FaComments } from "react-icons/fa";
import ClassForumBoard from "../../features/classCommunity/ClassForumBoard.jsx";

// [s02] The class forum for students: ask about an exercise, answer classmates, see teacher-approved answers.
export default function ClassForum() {
    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-2 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge" aria-hidden="true"><FaComments /></span>
                    <div>
                        <span className="eyebrow">Class forum · פורום הכיתה</span>
                        <h2 className="display text-3xl sm:text-4xl">שואלים, עונים, לומדים יחד</h2>
                    </div>
                </div>
                <p className="muted">
                    נתקעתם בתרגיל? חפשו קודם – אולי מישהו כבר שאל. תשובות שהמורה סימן/ה כנכונות מסומנות ב־✓.
                </p>
            </div>
            <ClassForumBoard />
        </div>
    );
}
