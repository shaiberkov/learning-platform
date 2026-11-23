import { useId } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { FaBriefcase, FaGlobeAmericas, FaShapes } from "react-icons/fa";
import { Skeleton } from "../../components/ui/Skeleton.jsx";
import { fetchRealWorldTopic } from "./realWorldApi.js";
import { simulationsForTopic } from "../simulations/simulationCatalog.js";

/**
 * "Why does it matter to me?" card for a practice topic or sub-topic: an everyday example, a link
 * to a profession, and the simulations of that topic. Renders nothing when the topic has no card.
 */
export default function RealWorldCard({ topic, className = "" }) {
    const headingId = useId();
    const { data, isLoading, isError } = useQuery({
        queryKey: ["real-world-topic", topic],
        queryFn: () => fetchRealWorldTopic(topic),
        enabled: Boolean(topic),
        retry: false,
    });

    if (!topic || isError) return null;
    if (isLoading) {
        return (
            <div className={`glass p-5 space-y-3 ${className}`} role="status" aria-live="polite">
                <span className="sr-only">טוען הסבר מהחיים</span>
                <Skeleton className="h-3 w-32" />
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-4 w-2/3" />
            </div>
        );
    }
    if (!data) return null;

    const sims = simulationsForTopic(data.topic);
    return (
        <section className={`glass p-5 sm:p-6 space-y-4 ${className}`} dir="rtl" aria-labelledby={headingId}>
            <div className="flex items-start justify-between gap-3">
                <div>
                    <span className="eyebrow">Real world · {data.topic}</span>
                    <h3 id={headingId} className="title text-lg mt-0.5">למה זה חשוב לי?</h3>
                </div>
                {data.customized && <span className="chip chip--mint">נכתב על ידי המורה</span>}
            </div>
            <div className="flex gap-3">
                <span className="icon-badge icon-badge--mint shrink-0" aria-hidden="true"><FaGlobeAmericas /></span>
                <p className="text-ink leading-relaxed text-sm">{data.everydayExample}</p>
            </div>
            <div className="flex gap-3">
                <span className="icon-badge icon-badge--amber shrink-0" aria-hidden="true"><FaBriefcase /></span>
                <p className="text-ink leading-relaxed text-sm">{data.careerLink}</p>
            </div>
            {sims.length > 0 && (
                <div className="flex flex-wrap gap-2 pt-1">
                    {sims.map((s) => (
                        <Link key={s.key} to={`/simulations?sim=${s.key}`} className="btn btn-ghost btn-sm">
                            <FaShapes aria-hidden="true" /> סימולציה: {s.title}
                        </Link>
                    ))}
                </div>
            )}
        </section>
    );
}
