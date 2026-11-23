import { describe, expect, it } from "vitest";
import { cleanDrafts, daysUntil, dueLabel, groupNotesByTarget, targetLink } from "./studyToolsUtils.js";

const TODAY = new Date(2026, 10, 23); // 23/11/2026, local time

describe("dueLabel", () => {
    it("counts whole days from today", () => {
        expect(daysUntil("2026-11-23", TODAY)).toBe(0);
        expect(daysUntil("2026-11-29", TODAY)).toBe(6);
        expect(daysUntil("2026-11-20", TODAY)).toBe(-3);
        expect(daysUntil(null, TODAY)).toBeNull();
    });

    it("labels today, overdue, tomorrow and later", () => {
        expect(dueLabel("2026-11-23", TODAY)).toBe("לחזרה היום");
        expect(dueLabel("2026-11-01", TODAY)).toBe("לחזרה היום");
        expect(dueLabel("2026-11-24", TODAY)).toBe("מחר");
        expect(dueLabel("2026-12-01", TODAY)).toBe("בעוד 8 ימים");
        expect(dueLabel(undefined, TODAY)).toBe("");
    });
});

describe("groupNotesByTarget", () => {
    const notes = [
        { id: 3, targetType: "QUESTION", targetId: 7, targetTitle: "2x+3=7", topic: "משוואות" },
        { id: 2, targetType: "LESSON", targetId: 1, targetTitle: "שברים", topic: "שברים" },
        { id: 1, targetType: "QUESTION", targetId: 7, targetTitle: "2x+3=7", topic: "משוואות" },
    ];

    it("keeps one group per target in the server's order", () => {
        const groups = groupNotesByTarget(notes);
        expect(groups.map((g) => g.key)).toEqual(["QUESTION:7", "LESSON:1"]);
        expect(groups[0].notes.map((n) => n.id)).toEqual([3, 1]);
        expect(groups[1].title).toBe("שברים");
    });

    it("handles an empty list", () => {
        expect(groupNotesByTarget(undefined)).toEqual([]);
    });
});

describe("targetLink", () => {
    it("links lessons, practice questions and the notebooks", () => {
        expect(targetLink({ targetType: "LESSON", targetId: 4 })).toBe("/lessons/4");
        expect(targetLink({ targetType: "QUESTION", targetId: 9, subject: "מתמטיקה", topic: "משוואות", subTopic: "ממעלה ראשונה" }))
            .toBe("/exercises/מתמטיקה/משוואות/ממעלה ראשונה");
        expect(targetLink({ targetType: "QUESTION", targetId: 9 })).toBeNull();
        expect(targetLink({ targetType: "REVIEW_ITEM", targetId: 2 })).toBe("/mistakes-notebook");
    });
});

describe("cleanDrafts", () => {
    it("trims and drops half-empty cards", () => {
        expect(cleanDrafts([{ front: " א ", back: "ב" }, { front: "ג", back: "  " }, { front: "", back: "ד" }]))
            .toEqual([{ front: "א", back: "ב" }]);
    });
});
