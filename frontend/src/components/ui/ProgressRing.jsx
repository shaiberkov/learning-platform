import { useId } from "react";

// Circular progress ring with a gradient stroke. value is 0–100.
export default function ProgressRing({ value = 0, size = 120, stroke = 10, children }) {
    const id = useId();
    const r = (size - stroke) / 2;
    const c = 2 * Math.PI * r;
    const clamped = Math.max(0, Math.min(100, Number(value) || 0));

    return (
        <div className="ring" style={{ width: size, height: size }}>
            <svg width={size} height={size} aria-hidden="true">
                <defs>
                    <linearGradient id={`ring-${id}`} x1="0" y1="0" x2="1" y2="1">
                        <stop offset="0%" stopColor="var(--accent)" />
                        <stop offset="55%" stopColor="var(--accent-2)" />
                        <stop offset="100%" stopColor="var(--mint)" />
                    </linearGradient>
                </defs>
                <circle className="ring__track" cx={size / 2} cy={size / 2} r={r} fill="none" strokeWidth={stroke} />
                <circle
                    className="ring__value"
                    cx={size / 2}
                    cy={size / 2}
                    r={r}
                    fill="none"
                    strokeWidth={stroke}
                    stroke={`url(#ring-${id})`}
                    strokeDasharray={c}
                    strokeDashoffset={c - (clamped / 100) * c}
                />
            </svg>
            <div className="ring__label">{children}</div>
        </div>
    );
}
