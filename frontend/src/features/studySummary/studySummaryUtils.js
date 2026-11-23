// [s05] Pure helpers for the summary sheet and the study podcast screens.

/** "dd/MM/yyyy HH:mm" (the server's test start time) → "dd/MM HH:mm". */
export function formatTestStart(startTime) {
    if (!startTime) return "";
    const match = /^(\d{2})\/(\d{2})\/\d{4}\s+(\d{2}:\d{2})/.exec(String(startTime).trim());
    if (!match) return String(startTime);
    const [, day, month, time] = match;
    return `${day}/${month} ${time}`;
}

/** ISO date-time → "dd/MM/yyyy". */
export function formatCreatedAt(iso) {
    if (!iso) return "";
    const [date] = String(iso).split("T");
    const [y, m, d] = date.split("-");
    return d && m && y ? `${d}/${m}/${y}` : String(iso);
}

/** The request body for a new sheet or podcast. An empty choice sends nulls (the server then picks the weak topics). */
export function buildGenerateRequest({ testId, topics }, basic = false) {
    const cleanTopics = (topics || []).map((t) => String(t).trim()).filter(Boolean);
    return {
        testId: testId ? Number(testId) : null,
        topics: cleanTopics.length ? cleanTopics : null,
        basic,
    };
}

/** Adds or removes a topic from the selection. */
export function toggleTopic(selected, topic) {
    return selected.includes(topic) ? selected.filter((t) => t !== topic) : [...selected, topic];
}

/**
 * Splits a script into pieces the browser's speechSynthesis reads reliably (long utterances get
 * cut off in some browsers). Breaks at sentence ends, then at commas, then at spaces.
 */
export function splitForSpeech(text, maxLength = 220) {
    const clean = String(text ?? "").replace(/\s+/g, " ").trim();
    if (!clean) return [];
    const sentences = clean.match(/[^.!?\n]+[.!?]*/g) || [clean];
    const chunks = [];
    for (const raw of sentences) {
        const sentence = raw.trim();
        if (!sentence) continue;
        if (sentence.length <= maxLength) {
            chunks.push(sentence);
            continue;
        }
        let current = "";
        for (const word of sentence.split(" ")) {
            const next = current ? `${current} ${word}` : word;
            if (next.length > maxLength && current) {
                chunks.push(current);
                current = word;
            } else {
                current = next;
            }
        }
        if (current) chunks.push(current);
    }
    return chunks;
}

export const PLAYBACK_RATES = [0.75, 1, 1.25, 1.5, 1.75, 2];
