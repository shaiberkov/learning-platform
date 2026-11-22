import { Link } from "react-router-dom";
import { FaTwitter, FaYoutube, FaFacebook } from "react-icons/fa";
import { useTranslation } from "react-i18next";

export default function Footer() {
    const { t, i18n } = useTranslation();
    return (

        <footer
            dir={i18n.dir()}
            className="relative mt-16 px-3 sm:px-6 pb-4"
        >
            <div className="glass max-w-7xl mx-auto px-6 sm:px-10 py-8 flex flex-col md:flex-row md:items-center md:justify-between gap-6 !rounded-[26px]">
                <div className="space-y-1">
                    <span className="eyebrow">Spatial Academy</span>
                    <p className="title text-lg">{t("app.name")}</p>
                    <p className="faint text-xs num">{t("footer.rights", { year: new Date().getFullYear() })}</p>
                </div>

                {/* קישורים עיקריים */}
                <nav aria-label={t("app.name")} className="flex flex-wrap gap-x-6 gap-y-2 text-sm font-medium">
                    <Link
                        to="/about"
                        className="link !text-ink-2 hover:!text-accent"
                    >
                        {t("footer.about")}
                    </Link>

                    <Link
                        to="/faq"
                        className="link !text-ink-2 hover:!text-accent"
                    >
                        {t("footer.faq")}
                    </Link>

                    <Link
                        to="/privacy-policy"
                        className="link !text-ink-2 hover:!text-accent"
                    >
                        {t("footer.privacy")}
                    </Link>

                    <Link
                        to="/terms-of-service"
                        className="link !text-ink-2 hover:!text-accent"
                    >
                        {t("footer.terms")}
                    </Link>

                    <Link
                        to="/accessibility"
                        className="link !text-ink-2 hover:!text-accent"
                    >
                        {t("footer.accessibility")}
                    </Link>
                </nav>

                <div className="flex gap-2 text-lg">
                    <a
                        href="https://twitter.com"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="grid place-items-center w-11 h-11 rounded-2xl border border-line bg-surface-strong text-ink-2 hover:text-accent hover:border-accent hover:-translate-y-1 transition-all duration-500 ease-[var(--ease-spring)]"
                        aria-label="Twitter (נפתח בחלון חדש)"
                    >
                        <FaTwitter aria-hidden="true"/>
                    </a>
                    <a
                        href="https://youtube.com"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="grid place-items-center w-11 h-11 rounded-2xl border border-line bg-surface-strong text-ink-2 hover:text-accent hover:border-accent hover:-translate-y-1 transition-all duration-500 ease-[var(--ease-spring)]"
                        aria-label="YouTube (נפתח בחלון חדש)"
                    >
                        <FaYoutube aria-hidden="true"/>
                    </a>
                    <a
                        href="https://facebook.com"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="grid place-items-center w-11 h-11 rounded-2xl border border-line bg-surface-strong text-ink-2 hover:text-accent hover:border-accent hover:-translate-y-1 transition-all duration-500 ease-[var(--ease-spring)]"
                        aria-label="Facebook (נפתח בחלון חדש)"
                    >
                        <FaFacebook aria-hidden="true"/>
                    </a>
                </div>
            </div>
        </footer>
    )

}
