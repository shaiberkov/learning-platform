// Full-screen loading state: nested orbit rings around a breathing core.
export default function OrbitLoader({ title, subtitle }) {
    return (
        <div role="status" aria-live="polite" className="min-h-[70vh] w-full grid place-items-center px-4 page-enter" dir="rtl">
            <div className="flex flex-col items-center text-center gap-6">
                <div className="orbit-loader" aria-hidden="true"><span /></div>
                <div className="space-y-2">
                    <h2 className="title text-2xl">{title}</h2>
                    {subtitle && <p className="muted text-sm max-w-xs mx-auto">{subtitle}</p>}
                </div>
                <div className="dot-pulse" aria-hidden="true"><span /><span /><span /></div>
            </div>
        </div>
    );
}
