import EmptyState from "../../components/ui/EmptyState.jsx";
import { trendMessage, withYearDeltas } from "./questPortfolio.helpers.js";

// [s09] Grades from year to year, used by the portfolio and its public share page.

const OUTCOME_LABELS = { PROMOTED: "עלה/תה כיתה", GRADUATED: "סיים/ה", STAYED: "נשאר/ה בשכבה" };

export default function YearsTimeline({ years }) {
    const rows = withYearDeltas(years);
    if (rows.length === 0) {
        return <EmptyState title="עוד אין היסטוריית ציונים" body="בסוף כל שנת לימודים נשמר כאן סיכום השנה, משכבה ז' ועד היום." />;
    }
    const trend = trendMessage(rows);
    return (
        <div className="space-y-3">
            {trend && <p className="text-ink font-semibold">{trend}</p>}
            <ol className="space-y-3" aria-label="ציונים לפי שנים">
                {rows.map((y, i) => (
                    <li key={`${y.yearName}-${i}`} className="row-card space-y-2">
                        <div className="flex flex-wrap items-center justify-between gap-2">
                            <span className="font-semibold text-ink">
                                כיתה {y.gradeName}{y.className ? ` (${y.className})` : ""} · {y.yearName}
                            </span>
                            <span className="flex items-center gap-2">
                                {y.current && <span className="chip chip--amber">בתהליך</span>}
                                {y.outcome && <span className="chip">{OUTCOME_LABELS[y.outcome] || y.outcome}</span>}
                            </span>
                        </div>
                        {typeof y.averageScore === "number" ? (
                            <>
                                <div className="track" role="img" aria-label={`ממוצע ${y.averageScore}`}>
                                    <span style={{ width: `${Math.min(100, Math.max(0, y.averageScore))}%` }} />
                                </div>
                                <p className="faint text-xs">
                                    ממוצע <span className="num">{y.averageScore}</span> ב-<span className="num">{y.testsCount}</span> מבחנים
                                    {y.delta !== null && (
                                        <> · <span className="num">{y.delta > 0 ? `+${y.delta}` : y.delta}</span> מהשנה הקודמת</>
                                    )}
                                </p>
                            </>
                        ) : (
                            <p className="faint text-xs">אין עדיין ציוני מבחנים לשנה זו</p>
                        )}
                    </li>
                ))}
            </ol>
        </div>
    );
}
