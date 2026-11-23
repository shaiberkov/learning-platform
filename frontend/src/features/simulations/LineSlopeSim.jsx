import { useRef, useState } from "react";
import { DraggablePoint, Plane, Stat } from "./Plane.jsx";
import { distance, formatNumber, lineThrough, makePlane, slopeAngle } from "./simMath.js";

const plane = makePlane(-10, 10);

/** Two draggable points: slope, line equation, distance and the right triangle under the segment. */
export default function LineSlopeSim({ onExplore }) {
    const svgRef = useRef(null);
    const [p, setP] = useState([-4, -2]);
    const [q, setQ] = useState([4, 3]);

    const move = (setter) => (x, y) => {
        onExplore?.();
        setter([x, y]);
    };

    const line = lineThrough(p, q);
    const dist = distance(p, q);
    const { sx, sy } = plane;

    // The drawn line spans the whole plane, not only the segment between the points.
    const ends = line.vertical
        ? [[p[0], plane.min], [p[0], plane.max]]
        : [[plane.min, line.slope * plane.min + line.intercept], [plane.max, line.slope * plane.max + line.intercept]];

    let explanation;
    if (line.vertical) explanation = "הקו אנכי: השינוי ב-x הוא 0, ולכן השיפוע לא מוגדר.";
    else if (line.slope === 0) explanation = "הקו אופקי: y לא משתנה, השיפוע 0.";
    else if (line.slope > 0) explanation = "שיפוע חיובי: כשהולכים ימינה הקו עולה. ככל שהשיפוע גדול יותר הקו תלול יותר.";
    else explanation = "שיפוע שלילי: כשהולכים ימינה הקו יורד.";

    return (
        <div className="grid md:grid-cols-[1fr_16rem] gap-5 items-start">
            <Plane plane={plane} svgRef={svgRef} label={`ישר דרך הנקודות (${p.join(", ")}) ו-(${q.join(", ")})`}>
                <line x1={sx.toPx(ends[0][0])} y1={sy.toPx(ends[0][1])} x2={sx.toPx(ends[1][0])} y2={sy.toPx(ends[1][1])}
                      stroke="var(--accent)" strokeWidth={2.5} />
                {/* Rise over run */}
                <path d={`M${sx.toPx(p[0])},${sy.toPx(p[1])} L${sx.toPx(q[0])},${sy.toPx(p[1])} L${sx.toPx(q[0])},${sy.toPx(q[1])}`}
                      fill="none" stroke="var(--amber)" strokeWidth={2} strokeDasharray="6 5" />
                <text x={(sx.toPx(p[0]) + sx.toPx(q[0])) / 2} y={sy.toPx(p[1]) + 16} textAnchor="middle"
                      fontSize="12" fill="var(--amber)" fontFamily="var(--font-mono)">Δx = {formatNumber(q[0] - p[0])}</text>
                <text x={sx.toPx(q[0]) + 6} y={(sy.toPx(p[1]) + sy.toPx(q[1])) / 2} fontSize="12"
                      fill="var(--amber)" fontFamily="var(--font-mono)">Δy = {formatNumber(q[1] - p[1])}</text>
                <DraggablePoint plane={plane} svgRef={svgRef} x={p[0]} y={p[1]} label="נקודה A" onMove={move(setP)} />
                <DraggablePoint plane={plane} svgRef={svgRef} x={q[0]} y={q[1]} label="נקודה B" color="var(--mint)" onMove={move(setQ)} />
            </Plane>

            <div className="space-y-4">
                <div className="glass--sunk rounded-xl p-3 num text-sm text-ink" dir="ltr">
                    {line.vertical ? `x = ${p[0]}` : `y = ${formatNumber(line.slope)}x ${line.intercept >= 0 ? "+" : "−"} ${formatNumber(Math.abs(line.intercept))}`}
                </div>
                <div className="grid grid-cols-2 gap-2">
                    <Stat label="שיפוע m = Δy / Δx" value={line.vertical ? "לא מוגדר" : formatNumber(line.slope)} />
                    <Stat label="זווית עם ציר x" value={`${formatNumber(slopeAngle(line.slope), 1)}°`} />
                    <Stat label="מרחק AB" value={formatNumber(dist)} />
                    <Stat label="אמצע הקטע" value={`(${formatNumber((p[0] + q[0]) / 2)}, ${formatNumber((p[1] + q[1]) / 2)})`} />
                </div>
                <p className="muted text-sm leading-relaxed" aria-live="polite">{explanation}</p>
                <p className="faint text-xs leading-relaxed">המרחק מחושב בפיתגורס: √(Δx² + Δy²). המשולש המקווקו מראה את Δx ואת Δy.</p>
            </div>
        </div>
    );
}
