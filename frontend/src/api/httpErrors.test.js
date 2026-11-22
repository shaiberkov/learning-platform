import { describe, expect, it } from 'vitest';
import {
    DEFAULT_ERROR_MESSAGE,
    NETWORK_ERROR_MESSAGE,
    TIMEOUT_ERROR_MESSAGE,
    getErrorMessage,
    getFieldErrors,
} from './httpErrors.js';

const problem = (status, data) => ({ response: { status, data } });

describe('getErrorMessage', () => {
    it('uses the Hebrew detail of an RFC 7807 problem', () => {
        const error = problem(400, { type: 'urn:learning-app:problem:validation', detail: 'יש להזין תעודת זהות' });
        expect(getErrorMessage(error)).toBe('יש להזין תעודת זהות');
    });

    it('falls back to errorCode for BasicResponse-shaped error bodies', () => {
        expect(getErrorMessage(problem(409, { success: false, errorCode: 'המשתמש כבר קיים' }))).toBe('המשתמש כבר קיים');
    });

    it('maps a status without a body to a Hebrew message', () => {
        expect(getErrorMessage(problem(403, ''))).toBe('אין לך הרשאה לבצע פעולה זו.');
        expect(getErrorMessage(problem(500, '<html>Whitelabel</html>'))).toBe('אירעה שגיאה בשרת. נסו שוב בעוד מספר רגעים.');
    });

    it('uses the fallback for an unknown status', () => {
        expect(getErrorMessage(problem(418, null))).toBe(DEFAULT_ERROR_MESSAGE);
        expect(getErrorMessage(problem(418, null), 'משהו')).toBe('משהו');
    });

    it('recognises network failures and timeouts', () => {
        expect(getErrorMessage({ code: 'ERR_NETWORK', request: {} })).toBe(NETWORK_ERROR_MESSAGE);
        expect(getErrorMessage({ code: 'ECONNABORTED', request: {} })).toBe(TIMEOUT_ERROR_MESSAGE);
    });

    it('never throws on odd input', () => {
        expect(getErrorMessage(undefined)).toBe(DEFAULT_ERROR_MESSAGE);
        expect(getErrorMessage(new Error('boom'))).toBe(DEFAULT_ERROR_MESSAGE);
    });
});

describe('getFieldErrors', () => {
    it('returns the field map of a validation problem', () => {
        const error = problem(400, { errors: { userId: 'יש להזין תעודת זהות', password: 'שדה חובה חסר' } });
        expect(getFieldErrors(error)).toEqual({ userId: 'יש להזין תעודת זהות', password: 'שדה חובה חסר' });
    });

    it('returns an empty object otherwise', () => {
        expect(getFieldErrors(problem(500, { detail: 'x' }))).toEqual({});
        expect(getFieldErrors(problem(400, { errors: ['a'] }))).toEqual({});
        expect(getFieldErrors(undefined)).toEqual({});
    });
});
