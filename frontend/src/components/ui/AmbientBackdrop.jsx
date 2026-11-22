// Fixed ambient mesh behind every screen: two drifting blobs, a faint
// engineering grid and film grain. Purely decorative.
export default function AmbientBackdrop() {
    return (
        <div className="ambient" aria-hidden="true">
            <div className="ambient__blob ambient__blob--a" />
            <div className="ambient__blob ambient__blob--b" />
            <div className="ambient__grid" />
            <div className="ambient__grain" />
        </div>
    );
}
