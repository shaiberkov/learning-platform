// Horizontal bar chart for one series of averages (0-100), in plain SVG.
// Each bar carries a native tooltip; the value is printed at the bar end.
// Colors come from the theme tokens, so it follows light and dark mode.
export default function BarChart({data, max = 100, label = "ממוצע"}) {
    if (!data || data.length === 0) {
        return <p className="faint text-sm">אין נתונים להצגה</p>;
    }
    const rowH = 30;
    const labelW = 110;
    const valueW = 46;
    const width = 520;
    const plotW = width - labelW - valueW;
    const height = data.length * rowH + 8;

    return (
        <svg viewBox={`0 0 ${width} ${height}`} className="w-full h-auto" role="img" aria-label={label}>
            {[0, 50, 100].map((tick) => {
                const x = labelW + (tick / max) * plotW;
                return <line key={tick} x1={x} x2={x} y1={0} y2={height} stroke="var(--line)" strokeWidth="1"/>;
            })}
            {data.map((d, i) => {
                const y = i * rowH + 6;
                const w = Math.max(2, (Math.min(d.average, max) / max) * plotW);
                return (
                    <g key={d.label}>
                        <title>{`${d.label}: ${label} ${d.average} (${d.count} ציונים)`}</title>
                        <rect x={0} y={y - 4} width={width} height={rowH} fill="transparent"/>
                        <text x={labelW - 8} y={y + 13} textAnchor="end" fontSize="12" fill="var(--ink-2)">
                            {d.label.length > 14 ? d.label.slice(0, 13) + "…" : d.label}
                        </text>
                        <rect x={labelW} y={y} width={w} height={rowH - 12} rx="4" fill="var(--accent)"/>
                        <text x={labelW + w + 6} y={y + 13} fontSize="12" fill="var(--ink)" className="num">
                            {d.average}
                        </text>
                    </g>
                );
            })}
        </svg>
    );
}
