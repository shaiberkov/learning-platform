import { useEffect, useRef, useState } from "react";
import { FaPause, FaPlay, FaUndo } from "react-icons/fa";
import { Slider, Stat } from "./Plane.jsx";
import { formatNumber, makeScale, motionAt, positionRange, turnaroundTime } from "./simMath.js";

const T_MAX = 10; // seconds of simulated motion
const W = 400;

/** A small time graph (x(t) or v(t)) with a marker at the current time. */
function TimeGraph({ label, f, t, color, unit }) {
    const values = Array.from({ length: 101 }, (_, i) => f((T_MAX * i) / 100));
    let lo = Math.min(0, ...values);
    let hi = Math.max(0, ...values);
    if (hi - lo < 1e-9) { lo -= 1; hi += 1; }
    const sx = makeScale([0, T_MAX], [8, W - 8]);
    const sy = makeScale([lo, hi], [112, 8]);
    const d = values.map((v, i) => `${i ? "L" : "M"}${sx.toPx((T_MAX * i) / 100).toFixed(1)},${sy.toPx(v).toFixed(1)}`).join(" ");
    return (
        <svg viewBox={`0 0 ${W} 120`} role="img" aria-label={`${label}: ${formatNumber(f(t))} ${unit} בזמן ${formatNumber(t, 1)} שניות`}
             className="w-full rounded-xl border border-line bg-surface-strong" dir="ltr">
            <line x1={8} y1={sy.toPx(0)} x2={W - 8} y2={sy.toPx(0)} stroke="var(--ink-3)" strokeWidth={1} />
            <path d={d} fill="none" stroke={color} strokeWidth={2.5} />
            <line x1={sx.toPx(t)} y1={8} x2={sx.toPx(t)} y2={112} stroke="var(--ink-3)" strokeDasharray="4 4" />
            <circle cx={sx.toPx(t)} cy={sy.toPx(f(t))} r={5} fill={color} />
            <text x={12} y={20} fontSize="12" fill="var(--ink-2)" fontFamily="var(--font-mono)">{label}</text>
        </svg>
    );
}

