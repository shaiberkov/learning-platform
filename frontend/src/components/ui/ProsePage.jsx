import Prism from "./Prism.jsx";

// Editorial layout for the static pages (about, FAQ, legal).
export default function ProsePage({ eyebrow, title, intro, children }) {
    return (
        <div dir="rtl" className="page page--wide">
            <header className="glass relative overflow-hidden p-7 sm:p-12 mb-4 sm:mb-5 reveal">
                <Prism size={54} orbits={false} className="absolute left-6 top-1/2 -translate-y-1/2 hidden sm:grid opacity-90" />
                <span className="eyebrow">{eyebrow}</span>
                <h1 className="display text-4xl sm:text-6xl mt-3 max-w-2xl">{title}</h1>
                {intro && <p className="muted text-base sm:text-lg mt-4 max-w-2xl leading-relaxed">{intro}</p>}
            </header>
            <article className="glass p-7 sm:p-12 prose-lp reveal" style={{ "--i": 1 }}>
                {children}
            </article>
        </div>
    );
}
