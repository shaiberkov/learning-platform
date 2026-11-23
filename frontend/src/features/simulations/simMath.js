// Pure math for the interactive simulations (no React, no DOM), tested in simMath.test.js.

export const clamp = (value, min, max) => Math.min(max, Math.max(min, value));

/** Rounds to the nearest multiple of step (drag targets snap to the grid). */
export const snap = (value, step) => Math.round(value / step) * step;

/** A number for display: at most `digits` decimals, no trailing zeros, never "-0". */
export function formatNumber(value, digits = 2) {
    if (!Number.isFinite(value)) return "∞";
    const rounded = Number(value.toFixed(digits));
    return String(Object.is(rounded, -0) ? 0 : rounded);
}

/**
 * Linear map between a world interval and a pixel interval. Passing a reversed pixel interval
 * (for example [400, 0]) flips the axis, which is how the y axis points up.
 */
export function makeScale([d0, d1], [r0, r1]) {
    const k = (r1 - r0) / (d1 - d0);
    return {
        toPx: (v) => r0 + (v - d0) * k,
        toWorld: (px) => d0 + (px - r0) / k,
    };
}

/** Side of the square SVG viewBox every coordinate plane is drawn in. */
export const PLANE_SIZE = 400;

/** World-to-pixel scales of a square plane showing [min, max] on both axes (y points up). */
export function makePlane(min = -10, max = 10) {
    return {
        min,
        max,
        sx: makeScale([min, max], [0, PLANE_SIZE]),
        sy: makeScale([min, max], [PLANE_SIZE, 0]),
    };
}

/** y = a(x - h)² + k written as y = ax² + bx + c. */
export function vertexToStandard(a, h, k) {
    return { a, b: -2 * a * h, c: a * h * h + k };
}

/** Real roots of ax² + bx + c = 0, ascending (one root when the discriminant is 0). */
export function quadraticRoots(a, b, c) {
    if (a === 0) return b === 0 ? [] : [-c / b];
    const disc = b * b - 4 * a * c;
    if (disc < -1e-12) return [];
    if (Math.abs(disc) <= 1e-12) return [-b / (2 * a)];
    const s = Math.sqrt(disc);
    return [(-b - s) / (2 * a), (-b + s) / (2 * a)].sort((x, y) => x - y);
}

/** Points of y = f(x) on [xMin, xMax]; points outside [yMin, yMax] split the curve into segments. */
export function sampleCurve(f, xMin, xMax, yMin, yMax, steps = 160) {
    const segments = [];
    let current = [];
    for (let i = 0; i <= steps; i++) {
        const x = xMin + ((xMax - xMin) * i) / steps;
        const y = f(x);
        if (Number.isFinite(y) && y >= yMin && y <= yMax) {
            current.push([x, y]);
        } else if (current.length) {
            segments.push(current);
            current = [];
        }
    }
    if (current.length) segments.push(current);
    return segments;
}

export function distance([x1, y1], [x2, y2]) {
    return Math.hypot(x2 - x1, y2 - y1);
}

/** Slope and y-intercept of the line through two points; vertical lines have slope Infinity. */
export function lineThrough([x1, y1], [x2, y2]) {
    if (x1 === x2) return { slope: Infinity, intercept: null, vertical: true };
    const slope = (y2 - y1) / (x2 - x1);
    return { slope, intercept: y1 - slope * x1, vertical: false };
}

/** Slope angle in degrees, between -90 and 90. */
export const slopeAngle = (slope) => (Number.isFinite(slope) ? (Math.atan(slope) * 180) / Math.PI : 90);

/** Area of a triangle from its vertices (shoelace formula). */
export function triangleArea([ax, ay], [bx, by], [cx, cy]) {
    return Math.abs((bx - ax) * (cy - ay) - (cx - ax) * (by - ay)) / 2;
}

/**
 * Side lengths (a opposite A, b opposite B, c opposite C) and angles in degrees (law of cosines).
 * A degenerate triangle (collinear vertices) reports degenerate = true.
 */
export function triangleMeasures(A, B, C) {
    const a = distance(B, C);
    const b = distance(A, C);
    const c = distance(A, B);
    const area = triangleArea(A, B, C);
    const angle = (opposite, s1, s2) => {
        if (s1 === 0 || s2 === 0) return 0;
        const cos = clamp((s1 * s1 + s2 * s2 - opposite * opposite) / (2 * s1 * s2), -1, 1);
        return (Math.acos(cos) * 180) / Math.PI;
    };
    return {
        sides: { a, b, c },
        angles: { A: angle(a, b, c), B: angle(b, a, c), C: angle(c, a, b) },
        area,
        perimeter: a + b + c,
        degenerate: area < 1e-9,
    };
}

/** Kind of triangle by its largest angle, in Hebrew. */
export function triangleKind(angles) {
    const largest = Math.max(angles.A, angles.B, angles.C);
    if (Math.abs(largest - 90) < 0.5) return "ישר-זווית";
    return largest > 90 ? "קהה-זווית" : "חד-זווית";
}

/** Uniformly accelerated motion: position and velocity at time t. */
export function motionAt(x0, v0, a, t) {
    return { x: x0 + v0 * t + (a * t * t) / 2, v: v0 + a * t };
}

/** Time at which the velocity is zero (the body turns around), or null when it never does in (0, tMax]. */
export function turnaroundTime(v0, a, tMax) {
    if (a === 0) return null;
    const t = -v0 / a;
    return t > 0 && t <= tMax ? t : null;
}

/** Smallest and largest position over [0, tMax], used to fit the track to the screen. */
export function positionRange(x0, v0, a, tMax) {
    const times = [0, tMax];
    const turn = turnaroundTime(v0, a, tMax);
    if (turn !== null) times.push(turn);
    const xs = times.map((t) => motionAt(x0, v0, a, t).x);
    return [Math.min(...xs), Math.max(...xs)];
}
