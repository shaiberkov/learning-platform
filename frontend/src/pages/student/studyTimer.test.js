import { describe, expect, it } from "vitest";
import { BREAK, FOCUS, elapsedSeconds, formatClock, roomPhase } from "./studyTimer.js";

const MIN = 60_000;

describe("roomPhase", () => {
    it("starts with 25 minutes of focus", () => {
        expect(roomPhase(0, 0)).toMatchObject({ phase: FOCUS, secondsLeft: 1500 });
    });

    it("switches to a 5 minute break after 25 minutes", () => {
        expect(roomPhase(0, 25 * MIN)).toMatchObject({ phase: BREAK, secondsLeft: 300 });
        expect(roomPhase(0, 29 * MIN + 30_000)).toMatchObject({ phase: BREAK, secondsLeft: 30 });
    });

    it("repeats the cycle every 30 minutes", () => {
        expect(roomPhase(0, 30 * MIN)).toMatchObject({ phase: FOCUS, secondsLeft: 1500 });
        expect(roomPhase(1000, 61 * MIN + 1000)).toMatchObject({ phase: FOCUS, secondsLeft: 24 * 60 });
    });

    it("treats a start in the future as the start", () => {
        expect(roomPhase(10 * MIN, 0)).toMatchObject({ phase: FOCUS, secondsLeft: 1500 });
    });
});

describe("formatClock", () => {
    it("pads minutes and seconds", () => {
        expect(formatClock(0)).toBe("00:00");
        expect(formatClock(65)).toBe("01:05");
        expect(formatClock(25 * 60)).toBe("25:00");
    });

    it("shows hours from an hour up and never goes negative", () => {
        expect(formatClock(3725)).toBe("1:02:05");
        expect(formatClock(-4)).toBe("00:00");
    });
});

describe("elapsedSeconds", () => {
    it("adds the running segment to earlier ones", () => {
        expect(elapsedSeconds(60_000, 1000, 31_000)).toBe(90);
    });

    it("ignores the clock while paused", () => {
        expect(elapsedSeconds(60_000, null, 999_999)).toBe(60);
    });
});
