import katex from "katex";
import "katex/dist/katex.min.css";

// Renders text where formulas are written between $...$ (inline) or $$...$$ (display) with KaTeX.
// Plain text stays React text, so only KaTeX output is injected as HTML.
const SPLIT = /(\$\$[^$]+\$\$|\$[^$]+\$)/g;

export default function MathText({ text, className = "" }) {
    if (!text) return null;
    const parts = String(text).split(SPLIT);
    return (
        <span className={className}>
            {parts.map((part, i) => {
                const display = part.startsWith("$$") && part.endsWith("$$") && part.length > 4;
                const inline = !display && part.startsWith("$") && part.endsWith("$") && part.length > 2;
                if (!display && !inline) return <span key={i}>{part}</span>;
                const expr = display ? part.slice(2, -2) : part.slice(1, -1);
                const html = katex.renderToString(expr, { throwOnError: false, displayMode: display });
                return <span key={i} dir="ltr" dangerouslySetInnerHTML={{ __html: html }} />;
            })}
        </span>
    );
}
