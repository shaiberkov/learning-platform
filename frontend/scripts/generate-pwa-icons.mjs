// Generates the PWA icons in public/ (no image tooling needed): an accent square with the
// app's diamond "prism" mark. Run with `node scripts/generate-pwa-icons.mjs` after changing them.
import { writeFileSync } from "node:fs";
import { deflateSync } from "node:zlib";

const ACCENT = [47, 63, 214];
const ACCENT_2 = [90, 72, 240];
const WHITE = [255, 255, 255];

const crcTable = Array.from({ length: 256 }, (_, n) => {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    return c >>> 0;
});
const crc32 = (buf) => {
    let c = 0xffffffff;
    for (const b of buf) c = crcTable[(c ^ b) & 0xff] ^ (c >>> 8);
    return (c ^ 0xffffffff) >>> 0;
};
const chunk = (type, data) => {
    const len = Buffer.alloc(4);
    len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]);
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(crc32(td));
    return Buffer.concat([len, td, crc]);
};

function png(size, pixel) {
    const raw = Buffer.alloc((size * 4 + 1) * size);
    for (let y = 0; y < size; y++) {
        raw[y * (size * 4 + 1)] = 0;
        for (let x = 0; x < size; x++) {
            const [r, g, b, a] = pixel(x + 0.5, y + 0.5, size);
            const o = y * (size * 4 + 1) + 1 + x * 4;
            raw[o] = r; raw[o + 1] = g; raw[o + 2] = b; raw[o + 3] = a;
        }
    }
    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(size, 0);
    ihdr.writeUInt32BE(size, 4);
    ihdr[8] = 8; ihdr[9] = 6; ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;
    return Buffer.concat([
        Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
        chunk("IHDR", ihdr),
        chunk("IDAT", deflateSync(raw)),
        chunk("IEND", Buffer.alloc(0)),
    ]);
}

const mix = (a, b, t) => a.map((v, i) => Math.round(v + (b[i] - v) * t));

// maskable: full-bleed background, mark inside the 80% safe zone; any: rounded square.
function icon(maskable) {
    return (x, y, s) => {
        const u = x / s, v = y / s;
        const r = 0.22;
        if (!maskable) {
            const dx = Math.max(r - u, 0, u - (1 - r));
            const dy = Math.max(r - v, 0, v - (1 - r));
            if (dx * dx + dy * dy > r * r) return [0, 0, 0, 0];
        }
        let color = mix(ACCENT, ACCENT_2, (u + v) / 2);
        const d = Math.abs(u - 0.5) + Math.abs(v - 0.5); // diamond distance
        const outer = maskable ? 0.26 : 0.3;
        if (d < outer) color = WHITE;
        if (d < outer * 0.5) color = mix(ACCENT, ACCENT_2, 0.5);
        return [...color, 255];
    };
}

for (const [name, size, maskable] of [
    ["pwa-192x192.png", 192, false],
    ["pwa-512x512.png", 512, false],
    ["maskable-icon-512x512.png", 512, true],
    ["apple-touch-icon-180x180.png", 180, true],
]) {
    writeFileSync(new URL(`../public/${name}`, import.meta.url), png(size, icon(maskable)));
    console.log("wrote", name);
}
