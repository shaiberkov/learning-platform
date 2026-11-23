import { FaComments } from "react-icons/fa";
import ClassForumBoard from "../../features/classCommunity/ClassForumBoard.jsx";

// [s02] The class forum for the teacher: answer, approve correct answers (✓) and remove posts.
export default function ClassForumModeration() {
    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-2 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge" aria-hidden="true"><FaComments /></span>
                    <div>
                        <span className="eyebrow">Class forum · פורום הכיתה</span>
                        <h2 className="display text-3xl sm:text-4xl">ניהול פורום הכיתה</h2>
                    </div>
                </div>
                <p className="muted">תשובה שתסמנו כמאושרת תופיע ראשונה, ותלמידים שייתקעו באותה שאלה יראו אותה מיד.</p>
            </div>
            <ClassForumBoard moderator />
        </div>
    );
}
