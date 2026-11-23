// Student accommodations on the client: dyslexia-friendly reading mode, read-aloud through the
// browser's Web Speech API (Hebrew), and the extra-time formula the server also uses.
import { useEffect, useState } from "react";
import { apiGet } from "./authApi.js";
import { ACCOMMODATION_BASE_PATH, ACCOMMODATION_GET } from "../constants/pages.constants.js";

const DYSLEXIA_KEY = "lp-dyslexia";
const DYSLEXIA_CLASS = "dyslexia-mode";

export function applyDyslexiaFont(on) {
    document.documentElement.classList.toggle(DYSLEXIA_CLASS, Boolean(on));
    try {
        localStorage.setItem(DYSLEXIA_KEY, on ? "1" : "0");
    } catch {
        // storage blocked: the server copy still applies on the next load
    }
}

export function storedDyslexiaFont() {
    try {
        return localStorage.getItem(DYSLEXIA_KEY) === "1";
    } catch {
        return false;
    }
}

/** Same rounding as AccommodationService.adjustedMinutes on the server. */
export function adjustedMinutes(baseMinutes, extraTimePercent) {
    if (!baseMinutes || baseMinutes <= 0 || !extraTimePercent || extraTimePercent <= 0) return baseMinutes;
    return Math.ceil((baseMinutes * (100 + extraTimePercent)) / 100);
}

export const canSpeak = () => typeof window !== "undefined" && "speechSynthesis" in window;

// Math symbols read as Hebrew words, so "3/4 + 1/2 = ?" is spoken naturally.
const MATH_WORDS = [
    [/\s*\+\s*/g, " ועוד "],
    [/\s*[×*]\s*/g, " כפול "],
    [/\s*[÷/]\s*/g, " חלקי "],
    [/\s*=\s*/g, " שווה "],
    [/\s*\^\s*/g, " בחזקת "],
    [/\s*√\s*/g, " שורש "],
    [/(\d)\s*-\s*(\d)/g, "$1 פחות $2"],
    [/\?/g, ""],
];

export function speakableText(text) {
    let t = String(text ?? "");
    for (const [re, word] of MATH_WORDS) t = t.replace(re, word);
    return t.replace(/\s+/g, " ").trim();
}

/** Reads text aloud in Hebrew. Returns false when the browser has no speech support. */
export function speak(text) {
    if (!canSpeak()) return false;
    const synth = window.speechSynthesis;
    synth.cancel();
    const utterance = new SpeechSynthesisUtterance(speakableText(text));
    utterance.lang = "he-IL";
    utterance.rate = 0.9;
    const hebrew = synth.getVoices().find((v) => v.lang?.toLowerCase().startsWith("he"));
    if (hebrew) utterance.voice = hebrew;
    synth.speak(utterance);
    return true;
}

export function stopSpeaking() {
    if (canSpeak()) window.speechSynthesis.cancel();
}

const NONE = { extraTimePercent: 0, readAloud: false, dyslexiaFont: false };

/** The signed-in student's accommodations; defaults for everyone else or on error. */
export function useAccommodations(user) {
    const [accommodations, setAccommodations] = useState(NONE);
    const [loaded, setLoaded] = useState(false);

    useEffect(() => {
        if (!user?.userId || user.role !== "STUDENT") {
            setLoaded(true);
            return;
        }
        let cancelled = false;
        apiGet(`${ACCOMMODATION_BASE_PATH}${ACCOMMODATION_GET}`, { studentId: user.userId })
            .then((res) => {
                if (!cancelled && res?.success && res.data) setAccommodations(res.data);
            })
            .catch(() => {})
            .finally(() => {
                if (!cancelled) setLoaded(true);
            });
        return () => {
            cancelled = true;
        };
    }, [user?.userId, user?.role]);

    return { accommodations, setAccommodations, loaded };
}
