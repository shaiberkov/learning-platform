import { describe, expect, it } from "vitest";
import { breathingPhase, goalPercent, goalType, moodLabel, shortDate } from "./wellbeing.js";

describe("breathingPhase", () => {
    it("walks through inhale, hold, exhale, hold", () => {
        expect(breathingPhase(0).phase.key).toBe("inhale");
        expect(breathingPhase(0).secondsLeft).toBe(4);
        expect(breathingPhase(5).phase.key).toBe("hold-in");
        expect(breathingPhase(9).phase.key).toBe("exhale");
        expect(breathingPhase(15).phase.key).toBe("hold-out");
        expect(breathingPhase(15).secondsLeft).toBe(1);
    });

    it("counts completed cycles and restarts", () => {
        expect(breathingPhase(16)).toMatchObject({ cycles: 1, secondsLeft: 4 });
        expect(breathingPhase(16).phase.key).toBe("inhale");
        expect(breathingPhase(-3).cycles).toBe(0);
    });
});

describe("goalPercent", () => {
    it("is null for a free goal", () => {
        expect(goalPercent(5, null)).toBeNull();
        expect(goalPercent(5, 0)).toBeNull();
    });

    it("is clamped to 0-100", () => {
        expect(goalPercent(10, 20)).toBe(50);
        expect(goalPercent(30, 20)).toBe(100);
        expect(goalPercent(undefined, 20)).toBe(0);
    });
});

describe("labels", () => {
    it("falls back sensibly", () => {
        expect(moodLabel("OK")).toBe("בסדר");
        expect(moodLabel("X")).toBe("X");
        expect(goalType("nope").value).toBe("FREE");
        expect(shortDate("2026-11-22")).toBe("22/11");
    });
});
