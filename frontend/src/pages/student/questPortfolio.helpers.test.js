import { describe, expect, it } from "vitest";
import {
    formatDate,
    questPercent,
    questStatusLabel,
    roomToneClass,
    shareUrl,
    trendMessage,
    withYearDeltas,
} from "./questPortfolio.helpers.js";

describe("questPercent", () => {
    it("rounds and clamps", () => {
        expect(questPercent(1, 3)).toBe(33);
        expect(questPercent(5, 4)).toBe(100);
        expect(questPercent(-1, 4)).toBe(0);
        expect(questPercent(2, 0)).toBe(0);
    });
});

describe("labels and tones", () => {
    it("maps known values and falls back", () => {
        expect(questStatusLabel("COMPLETED")).toBe("הושלם");
        expect(questStatusLabel("???")).toBe("");
        expect(roomToneClass("mint")).toBe("bg-mint-soft");
        expect(roomToneClass("purple")).toBe("bg-accent-soft");
    });
});

describe("withYearDeltas", () => {
    it("compares each year with the previous year that has an average", () => {
        const result = withYearDeltas([
            { gradeName: "ז'", averageScore: 80 },
            { gradeName: "ח'", averageScore: null },
            { gradeName: "ט'", averageScore: 86.5 },
        ]);
        expect(result.map((y) => y.delta)).toEqual([null, null, 6.5]);
    });

    it("handles a missing list", () => {
        expect(withYearDeltas(undefined)).toEqual([]);
    });
});

describe("trendMessage", () => {
    it("describes the change from the first scored grade", () => {
        expect(trendMessage([{ gradeName: "ז'", averageScore: 70 }, { gradeName: "ט'", averageScore: 82 }]))
            .toContain("עלייה של 12");
        expect(trendMessage([{ gradeName: "ז'", averageScore: 90 }, { gradeName: "ח'", averageScore: 85 }]))
            .toContain("ירידה של 5");
        expect(trendMessage([{ gradeName: "ז'", averageScore: 90 }])).toBe("");
    });
});

describe("formatDate", () => {
    it("formats dates and date-times", () => {
        expect(formatDate("2026-11-23")).toBe("23/11/2026");
        expect(formatDate("2026-11-23T10:15:00")).toBe("23/11/2026");
        expect(formatDate(null)).toBe("");
    });
});

describe("shareUrl", () => {
    it("builds the public page address", () => {
        expect(shareUrl("https://app.example/", "abc")).toBe("https://app.example/shared-portfolio/abc");
        expect(shareUrl("https://app.example", "")).toBe("");
    });
});
