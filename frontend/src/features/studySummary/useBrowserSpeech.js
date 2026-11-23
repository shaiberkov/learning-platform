// [s05] Reads a long Hebrew script aloud with the browser's speechSynthesis (he-IL), sentence by
// sentence, with pause / resume / stop and an adjustable rate. Works without any AI key.
import { useEffect, useRef, useState } from "react";
import { canSpeak, speakableText } from "../../Utils/accessibility.js";
import { splitForSpeech } from "./studySummaryUtils.js";

export default function useBrowserSpeech() {
    const supported = canSpeak();
    const [status, setStatus] = useState("idle"); // idle | playing | paused
    const [position, setPosition] = useState({ index: 0, total: 0 });
    const chunksRef = useRef([]);
    const indexRef = useRef(0);
    const rateRef = useRef(1);
    const spokenRateRef = useRef(1); // the rate of the sentence being spoken
    const runRef = useRef(0);

    const speakAt = (i, run) => {
        if (run !== runRef.current) return;
        const chunks = chunksRef.current;
        if (i >= chunks.length) {
            indexRef.current = 0;
            setStatus("idle");
            setPosition({ index: 0, total: chunks.length });
            return;
        }
        indexRef.current = i;
        setPosition({ index: i, total: chunks.length });
        const synth = window.speechSynthesis;
        const utterance = new SpeechSynthesisUtterance(speakableText(chunks[i]));
        utterance.lang = "he-IL";
        utterance.rate = rateRef.current;
        spokenRateRef.current = rateRef.current;
        const hebrew = synth.getVoices().find((v) => v.lang?.toLowerCase().startsWith("he"));
        if (hebrew) utterance.voice = hebrew;
        utterance.onend = () => speakAt(i + 1, run);
        utterance.onerror = (e) => {
            if (run === runRef.current && e.error !== "interrupted" && e.error !== "canceled") setStatus("idle");
        };
        synth.speak(utterance);
    };

    const restartAt = (i) => {
        window.speechSynthesis.cancel();
        runRef.current += 1;
        setStatus("playing");
        speakAt(i, runRef.current);
    };

    const play = (text) => {
        if (!supported) return;
        chunksRef.current = splitForSpeech(text);
        restartAt(0);
    };

    const pause = () => {
        if (!supported) return;
        window.speechSynthesis.pause();
        setStatus("paused");
    };

    const resume = () => {
        if (!supported) return;
        // The speed changed while paused: say the current sentence again at the new speed.
        if (spokenRateRef.current !== rateRef.current) {
            window.speechSynthesis.resume(); // some browsers stay paused after cancel()
            restartAt(indexRef.current);
            return;
        }
        window.speechSynthesis.resume();
        setStatus("playing");
    };

    const stop = () => {
        if (!supported) return;
        runRef.current += 1;
        window.speechSynthesis.cancel();
        indexRef.current = 0;
        setStatus("idle");
        setPosition({ index: 0, total: chunksRef.current.length });
    };

    /** A new rate applies at once: the current sentence starts again at the new speed. */
    const setRate = (rate) => {
        rateRef.current = rate;
        if (supported && status === "playing") restartAt(indexRef.current);
    };

    useEffect(() => () => {
        if (canSpeak()) {
            runRef.current += 1;
            window.speechSynthesis.cancel();
        }
    }, []);

    return { supported, status, position, play, pause, resume, stop, setRate };
}
