import { Link } from "react-router-dom";
import Prism from "./Prism.jsx";

const dateLabel = () =>
    new Date().toLocaleDateString("he-IL", { weekday: "long", day: "numeric", month: "long" });

// Hero bento tile shared by every role dashboard.
export default function DashboardHero({ greeting, name, subtitle, eyebrow, children, className = "" }) {
    return (
        <section className={`glass tile relative overflow-hidden p-6 sm:p-9 min-h-[17rem] flex flex-col justify-between reveal ${className}`} style={{ "--i": 0 }}>
            <div className="hidden sm:block absolute left-2 -bottom-6 opacity-90 pointer-events-auto">
                <Prism size={70} />
            </div>

            <div className="relative z-10 flex items-center justify-between gap-3">
                <span className="eyebrow">{eyebrow}</span>
                <span className="chip num">{dateLabel()}</span>
            </div>

            <div className="relative z-10 mt-8 max-w-xl">
                <p className="muted text-lg">{greeting},</p>
                <h1 className="display text-4xl sm:text-6xl mt-1">
                    <span className="gradient-text">{name}</span>
                </h1>
                {subtitle && <p className="muted mt-3 text-base sm:text-lg">{subtitle}</p>}
            </div>

            {children && <div className="relative z-10 mt-6">{children}</div>}
        </section>
    );
}

// Small action tile linking to an existing route.
export function QuickTile({ to, icon, label, hint, tone = "", index = 0 }) {
    return (
        <Link
            to={to}
            className="glass tile group p-5 flex flex-col justify-between gap-6 min-h-[9.5rem] reveal"
            style={{ "--i": index }}
        >
            <span className={`icon-badge ${tone}`}>{icon}</span>
            <span>
                <span className="title block text-base">{label}</span>
                {hint && <span className="faint text-xs mt-1 block">{hint}</span>}
            </span>
            <span className="absolute top-5 left-5 text-ink-3 transition-all duration-500 ease-[var(--ease-spring)] group-hover:text-accent group-hover:-translate-x-1 group-hover:-translate-y-1" aria-hidden="true">
                ↖
            </span>
        </Link>
    );
}
