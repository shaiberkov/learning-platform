// One-series line chart over ordered labels (e.g. months), values 0-100, in plain SVG.
// Points have native tooltips; only the last point is labeled.
export default function LineChart({data, max = 100, label = "ממוצע"}) {
    if (!data || data.length === 0) {
        return <p className="faint text-sm">אין נתונים להצגה</p>;
    }
    const width = 520;
    const height = 220;
    const pad = {top: 16, right: 24, bottom: 30, left: 36};
    const plotW = width - pad.left - pad.right;
    const plotH = height - pad.top - pad.bottom;
    const x = (i) => pad.left + (data.length === 1 ? plotW / 2 : (i / (data.length - 1)) * plotW);
    const y = (v) => pad.top + plotH - (Math.min(v, max) / max) * plotH;
    const path = data.map((d, i) => `${i === 0 ? "M" : "L"}${x(i)},${y(d.average)}`).join(" ");
    const last = data[data.length - 1];

    return (
        // Charts read left-to-right in time even inside the RTL page.
        <svg viewBox={`0 0 ${width} ${height}`} className="w-full h-auto" role="img" aria-label={label} direction="ltr">
            {[0, 50, 100].map((tick) => (
                <g key={tick}>
                    <line x1={pad.left} x2={width - pad.right} y1={y(tick)} y2={y(tick)} stroke="var(--line)"/>
                    <text x={pad.left - 6} y={y(tick) + 4} textAnchor="end" fontSize="11" fill="var(--ink-3)">{tick}</text>
                </g>
            ))}
            <path d={path} fill="none" stroke="var(--accent)" strokeWidth="2" strokeLinejoin="round"/>
            {data.map((d, i) => (
                <g key={d.label}>
                    <title>{`${d.label}: ${label} ${d.average} (${d.count} ציונים)`}</title>
                    <circle cx={x(i)} cy={y(d.average)} r="12" fill="transparent"/>
                    <circle cx={x(i)} cy={y(d.average)} r="4" fill="var(--accent)" stroke="var(--surface-strong)" strokeWidth="2"/>
                    <text x={x(i)} y={height - 8} textAnchor="middle" fontSize="11" fill="var(--ink-3)">{d.label}</text>
                </g>
            ))}
            <text x={x(data.length - 1)} y={y(last.average) - 10} textAnchor="middle" fontSize="12" fill="var(--ink)">
                {last.average}
            </text>
        </svg>
    );
}
