import axios from "axios";
import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { PageSkeleton } from "../../components/ui/Skeleton.jsx";
import EmptyState from "../../components/ui/EmptyState.jsx";
import YearsTimeline from "../student/YearsTimeline.jsx";
import { formatDate, PORTFOLIO_PRINT_CSS } from "../student/questPortfolio.helpers.js";
import { PORTFOLIO_PUBLIC_BASE_PATH } from "../../constants/pages.constants.js";

// [s09] The read-only page behind a portfolio share link. Public: no login, only the token in the URL.

async function fetchShared(token) {
    const response = await axios.get(`${PORTFOLIO_PUBLIC_BASE_PATH}/${encodeURIComponent(token)}`, { silentError: true });
    return response.data?.data;
}

export default function SharedPortfolio() {
    const { token } = useParams();
    const shared = useQuery({
        queryKey: ["shared-portfolio", token],
        queryFn: () => fetchShared(token),
        enabled: !!token,
        retry: false,
    });

    if (shared.isLoading) return <PageSkeleton />;
    if (shared.isError || !shared.data) {
        return (
            <div className="page page--medium" dir="rtl">
                <div className="glass p-6">
                    <EmptyState title="הקישור לא זמין" body="ייתכן שהשיתוף בוטל או שהקישור שגוי. אפשר לבקש קישור חדש." />
                </div>
            </div>
        );
    }
    const p = shared.data;

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <style>{PORTFOLIO_PRINT_CSS}</style>
            <section className="glass p-6 sm:p-8 flex flex-wrap items-center gap-6 reveal">
                <span className="grid place-items-center w-24 h-24 rounded-full bg-accent-soft text-6xl" aria-hidden="true">{p.avatarEmoji}</span>
                <div className="space-y-1 flex-1 min-w-0">
                    <span className="eyebrow">Portfolio · פורטפוליו</span>
                    <h1 className="display text-3xl sm:text-4xl">{p.displayName}</h1>
                    <p className="muted">
                        {p.gradeName ? `כיתה ${p.gradeName} · ` : ""}רמה <span className="num">{p.level}</span>
                        {" "}· <span className="num">{p.totalXp}</span> נקודות
                    </p>
                    <p className="faint text-xs">שותף ב-{formatDate(p.sharedOn)} · לקריאה בלבד</p>
                </div>
                <button type="button" className="btn btn-ghost no-print" onClick={() => window.print()}>הדפסה / שמירה כ-PDF</button>
            </section>

            <section className="glass p-6 sm:p-8 space-y-4" aria-labelledby="shared-years">
                <h2 id="shared-years" className="title text-2xl">התקדמות לאורך השנים</h2>
                <YearsTimeline years={p.years} />
            </section>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
                <section className="glass p-6 sm:p-8 space-y-4" aria-labelledby="shared-badges">
                    <h2 id="shared-badges" className="title text-2xl">תגים</h2>
                    {p.badges.length === 0 ? <EmptyState title="אין תגים להצגה" /> : (
                        <ul className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                            {p.badges.map((b, i) => (
                                <li key={`${b.title}-${i}`} className="row-card space-y-1">
                                    <div className="font-semibold text-ink">{b.title}</div>
                                    {b.description && <div className="muted text-sm">{b.description}</div>}
                                    {b.earnedOn && <div className="faint text-xs">{formatDate(b.earnedOn)}</div>}
                                </li>
                            ))}
                        </ul>
                    )}
                </section>
                <section className="glass p-6 sm:p-8 space-y-4" aria-labelledby="shared-quests">
                    <h2 id="shared-quests" className="title text-2xl">מסעות שהושלמו</h2>
                    {p.quests.length === 0 ? <EmptyState title="אין מסעות להצגה" /> : (
                        <ul className="space-y-2">
                            {p.quests.map((q) => (
                                <li key={q.code} className="row-card flex items-center gap-3">
                                    <span className="text-3xl" aria-hidden="true">{q.emoji}</span>
                                    <span className="font-semibold text-ink flex-1">{q.title}</span>
                                    <span className="faint text-xs">{formatDate(q.completedAt)}</span>
                                </li>
                            ))}
                        </ul>
                    )}
                </section>
            </div>

            <section className="glass p-6 sm:p-8 space-y-4" aria-labelledby="shared-projects">
                <h2 id="shared-projects" className="title text-2xl">פרויקטים</h2>
                {p.projects.length === 0 ? <EmptyState title="אין פרויקטים להצגה" /> : (
                    <ul className="grid grid-cols-1 md:grid-cols-2 gap-3">
                        {p.projects.map((proj, i) => (
                            <li key={`${proj.title}-${i}`} className="row-card space-y-2">
                                <span className="font-semibold text-ink">{proj.title}</span>
                                {proj.description && <p className="muted text-sm whitespace-pre-line">{proj.description}</p>}
                                <p className="faint text-xs">
                                    {proj.gradeName ? `כיתה ${proj.gradeName} · ` : ""}{formatDate(proj.createdOn)}
                                </p>
                            </li>
                        ))}
                    </ul>
                )}
            </section>
        </div>
    );
}
