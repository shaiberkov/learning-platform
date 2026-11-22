import { useRef } from "react";

// CSS-only 3D hexagonal glass prism with orbiting knowledge nodes.
// Tracks the pointer to tilt the whole scene (depth parallax).
export default function Prism({ size = 120, speed = 22, orbits = true, className = "" }) {
    const ref = useRef(null);

    const onMove = (e) => {
        const el = ref.current;
        if (!el) return;
        const r = el.getBoundingClientRect();
        el.style.setProperty("--px", ((e.clientX - r.left) / r.width - 0.5).toFixed(3));
        el.style.setProperty("--py", ((e.clientY - r.top) / r.height - 0.5).toFixed(3));
    };

    const onLeave = () => {
        ref.current?.style.setProperty("--px", 0);
        ref.current?.style.setProperty("--py", 0);
    };

    return (
        <div
            ref={ref}
            onPointerMove={onMove}
            onPointerLeave={onLeave}
            className={`scene grid place-items-center select-none ${className}`}
            aria-hidden="true"
        >
            <div className="parallax relative grid place-items-center" style={{ width: size * 2.6, height: size * 2.4 }}>
                {orbits && (
                    <>
                        <div className="orbit" style={{ "--orbit-speed": "16s", inset: `${size * 0.1}px` }}>
                            <span className="orbit__node" />
                        </div>
                        <div className="orbit" style={{ "--orbit-speed": "26s", inset: `${size * 0.4}px`, animationDirection: "reverse" }}>
                            <span className="orbit__node" style={{ "--node": "var(--amber)" }} />
                        </div>
                    </>
                )}
                <div className="float">
                    <div className="prism" style={{ "--size": `${size}px`, "--speed": `${speed}s` }}>
                        <span className="prism__face" />
                        <span className="prism__face" />
                        <span className="prism__face" />
                        <span className="prism__face" />
                        <span className="prism__face" />
                        <span className="prism__face" />
                        <span className="prism__core" />
                    </div>
                    <div className="prism-shadow mt-10" style={{ "--size": `${size}px` }} />
                </div>
            </div>
        </div>
    );
}
