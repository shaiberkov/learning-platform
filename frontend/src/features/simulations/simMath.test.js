import { describe, expect, it } from "vitest";
import {
    formatNumber,
    lineThrough,
    makeScale,
    motionAt,
    positionRange,
    quadraticRoots,
    sampleCurve,
    snap,
    triangleKind,
    triangleMeasures,
    turnaroundTime,
    vertexToStandard,
} from "./simMath.js";
import { SIMULATIONS, simulationsForTopic } from "./simulationCatalog.js";

describe("simMath", () => {
    it("formats numbers without -0 and trailing zeros", () => {
        expect(formatNumber(-0.0001)).toBe("0");
        expect(formatNumber(2.5)).toBe("2.5");
        expect(formatNumber(1 / 3, 3)).toBe("0.333");
        expect(formatNumber(Infinity)).toBe("∞");
        expect(snap(1.26, 0.5)).toBe(1.5);
    });

    it("maps world to pixels and back, flipping the y axis", () => {
        const y = makeScale([-10, 10], [400, 0]);
        expect(y.toPx(10)).toBe(0);
        expect(y.toPx(-10)).toBe(400);
        expect(y.toWorld(200)).toBe(0);
    });

    it("converts vertex form and finds roots", () => {
        expect(vertexToStandard(1, 2, -4)).toEqual({ a: 1, b: -4, c: 0 });
        expect(quadraticRoots(1, -4, 0)).toEqual([0, 4]);
        expect(quadraticRoots(1, 0, 1)).toEqual([]);
        expect(quadraticRoots(1, -2, 1)).toEqual([1]);
        expect(quadraticRoots(0, 2, -4)).toEqual([2]);
    });

    it("splits a curve where it leaves the view", () => {
        const segments = sampleCurve((x) => x * x, -10, 10, -10, 10, 40);
        expect(segments).toHaveLength(1);
        segments[0].forEach(([, yv]) => expect(yv).toBeLessThanOrEqual(10));
        expect(sampleCurve((x) => 1 / x, -1, 1, -5, 5, 40)).toHaveLength(2);
    });

    it("computes slope and vertical lines", () => {
        expect(lineThrough([0, 1], [2, 5])).toEqual({ slope: 2, intercept: 1, vertical: false });
        expect(lineThrough([3, 0], [3, 4]).vertical).toBe(true);
    });

    it("measures a 3-4-5 right triangle", () => {
        const m = triangleMeasures([0, 0], [4, 0], [0, 3]);
        expect(m.area).toBe(6);
        expect(m.perimeter).toBeCloseTo(12);
        expect(m.angles.A).toBeCloseTo(90);
        expect(m.angles.A + m.angles.B + m.angles.C).toBeCloseTo(180);
        expect(triangleKind(m.angles)).toBe("ישר-זווית");
        expect(triangleMeasures([0, 0], [1, 1], [2, 2]).degenerate).toBe(true);
    });

    it("models uniformly accelerated motion", () => {
        expect(motionAt(0, 10, -2, 3)).toEqual({ x: 21, v: 4 });
        expect(turnaroundTime(10, -2, 10)).toBe(5);
        expect(turnaroundTime(10, 2, 10)).toBeNull();
        expect(positionRange(0, 10, -2, 10)).toEqual([0, 25]);
    });
});

describe("simulationCatalog", () => {
    it("links topics to simulations and keeps keys unique", () => {
        const keys = SIMULATIONS.map((s) => s.key);
        expect(new Set(keys).size).toBe(keys.length);
        expect(simulationsForTopic("משוואות").map((s) => s.key)).toContain("parabola");
        expect(simulationsForTopic("שברים")).toEqual([]);
        expect(simulationsForTopic(undefined)).toEqual([]);
    });
});
