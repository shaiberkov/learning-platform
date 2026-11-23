// Offline practice storage (IndexedDB "lp-offline"):
// - "questions": questions saved in advance per exercise, served one by one while offline.
// - "answers":   answers given offline, sent to the server by syncAnswers() once online.
// Every call fails soft: without IndexedDB (private mode, old browser) practice simply
// needs a connection, as before.

const DB_NAME = "lp-offline";
const DB_VERSION = 1;

function openDb() {
    return new Promise((resolve, reject) => {
        if (!("indexedDB" in window)) {
            reject(new Error("IndexedDB unavailable"));
            return;
        }
        const req = indexedDB.open(DB_NAME, DB_VERSION);
        req.onupgradeneeded = () => {
            const db = req.result;
            if (!db.objectStoreNames.contains("questions")) {
                const store = db.createObjectStore("questions", { keyPath: "localId", autoIncrement: true });
                store.createIndex("byExercise", "exerciseKey");
            }
            if (!db.objectStoreNames.contains("answers")) {
                db.createObjectStore("answers", { keyPath: "localId", autoIncrement: true });
            }
        };
        req.onsuccess = () => resolve(req.result);
        req.onerror = () => reject(req.error);
    });
}

function run(storeName, mode, work) {
    return openDb().then((db) => new Promise((resolve, reject) => {
        const tx = db.transaction(storeName, mode);
        const store = tx.objectStore(storeName);
        let result;
        Promise.resolve(work(store)).then((r) => { result = r; });
        tx.oncomplete = () => { db.close(); resolve(result); };
        tx.onerror = () => { db.close(); reject(tx.error); };
        tx.onabort = () => { db.close(); reject(tx.error); };
    }));
}

const asPromise = (req) => new Promise((resolve, reject) => {
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
});

export const exerciseKey = (userId, subject, topic, exercise) => `${userId}|${subject}|${topic}|${exercise}`;

export async function saveQuestions(key, questions) {
    return run("questions", "readwrite", (store) => {
        questions.forEach((q) => store.add({ exerciseKey: key, question: q, savedAt: Date.now() }));
        return questions.length;
    });
}

export async function countQuestions(key) {
    try {
        return await run("questions", "readonly", (store) => asPromise(store.index("byExercise").count(key)));
    } catch {
        return 0;
    }
}

/** Takes (and removes) the oldest saved question for the exercise, or null. */
export async function takeQuestion(key) {
    return run("questions", "readwrite", async (store) => {
        const cursor = await asPromise(store.index("byExercise").openCursor(key));
        if (!cursor) return null;
        const { question } = cursor.value;
        cursor.delete();
        return question;
    });
}

export async function queueAnswer(answer) {
    return run("answers", "readwrite", (store) => store.add({ ...answer, queuedAt: Date.now() }));
}

export async function pendingAnswers() {
    try {
        return await run("answers", "readonly", (store) => asPromise(store.getAll()));
    } catch {
        return [];
    }
}

async function removeAnswer(localId) {
    return run("answers", "readwrite", (store) => store.delete(localId));
}

let syncing = null;

/**
 * Sends every queued answer with `submit(answer)` (which resolves to the server's result
 * data or throws). Answers that fail on the network stay queued for the next try.
 * Resolves to { sent, correct }.
 */
export function syncAnswers(submit) {
    if (syncing) return syncing;
    syncing = (async () => {
        let sent = 0;
        let correct = 0;
        for (const answer of await pendingAnswers()) {
            try {
                const result = await submit(answer);
                await removeAnswer(answer.localId);
                sent += 1;
                if (result?.correct) correct += 1;
            } catch (error) {
                if (!error?.response) break; // still offline: keep the rest queued
                await removeAnswer(answer.localId); // the server rejected it: do not retry forever
            }
        }
        return { sent, correct };
    })().finally(() => { syncing = null; });
    return syncing;
}
