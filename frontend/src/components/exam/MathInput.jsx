import { useEffect, useRef, useState } from "react";
import "katex/dist/katex.min.css";

// Formula input built on MathLive's <math-field>. MathLive is loaded on first use (it is large).
// Its glyphs come from the KaTeX fonts that katex.min.css already declares, so MathLive's own
// font and sound folders are switched off. Falls back to a plain text field if loading fails.
let mathliveLoad = null;
const loadMathlive = () => {
    if (!mathliveLoad) {
        mathliveLoad = import("mathlive").then(({ MathfieldElement }) => {
            MathfieldElement.fontsDirectory = null;
            MathfieldElement.soundsDirectory = null;
        });
    }
    return mathliveLoad;
};

export default function MathInput({ value, onChange, placeholder = "" }) {
    const ref = useRef(null);
    const [status, setStatus] = useState("loading");

    useEffect(() => {
        let alive = true;
        loadMathlive()
            .then(() => alive && setStatus("ready"))
            .catch(() => alive && setStatus("fallback"));
        return () => { alive = false; };
    }, []);

    useEffect(() => {
        const field = ref.current;
        if (status !== "ready" || !field) return;
        const handler = () => onChange(field.value);
        field.addEventListener("input", handler);
        return () => field.removeEventListener("input", handler);
    }, [status, onChange]);

    useEffect(() => {
        const field = ref.current;
        if (status === "ready" && field && field.value !== (value || "")) {
            field.value = value || "";
        }
    }, [status, value]);

    if (status === "fallback") {
        return (
            <input
                type="text"
                dir="ltr"
                className="field num"
                value={value || ""}
                placeholder={placeholder}
                onChange={(e) => onChange(e.target.value)}
            />
        );
    }
    if (status === "loading") {
        return <div className="field faint text-sm">טוען עורך נוסחאות...</div>;
    }
    return (
        <math-field
            ref={ref}
            dir="ltr"
            class="field num block w-full"
            style={{ minHeight: "3rem", fontSize: "1.25rem" }}
            placeholder={placeholder}
        />
    );
}
