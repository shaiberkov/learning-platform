import { Link } from "react-router-dom";

// A helpful empty state: what is missing, why, and (optionally) the next step.
export default function EmptyState({ title, body, actionLabel, actionTo, onAction, icon }) {
    return (
        <div className="flex flex-col items-center justify-center gap-3 p-6 text-center">
            <span aria-hidden="true" className="w-14 h-14 rounded-2xl border border-dashed border-line-strong grid place-items-center text-ink-3 text-xl">
                {icon ?? <span className="w-3 h-3 rotate-45 rounded-[3px] bg-current opacity-60" />}
            </span>
            {title && <p className="title text-base">{title}</p>}
            {body && <p className="muted text-sm max-w-sm leading-relaxed">{body}</p>}
            {actionLabel && actionTo && (
                <Link to={actionTo} className="btn btn-ghost btn-sm">{actionLabel}</Link>
            )}
            {actionLabel && onAction && !actionTo && (
                <button type="button" onClick={onAction} className="btn btn-ghost btn-sm">{actionLabel}</button>
            )}
        </div>
    );
}
