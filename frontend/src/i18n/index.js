import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import axios from "axios";
import he from "./locales/he.json";
import ar from "./locales/ar.json";
import en from "./locales/en.json";

// Hebrew is the default; Arabic is also right-to-left. The choice is a per-browser
// convenience kept in localStorage ("lp-lang") and sent to the server as Accept-Language,
// so server messages that use MessageSource come back in the same language.
export const LANGUAGES = [
    { code: "he", dir: "rtl" },
    { code: "ar", dir: "rtl" },
    { code: "en", dir: "ltr" },
];
const STORAGE_KEY = "lp-lang";

export const dirFor = (lng) => (LANGUAGES.find((l) => l.code === lng)?.dir ?? "rtl");

function readInitialLanguage() {
    try {
        const saved = localStorage.getItem(STORAGE_KEY);
        if (LANGUAGES.some((l) => l.code === saved)) return saved;
    } catch {
        /* storage unavailable: use the default */
    }
    return "he";
}

function applyLanguage(lng) {
    document.documentElement.lang = lng;
    document.documentElement.dir = dirFor(lng);
    axios.defaults.headers.common["Accept-Language"] = lng;
    try {
        localStorage.setItem(STORAGE_KEY, lng);
    } catch {
        /* storage unavailable: the language still applies for this visit */
    }
}

i18n.use(initReactI18next).init({
    resources: { he: { translation: he }, ar: { translation: ar }, en: { translation: en } },
    lng: readInitialLanguage(),
    fallbackLng: "he",
    interpolation: { escapeValue: false },
    returnNull: false,
});

applyLanguage(i18n.language);
i18n.on("languageChanged", applyLanguage);

export default i18n;
