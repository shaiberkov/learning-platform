import { useEffect, useState } from "react";
import { FaPause, FaPlay } from "react-icons/fa";
import { breathingPhase, PHASE_SECONDS } from "./wellbeing.js";

const prefersReducedMotion = () =>
    typeof window !== "undefined" && window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;

// [s10] Box breathing with a circle that grows on the inhale and shrinks on the exhale.
// With reduced motion the circle stays still and only the text and countdown change.
export default function BreathingExercise({ cyclesGoal = 4 }) {
    const [running, setRunning] = useState(false);
    const [elapsed, setElapsed] = useState(0);
    const reduced = prefersReducedMotion();

    useEffect(() => {
        if (!running) return undefined;
        const id = setInterval(() => setElapsed((e) => e + 1), 1000);
        return () => clearInterval(id);
    }, [running]);

    const { phase, secondsLeft, cycles } = breathingPhase(elapsed);
    const done = cycles >= cyclesGoal;

    useEffect(() => {
        if (done) setRunning(false);
    }, [done]);

    const restart = () => {
        setElapsed(0);
        setRunning(true);
    };

    return (
        <div className="row-card flex flex-col items-center gap-4 py-6" aria-label="תרגיל נשימה">
            <div className="relative grid place-items-center" style={{ width: 180, height: 180 }}>
                <span
                    aria-hidden="true"
                    className="absolute inset-0 rounded-full bg-accent-soft border border-line-strong"
                    style={{
                        transform: `scale(${running && !reduced ? phase.scale : 0.75})`,
                        transition: reduced ? "none" : `transform ${PHASE_SECONDS}s ease-in-out`,
                    }}
                />
                <span className="relative text-center" aria-live="polite">
                    <span className="title text-xl block">{done ? "כל הכבוד" : running ? phase.label : "מוכנ/ה?"}</span>
                    {running && <span className="num text-2xl text-ink">{secondsLeft}</span>}
                </span>
            </div>
            <p className="muted text-sm text-center max-w-xs">
                {done
                    ? "סיימת " + cyclesGoal + " סבבים. שימ/י לב איך הגוף מרגיש עכשיו."
                    : `שאיפה, החזקה, נשיפה והחזקה: ${PHASE_SECONDS} שניות כל אחת. סבב ${Math.min(cycles + 1, cyclesGoal)} מתוך ${cyclesGoal}.`}
            </p>
            <div className="flex gap-2">
                {running ? (
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => setRunning(false)}>
                        <FaPause aria-hidden="true" /> השהיה
                    </button>
                ) : (
                    <button type="button" className="btn btn-mint btn-sm" onClick={done || elapsed === 0 ? restart : () => setRunning(true)}>
                        <FaPlay aria-hidden="true" /> {done ? "שוב" : elapsed === 0 ? "התחלת תרגיל נשימה" : "המשך"}
                    </button>
                )}
            </div>
        </div>
    );
}
