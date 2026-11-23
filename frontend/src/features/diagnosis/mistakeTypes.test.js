import { describe, expect, it } from "vitest";
import { dominantAdvice, mistakeType, scoreBand, summarizeCounts } from "./mistakeTypes.js";

describe("mistakeType", () => {
    it("gives each type its own follow-up practice", () => {
        expect(mistakeType("CALCULATION").action).toBe("SELF_CHECK");
        expect(mistakeType("CALCULATION").actionLabel).toBe("בדוק את עצמך");
        expect(mistakeType("CONCEPTUAL").action).toBe("LESSON");
        expect(mistakeType("SOMETHING_NEW").action).toBe("AI");
    });
});

describe("summarizeCounts", () => {
    it("returns every type in order with its share", () => {
        const rows = summarizeCounts({ CALCULATION: 3, CONCEPTUAL: 1 });
        expect(rows.map((r) => r.type)).toEqual(["CALCULATION", "CONCEPTUAL", "MISREAD", "UNCLASSIFIED"]);
        expect(rows[0].percent).toBe(75);
        expect(rows[2].count).toBe(0);
    });

    it("handles no mistakes", () => {
        expect(summarizeCounts().every((r) => r.percent === 0)).toBe(true);
    });
});

describe("dominantAdvice and scoreBand", () => {
    it("names the dominant type", () => {
        expect(dominantAdvice("CONCEPTUAL")).toContain("טעות מושגית");
        expect(dominantAdvice(null)).toContain("עוד אין");
    });

    it("bands the explanation score", () => {
        expect(scoreBand(85).chip).toBe("chip--mint");
        expect(scoreBand(60).chip).toBe("chip--amber");
        expect(scoreBand(10).chip).toBe("chip--danger");
        expect(scoreBand(null).label).toBe("לא נבדק");
    });
});
