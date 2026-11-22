import { useEffect, useState } from "react";

// UI-only theme state: sets data-theme on <html> and remembers the choice.
const STORAGE_KEY = "lp-theme";

function readInitialTheme() {
    const attr = document.documentElement.dataset.theme;
    if (attr === "light" || attr === "dark") return attr;
    return window.matchMedia?.("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

// The orbital focus dial: a planet circles the dial face and the core
// contracts as the studio moves from daylight to night focus.
export default function ThemeDial() {
    const [theme, setTheme] = useState(readInitialTheme);

    useEffect(() => {
        document.documentElement.dataset.theme = theme;
        try {
            localStorage.setItem(STORAGE_KEY, theme);
        } catch {
            /* storage unavailable: theme still applies for this visit */
        }
    }, [theme]);

    const toggle = (e) => {
        const sweep = document.createElement("div");
        sweep.className = "theme-sweep";
        sweep.style.setProperty("--sx", `${e.clientX}px`);
        sweep.style.setProperty("--sy", `${e.clientY}px`);
        document.body.appendChild(sweep);
        setTimeout(() => sweep.remove(), 900);
        setTheme((t) => (t === "dark" ? "light" : "dark"));
    };

    const isDark = theme === "dark";

    return (
        <button
            type="button"
            onClick={toggle}
            className="dial"
            aria-label={isDark ? "מעבר למצב יום" : "מעבר למצב לילה"}
            title={isDark ? "Daylight Studio" : "Night Owl Focus"}
            style={{ "--dial-rot": isDark ? "180deg" : "0deg" }}
        >
            <span className="dial__face" aria-hidden="true">
                <span className="dial__planet" />
                <span className="dial__core" />
            </span>
            <span className="dial__label hidden sm:grid" aria-hidden="true">
                <small>{isDark ? "mode 02" : "mode 01"}</small>
                <b>{isDark ? "NIGHT OWL" : "DAY STUDIO"}</b>
            </span>
        </button>
    );
}