/** Uniformly accelerated motion along a straight road: x = v0·t + a·t²/2, v = v0 + a·t. */
export default function MotionSim({ onExplore }) {
    const [v0, setV0] = useState(8);
    const [a, setA] = useState(-1.5);
    const [t, setT] = useState(0);
    const [playing, setPlaying] = useState(false);
    const frame = useRef(null);
    const timeRef = useRef(0);
    const setTime = (value) => {
        timeRef.current = value;
        setT(value);
    };

    useEffect(() => {
        if (!playing) return undefined;
        let last = performance.now();
        const tick = (now) => {
            const next = Math.min(T_MAX, timeRef.current + (now - last) / 1000);
            last = now;
            timeRef.current = next;
            setT(next);
            if (next >= T_MAX) {
                setPlaying(false);
                return;
            }
            frame.current = requestAnimationFrame(tick);
        };
        frame.current = requestAnimationFrame(tick);
        return () => cancelAnimationFrame(frame.current);
    }, [playing]);

    const touch = (fn) => (value) => {
        onExplore?.();
        fn(value);
    };
    const changeParam = (setter) => touch((value) => {
        setter(value);
        setPlaying(false);
        setTime(0);
    });

    const { x, v } = motionAt(0, v0, a, t);
    const [lo, hi] = positionRange(0, v0, a, T_MAX);
    const pad = Math.max(2, (hi - lo) * 0.08);
    const track = makeScale([lo - pad, hi + pad], [24, W - 24]);
    const turn = turnaroundTime(v0, a, T_MAX);
    const carX = track.toPx(x);
    const facing = v < 0 ? -1 : 1;

    let explanation;
    if (a === 0) explanation = "אין תאוצה: המהירות קבועה והמיקום משתנה בקצב קבוע (הגרף של x הוא ישר).";
    else if (v0 * a > 0 || v0 === 0) explanation = "התאוצה באותו כיוון של התנועה: המכונית מאיצה והגרף של x מתעקם כלפי חוץ.";
    else explanation = `התאוצה מנוגדת לתנועה: המכונית מאטה${turn !== null ? `, עוצרת אחרי ${formatNumber(turn, 1)} שניות ומשנה כיוון` : ""}.`;

    return (
        <div className="grid md:grid-cols-[1fr_16rem] gap-5 items-start">
            <div className="space-y-3">
                <svg viewBox={`0 0 ${W} 80`} role="img" dir="ltr"
                     aria-label={`מכונית במיקום ${formatNumber(x, 1)} מטר, מהירות ${formatNumber(v, 1)} מטר לשנייה`}
                     className="w-full rounded-xl border border-line bg-surface-strong">
                    <line x1={16} y1={60} x2={W - 16} y2={60} stroke="var(--ink-3)" strokeWidth={2} />
                    <line x1={track.toPx(0)} y1={50} x2={track.toPx(0)} y2={70} stroke="var(--amber)" strokeWidth={2} />
                    <text x={track.toPx(0)} y={78} textAnchor="middle" fontSize="9" fill="var(--amber)" fontFamily="var(--font-mono)">x=0</text>
                    <g transform={`translate(${carX} 44) scale(${facing} 1)`}>
                        <rect x={-16} y={-10} width={32} height={14} rx={4} fill="var(--accent)" />
                        <rect x={-6} y={-17} width={16} height={9} rx={3} fill="var(--accent-2)" />
                        <circle cx={-9} cy={6} r={4} fill="var(--ink)" />
                        <circle cx={9} cy={6} r={4} fill="var(--ink)" />
                    </g>
                    {Math.abs(v) > 0.05 && (
                        <line x1={carX} y1={22} x2={carX + Math.sign(v) * Math.min(60, Math.abs(v) * 4)} y2={22}
                              stroke="var(--mint)" strokeWidth={3} markerEnd="url(#sim-arrow)" />
                    )}
                    <defs>
                        <marker id="sim-arrow" viewBox="0 0 10 10" refX="5" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse">
                            <path d="M0,0 L10,5 L0,10 z" fill="var(--mint)" />
                        </marker>
                    </defs>
                </svg>
                <TimeGraph label="x(t) מיקום" unit="מטר" f={(s) => motionAt(0, v0, a, s).x} t={t} color="var(--accent)" />
                <TimeGraph label="v(t) מהירות" unit="מטר לשנייה" f={(s) => motionAt(0, v0, a, s).v} t={t} color="var(--mint)" />
            </div>

            <div className="space-y-4">
                <Slider id="motion-v0" label="מהירות התחלתית v₀" unit=" m/s" value={v0} min={-10} max={15} step={0.5} onChange={changeParam(setV0)} />
                <Slider id="motion-a" label="תאוצה a" unit=" m/s²" value={a} min={-4} max={4} step={0.25} onChange={changeParam(setA)} />
                <Slider id="motion-t" label="זמן t" unit=" s" value={Number(t.toFixed(1))} min={0} max={T_MAX} step={0.1}
                        onChange={touch((value) => { setPlaying(false); setTime(value); })} />
                <div className="flex gap-2">
                    <button type="button" className="btn btn-primary btn-sm"
                            onClick={() => { onExplore?.(); if (t >= T_MAX) setTime(0); setPlaying((p) => !p); }}>
                        {playing ? <><FaPause aria-hidden="true" /> השהיה</> : <><FaPlay aria-hidden="true" /> הפעלה</>}
                    </button>
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => { setPlaying(false); setTime(0); }}>
                        <FaUndo aria-hidden="true" /> מההתחלה
                    </button>
                </div>
                <div className="grid grid-cols-2 gap-2">
                    <Stat label="מיקום x" value={`${formatNumber(x, 1)} m`} />
                    <Stat label="מהירות v" value={`${formatNumber(v, 1)} m/s`} />
                </div>
                <p className="muted text-sm leading-relaxed" aria-live="polite">{explanation}</p>
            </div>
        </div>
    );
}
