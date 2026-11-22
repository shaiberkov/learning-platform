import AiPolicyNotice from "../../components/ai/AiPolicyNotice.jsx";
import { AiUsagePanel, FlaggedMessagesPanel } from "../../components/ai/AiOversightPanels.jsx";

// School manager: AI cost for the school and the safety-flagged tutor messages.
export default function AiSchoolOverview() {
    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="reveal">
                <span className="eyebrow">AI · בית הספר</span>
                <h2 className="display text-3xl sm:text-4xl mt-1">AI בבית הספר</h2>
            </div>
            <AiPolicyNotice audience="staff" />
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
                <AiUsagePanel />
                <FlaggedMessagesPanel />
            </div>
        </div>
    );
}
