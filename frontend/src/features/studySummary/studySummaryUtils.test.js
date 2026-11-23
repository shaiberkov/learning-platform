import { describe, expect, it } from "vitest";
import {
    buildGenerateRequest,
    formatCreatedAt,
    formatTestStart,
    splitForSpeech,
    toggleTopic,
} from "./studySummaryUtils.js";

describe("formatTestStart", () => {
    it("shows day/month and time", () => {
        expect(formatTestStart("03/12/2026 08:30")).toBe("03/12 08:30");
    });
    it("keeps an unknown format and handles empty values", () => {
        expect(formatTestStart("מחר")).toBe("מחר");
        expect(formatTestStart(null)).toBe("");
    });
});

describe("formatCreatedAt", () => {
    it("formats an ISO date-time", () => {
        expect(formatCreatedAt("2026-11-23T10:15:00")).toBe("23/11/2026");
    });
});

describe("buildGenerateRequest", () => {
    it("sends the test id as a number and trims topics", () => {
        expect(buildGenerateRequest({ testId: "12", topics: [" אלגברה ", ""] })).toEqual({
            testId: 12,
            topics: ["אלגברה"],
            basic: false,
        });
    });
    it("sends nulls for an empty choice and passes basic", () => {
        expect(buildGenerateRequest({ testId: "", topics: [] }, true)).toEqual({ testId: null, topics: null, basic: true });
    });
});

describe("toggleTopic", () => {
    it("adds and removes", () => {
        expect(toggleTopic(["א"], "ב")).toEqual(["א", "ב"]);
        expect(toggleTopic(["א", "ב"], "א")).toEqual(["ב"]);
    });
});

describe("splitForSpeech", () => {
    it("splits at sentence ends", () => {
        expect(splitForSpeech("שלום. מה שלומך? טוב!")).toEqual(["שלום.", "מה שלומך?", "טוב!"]);
    });
    it("breaks a long sentence at spaces without losing words", () => {
        const sentence = Array.from({ length: 60 }, (_, i) => `מילה${i}`).join(" ");
        const chunks = splitForSpeech(sentence, 50);
        expect(chunks.every((c) => c.length <= 50)).toBe(true);
        expect(chunks.join(" ")).toBe(sentence);
    });
    it("returns nothing for empty text", () => {
        expect(splitForSpeech("  ")).toEqual([]);
    });
});
