import { useEffect, useState } from "react";
import { FaBookOpen } from "react-icons/fa";
import { QuickTile } from "../../components/ui/DashboardHero.jsx";
import { fetchReviewSummary } from "./adaptiveApi.js";

/** Dashboard tile for the mistakes notebook, showing how many items are due today. */
function ReviewDueTile({ userId, index = 0 }) {
    const [dueToday, setDueToday] = useState(null);

    useEffect(() => {
        if (!userId) return;
        (async () => {
            const res = await fetchReviewSummary(userId);
            if (res.success) setDueToday(res.data.dueToday);
        })();
    }, [userId]);

    const hint = dueToday === null ? "חזרה מרווחת" : dueToday > 0 ? `${dueToday} לחזרה היום` : "אין חזרות להיום";
    return <QuickTile index={index} to="/mistakes-notebook" icon={<FaBookOpen />} label="מחברת טעויות" hint={hint} tone="icon-badge--amber" />;
}

export default ReviewDueTile;
