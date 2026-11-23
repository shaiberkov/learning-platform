import { useRef, useState } from "react";
import { DraggablePoint, Plane, Stat } from "./Plane.jsx";
import { formatNumber, makePlane, triangleKind, triangleMeasures } from "./simMath.js";

const plane = makePlane(-10, 10);
const NAMES = ["A", "B", "C"];
const COLORS = ["var(--accent)", "var(--mint)", "var(--amber)"];

/** A triangle with draggable vertices: angles, sides, area and perimeter update live. */
export default function TriangleSim({ onExplore }) {
    const svgRef = useRef(null);
    const [vertices, setVertices] = useState([[-6, -4], [6, -4], [0, 6]]);
    const { sx, sy } = plane;

    const moveVertex = (index) => (x, y) => {
        onExplore?.();
        setVertices((vs) => vs.map((v, i) => (i === index ? [x, y] : v)));
    };

    const m = triangleMeasures(...vertices);
    const centroid = [0, 1].map((axis) => vertices.reduce((sum, v) => sum + v[axis], 0) / 3);
    const angleList = [m.angles.A, m.angles.B, m.angles.C];
    const sum = angleList.reduce((s, v) => s + v, 0);

    return (
        <div className="grid md:grid-cols-[1fr_16rem] gap-5 items-start">
            <Plane plane={plane} svgRef={svgRef}
                   label={`משולש עם קודקודים ${vertices.map((v, i) => `${NAMES[i]}(${v.join(", ")})`).join(", ")}`}>
                <polygon points={vertices.map(([x, y]) => `${sx.toPx(x)},${sy.toPx(y)}`).join(" ")}
                         fill="var(--accent-soft)" stroke="var(--accent)" strokeWidth={2.5} strokeLinejoin="round" />
                {vertices.map(([x, y], i) => {
                    // Label each vertex slightly outside the triangle, away from the centroid.
                    const dx = x - centroid[0];
                    const dy = y - centroid[1];
                    const len = Math.hypot(dx, dy) || 1;
                    return (
                        <text key={NAMES[i]} x={sx.toPx(x + (dx / len) * 1.3)} y={sy.toPx(y + (dy / len) * 1.3) + 4}
                              textAnchor="middle" fontSize="13" fontWeight="600" fill={COLORS[i]} fontFamily="var(--font-mono)">
                            {NAMES[i]} {formatNumber(angleList[i], 0)}°
                        </text>
                    );
                })}
                {vertices.map(([x, y], i) => (
                    <DraggablePoint key={NAMES[i]} plane={plane} svgRef={svgRef} x={x} y={y} color={COLORS[i]}
                                    label={`קודקוד ${NAMES[i]}`} onMove={moveVertex(i)} />
                ))}
            </Plane>

            <div className="space-y-4">
                <div className="grid grid-cols-3 gap-2">
                    <Stat label="∠A" value={`${formatNumber(m.angles.A, 1)}°`} />
                    <Stat label="∠B" value={`${formatNumber(m.angles.B, 1)}°`} />
                    <Stat label="∠C" value={`${formatNumber(m.angles.C, 1)}°`} />
                </div>
                <div className="grid grid-cols-3 gap-2">
                    <Stat label="BC" value={formatNumber(m.sides.a)} />
                    <Stat label="AC" value={formatNumber(m.sides.b)} />
                    <Stat label="AB" value={formatNumber(m.sides.c)} />
                </div>
                <div className="grid grid-cols-2 gap-2">
                    <Stat label="שטח" value={formatNumber(m.area)} />
                    <Stat label="היקף" value={formatNumber(m.perimeter)} />
                </div>
                <p className="muted text-sm leading-relaxed" aria-live="polite">
                    {m.degenerate
                        ? "שלושת הקודקודים על ישר אחד: זה כבר לא משולש, והשטח 0."
                        : `משולש ${triangleKind(m.angles)}. סכום הזוויות: ${formatNumber(sum, 1)}° — תמיד 180°, לא משנה איך מזיזים את הקודקודים.`}
                </p>
            </div>
        </div>
    );
}
