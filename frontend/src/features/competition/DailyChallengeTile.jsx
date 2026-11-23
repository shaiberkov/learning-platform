import { useQuery } from "@tanstack/react-query";
import { FaCalendarCheck } from "react-icons/fa";
import { QuickTile } from "../../components/ui/DashboardHero.jsx";
import { apiGet } from "../../lib/api.js";
import { DAILY_CHALLENGE_BASE_PATH, DAILY_CHALLENGE_TODAY } from "../../constants/pages.constants.js";
import { DAILY_CHALLENGE_QUERY_KEY, streakLabel } from "./competitionHelpers.js";

/** [s03] Dashboard tile for the daily challenge: whether today's is still open, and the streak. */
function DailyChallengeTile({ index = 0 }) {
    const { data } = useQuery({
        queryKey: DAILY_CHALLENGE_QUERY_KEY,
        queryFn: () => apiGet(`${DAILY_CHALLENGE_BASE_PATH}${DAILY_CHALLENGE_TODAY}`),
    });

    const open = data && (data.status === "NOT_STARTED" || data.status === "IN_PROGRESS");
    const hint = !data ? "שאלה אחת ביום" : `${open ? "מחכה לך" : "הושלם"} · ${streakLabel(data.streak)}`;
    return <QuickTile index={index} to="/daily-challenge" icon={<FaCalendarCheck />} label="אתגר יומי" hint={hint} tone="icon-badge--mint" />;
}

export default DailyChallengeTile;
