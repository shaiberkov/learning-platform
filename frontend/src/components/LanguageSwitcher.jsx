import { useTranslation } from "react-i18next";
import { LANGUAGES } from "../i18n/index.js";

// Interface language: Hebrew (default, RTL), Arabic (RTL), English (LTR).
export default function LanguageSwitcher({ compact = false }) {
    const { t, i18n } = useTranslation();
    return (
        <label className={compact ? "inline-flex items-center" : "grid gap-1"}>
            <span className={compact ? "sr-only" : "label"}>{t("lang.label")}</span>
            <select
                value={i18n.language}
                onChange={(e) => i18n.changeLanguage(e.target.value)}
                className={compact
                    ? "h-11 rounded-2xl bg-surface-sunk border border-line text-ink text-sm px-2 hover:border-accent"
                    : "field"}
            >
                {LANGUAGES.map((l) => (
                    <option key={l.code} value={l.code} lang={l.code}>{t(`lang.${l.code}`)}</option>
                ))}
            </select>
        </label>
    );
}
