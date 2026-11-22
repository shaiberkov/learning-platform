import Prism from "./Prism.jsx";
import { useTranslation } from "react-i18next";

// Split auth layout: an editorial 3D stage beside the form card.
// On phones the stage collapses into a compact header above the card.
export default function AuthShell({ eyebrow, title, subtitle, children }) {
    const { i18n } = useTranslation();
    return (
        <div className="page page--wide" dir={i18n.dir()}>
            <div className="grid lg:grid-cols-[1.05fr_1fr] gap-5 lg:gap-8 items-stretch">
                <aside className="glass relative overflow-hidden p-6 sm:p-10 flex flex-col justify-between min-h-[15rem] lg:min-h-[34rem] reveal" style={{ "--i": 0 }}>
                    <div className="relative z-10 space-y-4">
                        <span className="eyebrow">{eyebrow}</span>
                        <h1 className="display text-4xl sm:text-5xl lg:text-6xl">
                            {title}
                        </h1>
                        {subtitle && <p className="muted text-base sm:text-lg max-w-md leading-relaxed">{subtitle}</p>}
                    </div>

                    <Prism size={92} className="hidden lg:grid absolute left-6 bottom-8" />
                    <Prism size={56} orbits={false} className="lg:hidden absolute -left-4 -top-2 opacity-80" />

                    <div className="relative z-10 hidden lg:flex items-center gap-6 pt-8">
                        <div>
                            <div className="num text-2xl font-semibold text-ink">4</div>
                            <div className="faint text-xs">תפקידים</div>
                        </div>
                        <div className="w-px h-8 bg-line" />
                        <div>
                            <div className="num text-2xl font-semibold text-ink">LIVE</div>
                            <div className="faint text-xs">התראות בזמן אמת</div>
                        </div>
                        <div className="w-px h-8 bg-line" />
                        <div>
                            <div className="num text-2xl font-semibold text-ink">AI</div>
                            <div className="faint text-xs">מרצה אישי</div>
                        </div>
                    </div>
                </aside>

                <div className="reveal" style={{ "--i": 1 }}>
                    {children}
                </div>
            </div>
        </div>
    );
}
