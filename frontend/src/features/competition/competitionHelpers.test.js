import { describe, expect, it } from "vitest";
import { duelResult, duelSides, duelStatusLabel, formatClock, nextQuestion, streakLabel } from "./competitionHelpers.js";

const duel = {
    mode: "COMPETITIVE",
    status: "ACTIVE",
    challengerId: "a",
    challengerName: "אלה",
    opponentId: "b",
    opponentName: "בני",
    challengerAnswered: 3,
    challengerCorrect: 2,
    opponentAnswered: 1,
    opponentCorrect: 1,
    outcome: null,
};

describe("duelSides", () => {
    it("puts the viewer on the 'my' side", () => {
        expect(duelSides(duel, "a")).toMatchObject({ iAmChallenger: true, myAnswered: 3, theirAnswered: 1, theirName: "בני" });
        expect(duelSides(duel, "b")).toMatchObject({ iAmChallenger: false, myAnswered: 1, theirCorrect: 2, theirName: "אלה" });
    });
});

describe("duelResult and duelStatusLabel", () => {
    it("reads the outcome from the viewer's side", () => {
        const finished = { ...duel, status: "FINISHED", outcome: "OPPONENT" };
        expect(duelResult(finished, "b").tone).toBe("mint");
        expect(duelResult(finished, "a").tone).toBe("danger");
        expect(duelResult({ ...finished, outcome: "DRAW" }, "a").tone).toBe("neutral");
    });

    it("labels coop results for the team", () => {
        expect(duelResult({ ...duel, mode: "COOP", outcome: "TEAM" }, "a").tone).toBe("mint");
        expect(duelResult({ ...duel, mode: "COOP", outcome: "SYSTEM" }, "b").tone).toBe("danger");
    });

    it("tells the invited student from the inviter", () => {
        const pending = { ...duel, status: "PENDING" };
        expect(duelStatusLabel(pending, "a")).toBe("ממתין לאישור");
        expect(duelStatusLabel(pending, "b")).toBe("הוזמנת");
    });
});

describe("small helpers", () => {
    it("finds the first unanswered question", () => {
        expect(nextQuestion([{ position: 0, myAnswer: "1" }, { position: 1, myAnswer: null }]).position).toBe(1);
        expect(nextQuestion([{ position: 0, myAnswer: "1" }])).toBeNull();
        expect(nextQuestion(undefined)).toBeNull();
    });

    it("formats the clock and the streak", () => {
        expect(formatClock(125)).toBe("2:05");
        expect(formatClock(-3)).toBe("0:00");
        expect(streakLabel(0)).toBe("עוד אין רצף");
        expect(streakLabel(4)).toBe("4 ימים ברצף");
    });
});
