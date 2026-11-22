import { Link } from "react-router-dom";
import { FaTwitter, FaYoutube, FaFacebook } from "react-icons/fa";

export default function Footer() {
    return (

        <footer
            dir="rtl"
            className="relative mt-16 px-3 sm:px-6 pb-4"
        >
            <div className="glass max-w-7xl mx-auto px-6 sm:px-10 py-8 flex flex-col md:flex-row md:items-center md:justify-between gap-6 !rounded-[26px]">
                <div className="space-y-1">
                    <span className="eyebrow">Spatial Academy</span>
                    <p className="title text-lg">מרחב הלמידה</p>
                    <p className="faint text-xs num">© כל הזכויות שמורות {new Date().getFullYear()}</p>
                </div>

                {/* קישורים עיקריים */}
                <nav className="flex flex-wrap gap-x-6 gap-y-2 text-sm font-medium">
                    <Link
                        to="/about"
                        className="link !text-ink-2 hover:!text-accent"
                    >
                        אודות
                    </Link>

                    <Link
                        to="/faq"
                        className="link !text-ink-2 hover:!text-accent"
                    >
                        שאלות נפוצות
                    </Link>

                    <Link
                        to="/privacy-policy"
                        className="link !text-ink-2 hover:!text-accent"
                    >
                        מדיניות פרטיות
                    </Link>

                    <Link
                        to="/terms-of-service"
                        className="link !text-ink-2 hover:!text-accent"
                    >
                        תנאי שימוש
                    </Link>
                </nav>

                <div className="flex gap-2 text-lg">
                    <a
                        href="https://twitter.com"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="grid place-items-center w-11 h-11 rounded-2xl border border-line bg-surface-strong text-ink-2 hover:text-accent hover:border-accent hover:-translate-y-1 transition-all duration-500 ease-[var(--ease-spring)]"
                        aria-label="Twitter"
                    >
                        <FaTwitter/>
                    </a>
                    <a
                        href="https://youtube.com"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="grid place-items-center w-11 h-11 rounded-2xl border border-line bg-surface-strong text-ink-2 hover:text-accent hover:border-accent hover:-translate-y-1 transition-all duration-500 ease-[var(--ease-spring)]"
                        aria-label="YouTube"
                    >
                        <FaYoutube/>
                    </a>
                    <a
                        href="https://facebook.com"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="grid place-items-center w-11 h-11 rounded-2xl border border-line bg-surface-strong text-ink-2 hover:text-accent hover:border-accent hover:-translate-y-1 transition-all duration-500 ease-[var(--ease-spring)]"
                        aria-label="Facebook"
                    >
                        <FaFacebook/>
                    </a>
                </div>
            </div>
        </footer>
    )

}
