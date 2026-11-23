import { describe, expect, it } from "vitest";
import {
    cleanDistractors,
    similarQuery,
    successRate,
    toSubmitPayload,
    validatePeerQuestion,
} from "./classCommunityUtils.js";

describe("cleanDistractors", () => {
    it("trims, drops blanks and case-insensitive duplicates", () => {
        expect(cleanDistractors([" 7 ", "", null, "abc", "ABC", "34"])).toEqual(["7", "abc", "34"]);
    });
});

describe("validatePeerQuestion", () => {
    const base = { questionText: "כמה זה 3*4?", correctAnswer: "12", distractors: ["7", "34"] };

    it("accepts a question with two different distractors", () => {
        expect(validatePeerQuestion(base)).toBeNull();
    });

    it("needs the question and the correct answer", () => {
        expect(validatePeerQuestion({ ...base, questionText: " " })).toMatch("נוסח");
        expect(validatePeerQuestion({ ...base, correctAnswer: "" })).toMatch("התשובה הנכונה");
    });

    it("needs at least two distinct distractors, none equal to the answer", () => {
        expect(validatePeerQuestion({ ...base, distractors: ["7", " 7"] })).toMatch("לפחות");
        expect(validatePeerQuestion({ ...base, distractors: ["7", "12"] })).toMatch("זהה");
    });

    it("allows at most five distractors", () => {
        expect(validatePeerQuestion({ ...base, distractors: ["1", "2", "3", "4", "5", "6"] })).toMatch("עד");
    });
});

describe("toSubmitPayload", () => {
    it("cleans the form and turns blank optional fields into null", () => {
        expect(toSubmitPayload({ questionText: " ש ", correctAnswer: " 1 ", distractors: ["2", "", "3"], topic: " ", explanation: "" }))
            .toEqual({ questionText: "ש", correctAnswer: "1", distractors: ["2", "3"], topic: null, explanation: null });
    });
});

describe("similarQuery", () => {
    it("joins title and exercise and skips short input", () => {
        expect(similarQuery("שב", "")).toBe("");
        expect(similarQuery(" שברים ", " 1/2+1/3 ")).toBe("שברים 1/2+1/3");
    });
});

describe("successRate", () => {
    it("is null before any answer and a rounded percent after", () => {
        expect(successRate(0, 0)).toBeNull();
        expect(successRate(3, 2)).toBe(67);
    });
});
