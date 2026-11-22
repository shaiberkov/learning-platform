import axios from "axios";
import Cookies from "universal-cookie";
import {
    AI_BASE_PATH,
    AI_STAFF_BASE_PATH,
    AI_TUTOR_STREAM,
    AI_TUTOR_TRANSCRIBE,
    AUTH_HEADER,
} from "../../constants/pages.constants.js";
import { BEARER_PREFIX } from "../../constants/shared.constant.js";

// AI endpoints take the user from the JWT, so every call sends the token.
export const authHeaders = () => {
    const token = new Cookies().get("token");
    return { [AUTH_HEADER]: `${BEARER_PREFIX}${token}` };
};

const SESSION_EXPIRED = "יש להתחבר מחדש כדי להשתמש בעוזר ה-AI";
const NETWORK_ERROR = "אין חיבור לשרת. נסו שוב.";

// Thin wrappers that always resolve to { success, errorCode, data }.
const call = async (request) => {
    try {
        const response = await request();
        return response.data;
    } catch (error) {
        const status = error?.response?.status;
        if (status === 401 || status === 403) {
            return { success: false, errorCode: SESSION_EXPIRED };
        }
        return error?.response?.data?.errorCode
            ? error.response.data
            : { success: false, errorCode: NETWORK_ERROR };
    }
};

export const aiGet = (path, params) =>
    call(() => axios.get(`${AI_BASE_PATH}${path}`, { headers: authHeaders(), params }));
export const aiPost = (path, body) =>
    call(() => axios.post(`${AI_BASE_PATH}${path}`, body, { headers: authHeaders() }));
export const aiDelete = (path) =>
    call(() => axios.delete(`${AI_BASE_PATH}${path}`, { headers: authHeaders() }));

export const staffGet = (path, params) =>
    call(() => axios.get(`${AI_STAFF_BASE_PATH}${path}`, { headers: authHeaders(), params }));
export const staffPost = (path, body, params) =>
    call(() => axios.post(`${AI_STAFF_BASE_PATH}${path}`, body ?? {}, { headers: authHeaders(), params }));
export const staffPut = (path, body) =>
    call(() => axios.put(`${AI_STAFF_BASE_PATH}${path}`, body, { headers: authHeaders() }));

export const transcribeAudio = (blob) => {
    const form = new FormData();
    form.append("audio", blob, "speech.webm");
    return call(() => axios.post(`${AI_BASE_PATH}${AI_TUTOR_TRANSCRIBE}`, form, { headers: authHeaders() }));
};

/**
 * Sends one tutor message and reads the server-sent events as they arrive.
 * EventSource cannot POST or send headers, so this reads the fetch body stream by hand.
 * Events: delta {text}, replace {text}, done {remaining}, error {message}.
 */
export async function streamTutor(body, { onDelta, onReplace, onDone, onError, signal }) {
    let response;
    try {
        response = await fetch(`${AI_BASE_PATH}${AI_TUTOR_STREAM}`, {
            method: "POST",
            headers: { ...authHeaders(), "Content-Type": "application/json", Accept: "text/event-stream" },
            body: JSON.stringify(body),
            signal,
        });
    } catch (error) {
        if (error?.name !== "AbortError") onError?.(NETWORK_ERROR);
        return;
    }
    if (response.status === 401 || response.status === 403) {
        onError?.(SESSION_EXPIRED);
        return;
    }
    if (!response.ok || !response.body) {
        onError?.(NETWORK_ERROR);
        return;
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    const handleBlock = (block) => {
        let event = "message";
        const dataLines = [];
        for (const line of block.split(/\r?\n/)) {
            if (line.startsWith("event:")) event = line.slice(6).trim();
            else if (line.startsWith("data:")) dataLines.push(line.slice(5).replace(/^ /, ""));
        }
        if (dataLines.length === 0) return;
        let data;
        try {
            data = JSON.parse(dataLines.join("\n"));
        } catch {
            return;
        }
        if (event === "delta") onDelta?.(data.text);
        else if (event === "replace") onReplace?.(data.text);
        else if (event === "done") onDone?.(data.remaining);
        else if (event === "error") onError?.(data.message);
    };

    try {
        for (;;) {
            const { value, done } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            const blocks = buffer.split(/\r?\n\r?\n/);
            buffer = blocks.pop();
            blocks.forEach(handleBlock);
        }
        if (buffer.trim()) handleBlock(buffer);
    } catch (error) {
        if (error?.name !== "AbortError") onError?.(NETWORK_ERROR);
    }
}

/** Shrinks a photo to at most 1024px on its long side, as a JPEG data URL (keeps uploads small). */
export function resizeImage(file, maxSide = 1024) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onerror = () => reject(new Error("read"));
        reader.onload = () => {
            const img = new Image();
            img.onerror = () => reject(new Error("decode"));
            img.onload = () => {
                const scale = Math.min(1, maxSide / Math.max(img.width, img.height));
                const canvas = document.createElement("canvas");
                canvas.width = Math.round(img.width * scale);
                canvas.height = Math.round(img.height * scale);
                const ctx = canvas.getContext("2d");
                ctx.fillStyle = "#fff";
                ctx.fillRect(0, 0, canvas.width, canvas.height);
                ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
                resolve(canvas.toDataURL("image/jpeg", 0.82));
            };
            img.src = reader.result;
        };
        reader.readAsDataURL(file);
    });
}
