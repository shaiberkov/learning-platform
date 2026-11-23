// Timer helpers shared by the study rooms and focus mode. Pure, so they are unit tested.

export const FOCUS = "FOCUS";
export const BREAK = "BREAK";

/**
 * Phase of a shared Pomodoro timer that started at startMs (focus first, then break, repeating).
 * Mirrors StudyRoomService.phaseAt on the server, so every member of a room sees the same phase.
 */
export function roomPhase(startMs, nowMs, focusMinutes = 25, breakMinutes = 5) {
    const focusMs = focusMinutes * 60_000;
    const cycleMs = focusMs + breakMinutes * 60_000;
    const elapsed = Math.max(0, nowMs - startMs);
    const inCycle = elapsed % cycleMs;
    if (inCycle < focusMs) {
        return { phase: FOCUS, secondsLeft: Math.ceil((focusMs - inCycle) / 1000), progress: inCycle / focusMs };
    }
    const intoBreak = inCycle - focusMs;
    const breakMs = cycleMs - focusMs;
    return { phase: BREAK, secondsLeft: Math.ceil((cycleMs - inCycle) / 1000), progress: intoBreak / breakMs };
}

/** Seconds as MM:SS (or H:MM:SS from an hour up). */
export function formatClock(totalSeconds) {
    const s = Math.max(0, Math.floor(totalSeconds));
    const hours = Math.floor(s / 3600);
    const minutes = Math.floor((s % 3600) / 60);
    const seconds = s % 60;
    const pad = (n) => String(n).padStart(2, "0");
    return hours > 0 ? `${hours}:${pad(minutes)}:${pad(seconds)}` : `${pad(minutes)}:${pad(seconds)}`;
}

/**
 * Seconds a focus session has run, given the segments it ran in: runningSinceMs is the start of the
 * current running segment (null while paused) and accumulatedMs what earlier segments added up to.
 */
export function elapsedSeconds(accumulatedMs, runningSinceMs, nowMs) {
    const running = runningSinceMs == null ? 0 : Math.max(0, nowMs - runningSinceMs);
    return Math.floor((accumulatedMs + running) / 1000);
}
