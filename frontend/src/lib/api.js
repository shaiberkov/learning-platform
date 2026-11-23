import axios from "axios";

// Small helpers for the screens that use TanStack Query. The session travels in httpOnly
// cookies (axios withCredentials, Utils/httpSetup.js), so no header is added here. They turn a BasicResponse with success=false into a thrown error whose
// message is the server's Hebrew errorCode.

export class ApiError extends Error {}

export function authConfig(extra = {}) {
    return extra;
}

function unwrap(response) {
    const body = response.data;
    if (body && body.success === false) {
        throw new ApiError(body.errorCode || "");
    }
    return body;
}

/** GET returning BasicResponse.data. */
export async function apiGet(url, params) {
    return unwrap(await axios.get(url, authConfig({ params }))).data;
}

/** POST with query parameters (the backend's @RequestParam style); returns the whole BasicResponse. */
export async function apiPost(url, params, body) {
    return unwrap(await axios.post(url, body ?? null, authConfig({ params })));
}

/** POST multipart/form-data (attachments); fields go in the form body, not the URL. */
export async function apiPostForm(url, fields) {
    const form = new FormData();
    Object.entries(fields).forEach(([key, value]) => {
        if (value !== undefined && value !== null) form.append(key, value);
    });
    return unwrap(await axios.post(url, form, authConfig()));
}

/** Downloads a protected file (sent with the JWT) and hands it to the browser. */
export async function downloadFile(url, params, fileName) {
    const response = await axios.get(url, authConfig({ params, responseType: "blob" }));
    const href = URL.createObjectURL(response.data);
    const a = document.createElement("a");
    a.href = href;
    a.download = fileName || "file";
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(href), 1000);
}

export function errorMessage(error, fallback) {
    if (error instanceof ApiError && error.message) return error.message;
    return fallback;
}
