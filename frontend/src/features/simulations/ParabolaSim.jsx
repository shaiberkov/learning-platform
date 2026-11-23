import { useRef, useState } from "react";
import { DraggablePoint, Plane, Slider, Stat } from "./Plane.jsx";
import { formatNumber, makePlane, quadraticRoots, sampleCurve, vertexToStandard } from "./simMath.js";

const plane = makePlane(-10, 10);

const toPath = (segment) =>
    segment.map(([x, y], i) => `${i ? "L" : "M"}${plane.sx.toPx(x).toFixed(1)},${plane.sy.toPx(y).toFixed(1)}`).join(" ");

/** y = a(x - h)² + k: drag the vertex (h, k), slide a. */
export default function ParabolaSim({ onExplore }) {
    const svgRef = useRef(null);
    const [a, setA] = useState(1);
    const [vertex, setVertex] = useState({ h: 0, k: -4 });

    const touch = (fn) => (...args) => {
        onExplore?.();
        fn(...args);
    };

    const { b, c } = vertexToStandard(a, vertex.h, vertex.k);
    const roots = quadraticRoots(a, b, c);
    const segments = sampleCurve((x) => a * (x - vertex.h) ** 2 + vertex.k, plane.min, plane.max, plane.min - 1, plane.max + 1);
    const opens = a > 0 ? "כלפי מעלה (צורת ∪), לקודקוד יש נקודת מינימום" : a < 0
        ? "כלפי מטה (צורת ∩), לקודקוד יש נקודת מקסימום" : "זה כבר לא פרבולה: כש-a = 0 מקבלים ישר";

    return (
        <div className="grid md:grid-cols-[1fr_16rem] gap-5 items-start">
            <Plane plane={plane} svgRef={svgRef} label={`גרף של הפרבולה y = ${formatNumber(a)}(x - ${vertex.h})² + ${vertex.k}`}>
                {segments.map((s, i) => (
                    <path key={i} d={toPath(s)} fill="none" stroke="var(--accent)" strokeWidth={3} strokeLinecap="round" />
                ))}
                {roots.map((r) => (
                    <circle key={r} cx={plane.sx.toPx(r)} cy={plane.sy.toPx(0)} r={5} fill="var(--mint)" />
                ))}
                <line x1={plane.sx.toPx(vertex.h)} y1={0} x2={plane.sx.toPx(vertex.h)} y2={400}
                      stroke="var(--amber)" strokeDasharray="6 6" strokeWidth={1.5} />
                <DraggablePoint plane={plane} svgRef={svgRef} x={vertex.h} y={vertex.k} label="קודקוד הפרבולה"
                                onMove={touch((h, k) => setVertex({ h, k }))} />
            </Plane>

            <div className="space-y-4">
                <Slider id="parabola-a" label="המקדם a" value={a} min={-3} max={3} step={0.25} onChange={touch(setA)} />
                <div className="glass--sunk rounded-xl p-3 space-y-1 num text-sm text-ink" dir="ltr">
                    <div>y = {formatNumber(a)}(x {vertex.h >= 0 ? "−" : "+"} {Math.abs(vertex.h)})² {vertex.k >= 0 ? "+" : "−"} {Math.abs(vertex.k)}</div>
                    <div>y = {formatNumber(a)}x² {b >= 0 ? "+" : "−"} {formatNumber(Math.abs(b))}x {c >= 0 ? "+" : "−"} {formatNumber(Math.abs(c))}</div>
                </div>
                <div className="grid grid-cols-2 gap-2">
                    <Stat label="קודקוד" value={`(${vertex.h}, ${vertex.k})`} />
                    <Stat label="ציר סימטריה" value={`x = ${vertex.h}`} />
                    <Stat label="חיתוך עם ציר y" value={formatNumber(c)} />
                    <Stat label="חיתוכים עם ציר x" value={roots.length ? roots.map((r) => formatNumber(r)).join(" , ") : "אין"} />
                </div>
                <p className="muted text-sm leading-relaxed" aria-live="polite">
                    הפרבולה נפתחת {opens}. {Math.abs(a) > 1 ? "ככל ש-|a| גדול יותר, הפרבולה צרה ותלולה יותר." : Math.abs(a) > 0 && Math.abs(a) < 1 ? "כש-|a| קטן מ-1 הפרבולה רחבה ושטוחה יותר." : ""}
                </p>
            </div>
        </div>
    );
}
