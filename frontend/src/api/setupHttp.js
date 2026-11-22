import axios from 'axios';
import { getErrorMessage, getFieldErrors } from './httpErrors.js';

// Every component imports the default axios instance, so one interceptor on it covers the whole app.
// A failed request (HTTP 4xx/5xx, network down, timeout) gets `error.userMessage` and `error.fieldErrors`
// and is announced to the subscribers (ErrorToaster shows it). The error is still rejected, so the
// component's own catch keeps working. A request can opt out of the toast with `{ silentError: true }`.

const listeners = new Set();

export function subscribeToHttpErrors(listener) {
    listeners.add(listener);
    return () => listeners.delete(listener);
}

export function notifyHttpError(message) {
    listeners.forEach((listener) => listener(message));
}

export function handleHttpError(error) {
    if (axios.isCancel(error)) return Promise.reject(error);

    const message = getErrorMessage(error);
    if (error && typeof error === 'object') {
        error.userMessage = message;
        error.fieldErrors = getFieldErrors(error);
    }
    if (!error?.config?.silentError) {
        notifyHttpError(message);
    }
    return Promise.reject(error);
}

let installedId = null;

export function installHttpErrorInterceptor(instance = axios) {
    if (installedId !== null) return installedId;
    installedId = instance.interceptors.response.use((response) => response, handleHttpError);
    return installedId;
}
