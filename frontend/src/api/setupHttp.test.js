import axios from 'axios';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { handleHttpError, subscribeToHttpErrors } from './setupHttp.js';

// A fake adapter: the request "reaches the server" and comes back with the given status and body.
const respondWith = (status, data) => (config) =>
    Promise.reject(Object.assign(new Error(`Request failed with status code ${status}`), {
        config,
        request: {},
        response: { status, data, headers: {}, config },
        isAxiosError: true,
    }));

const clientReturning = (status, data) => {
    const client = axios.create({ adapter: respondWith(status, data) });
    client.interceptors.response.use((response) => response, handleHttpError);
    return client;
};

describe('http error interceptor', () => {
    const unsubscribers = [];
    afterEach(() => unsubscribers.splice(0).forEach((unsubscribe) => unsubscribe()));

    it('announces the Hebrew message and still rejects', async () => {
        const listener = vi.fn();
        unsubscribers.push(subscribeToHttpErrors(listener));
        const client = clientReturning(400, { detail: 'מספר השאלות חייב להיות לפחות 1', errors: { questionCount: 'מספר השאלות חייב להיות לפחות 1' } });

        const error = await client.get('/x').catch((e) => e);

        expect(listener).toHaveBeenCalledWith('מספר השאלות חייב להיות לפחות 1');
        expect(error.userMessage).toBe('מספר השאלות חייב להיות לפחות 1');
        expect(error.fieldErrors).toEqual({ questionCount: 'מספר השאלות חייב להיות לפחות 1' });
    });

    it('stays quiet for requests marked silentError', async () => {
        const listener = vi.fn();
        unsubscribers.push(subscribeToHttpErrors(listener));
        const client = clientReturning(500, { detail: 'שגיאה' });

        const error = await client.get('/x', { silentError: true }).catch((e) => e);

        expect(listener).not.toHaveBeenCalled();
        expect(error.userMessage).toBe('שגיאה');
    });
});
