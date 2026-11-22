// Shared frame for the management forms: glass card, eyebrow, title, icon.
export default function FormShell({ eyebrow, title, icon, tone = "", width = "narrow", children }) {
    return (
        <div className={`page page--${width}`} dir="rtl">
            <div className="glass relative overflow-hidden p-6 sm:p-8 reveal">
                <div
                    className="absolute -top-24 -left-24 w-64 h-64 rounded-full pointer-events-none bg-[radial-gradient(circle,var(--accent-soft),transparent_70%)]"
                    aria-hidden="true"
                />
                <div className="relative flex items-start gap-4 pb-5 mb-6 border-b border-dashed border-line">
                    {icon && <span className={`icon-badge ${tone}`}>{icon}</span>}
                    <div className="min-w-0">
                        <span className="eyebrow">{eyebrow}</span>
                        <h2 className="title text-2xl sm:text-3xl mt-1.5">{title}</h2>
                    </div>
                </div>
                <div className="relative space-y-5">{children}</div>
            </div>
        </div>
    );
}
