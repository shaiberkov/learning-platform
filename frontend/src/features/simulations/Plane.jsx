import { useRef } from "react";
import { clamp, PLANE_SIZE as SIZE, snap } from "./simMath.js";

// A square coordinate plane drawn in SVG, plus a draggable point that also moves with the arrow keys.
// Colors come from the theme tokens, so the plane follows light and dark mode.

export function Plane({ plane, label, children, svgRef }) {
    const { min, max, sx, sy } = plane;
    const ticks = [];
    for (let v = Math.ceil(min); v <= max; v++) ticks.push(v);
    return (
        <svg
            ref={svgRef}
            viewBox={`0 0 ${SIZE} ${SIZE}`}
            role="img"
            aria-label={label}
            className="w-full max-w-[28rem] mx-auto block rounded-2xl border border-line bg-surface-strong touch-none select-none"
            dir="ltr"
        >
            {ticks.map((v) => (
                <g key={v}>
                    <line x1={sx.toPx(v)} y1={0} x2={sx.toPx(v)} y2={SIZE} stroke="var(--line)" strokeWidth={1} />
                    <line x1={0} y1={sy.toPx(v)} x2={SIZE} y2={sy.toPx(v)} stroke="var(--line)" strokeWidth={1} />
                </g>
            ))}
            <line x1={0} y1={sy.toPx(0)} x2={SIZE} y2={sy.toPx(0)} stroke="var(--ink-3)" strokeWidth={1.5} />
            <line x1={sx.toPx(0)} y1={0} x2={sx.toPx(0)} y2={SIZE} stroke="var(--ink-3)" strokeWidth={1.5} />
            {ticks.filter((v) => v !== 0 && v % 5 === 0).map((v) => (
                <g key={`l${v}`} fontSize="11" fill="var(--ink-3)" fontFamily="var(--font-mono)">
                    <text x={sx.toPx(v)} y={sy.toPx(0) + 14} textAnchor="middle">{v}</text>
                    <text x={sx.toPx(0) - 6} y={sy.toPx(v) + 4} textAnchor="end">{v}</text>
                </g>
            ))}
            {children}
        </svg>
    );
}

/** Pointer position inside the SVG, in world coordinates. */
function worldFromEvent(svg, event, plane) {
    const pt = svg.createSVGPoint();
    pt.x = event.clientX;
    pt.y = event.clientY;
    const local = pt.matrixTransform(svg.getScreenCTM().inverse());
    return [plane.sx.toWorld(local.x), plane.sy.toWorld(local.y)];
}

/**
 * A point the student drags (mouse, touch or pen) or moves with the arrow keys. Positions snap to
 * `step` and stay inside the plane.
 */
export function DraggablePoint({ plane, svgRef, x, y, onMove, label, color = "var(--accent)", step = 0.5 }) {
    const dragging = useRef(false);
    const place = (wx, wy) => {
        onMove(clamp(snap(wx, step), plane.min, plane.max), clamp(snap(wy, step), plane.min, plane.max));
    };
    const onPointerDown = (e) => {
        dragging.current = true;
        e.currentTarget.setPointerCapture?.(e.pointerId);
    };
    const onPointerMove = (e) => {
        if (!dragging.current || !svgRef.current) return;
        const [wx, wy] = worldFromEvent(svgRef.current, e, plane);
        place(wx, wy);
    };
    const onPointerUp = () => {
        dragging.current = false;
    };
    const onKeyDown = (e) => {
        const moves = { ArrowLeft: [-step, 0], ArrowRight: [step, 0], ArrowUp: [0, step], ArrowDown: [0, -step] };
        const move = moves[e.key];
        if (!move) return;
        e.preventDefault();
        place(x + move[0], y + move[1]);
    };
    return (
        <g
            tabIndex={0}
            role="button"
            aria-label={`${label}: (${x}, ${y}). אפשר לגרור או להזיז בחיצים`}
            onPointerDown={onPointerDown}
            onPointerMove={onPointerMove}
            onPointerUp={onPointerUp}
            onPointerCancel={onPointerUp}
            onKeyDown={onKeyDown}
            className="cursor-grab"
        >
            <circle cx={plane.sx.toPx(x)} cy={plane.sy.toPx(y)} r={16} fill={color} opacity={0.15} />
            <circle cx={plane.sx.toPx(x)} cy={plane.sy.toPx(y)} r={7} fill={color} stroke="var(--surface-strong)" strokeWidth={2} />
        </g>
    );
}

/** A labelled range slider with its value shown in the mono font. */
export function Slider({ id, label, value, min, max, step, onChange, unit = "" }) {
    return (
        <div className="space-y-1">
            <label htmlFor={id} className="label flex justify-between gap-2">
                <span>{label}</span>
                <span className="num" dir="ltr">{value}{unit}</span>
            </label>
            <input
                id={id}
                type="range"
                min={min}
                max={max}
                step={step}
                value={value}
                onChange={(e) => onChange(Number(e.target.value))}
                className="w-full accent-accent"
                dir="ltr"
            />
        </div>
    );
}

/** A small read-out tile: name and value. */
export function Stat({ label, value }) {
    return (
        <div className="glass--sunk rounded-xl px-3 py-2">
            <div className="faint text-xs">{label}</div>
            <div className="num text-ink text-base" dir="ltr">{value}</div>
        </div>
    );
}
