// Turns any failed request into one Hebrew message, the same way on every screen.
//
// The backend answers errors in two shapes:
//  - BasicResponse with HTTP 200: { success: false, errorCode: "<Hebrew text>" } (business errors;
//    screens keep handling these themselves, they never reach the interceptor),
//  - RFC 7807 ProblemDetail (application/problem+json) with HTTP 4xx/5xx:
//    { type, title, status, detail: "<Hebrew text>", errors?: { field: "<Hebrew text>" }, errorCode, success: false }.

export const DEFAULT_ERROR_MESSAGE = 'אירעה שגיאה. נסו שוב בעוד מספר רגעים.';
export const NETWORK_ERROR_MESSAGE = 'אין חיבור לשרת. בדקו את החיבור לאינטרנט ונסו שוב.';
export const TIMEOUT_ERROR_MESSAGE = 'השרת לא הגיב בזמן. נסו שוב.';

const STATUS_MESSAGES = {
    401: 'יש להתחבר מחדש.',
    403: 'אין לך הרשאה לבצע פעולה זו.',
    404: 'המשאב המבוקש לא נמצא.',
    413: 'הבקשה גדולה מדי.',
    429: 'יותר מדי בקשות. המתינו רגע ונסו שוב.',
    500: 'אירעה שגיאה בשרת. נסו שוב בעוד מספר רגעים.',
    502: 'השרת אינו זמין כרגע. נסו שוב בעוד מספר רגעים.',
    503: 'השרת אינו זמין כרגע. נסו שוב בעוד מספר רגעים.',
};

const isText = (value) => typeof value === 'string' && value.trim() !== '';

/** Field errors from a ProblemDetail body ({ field: message }), or {} when there are none. */
export function getFieldErrors(error) {
    const errors = error?.response?.data?.errors;
    return errors && typeof errors === 'object' && !Array.isArray(errors) ? errors : {};
}

/** The Hebrew message to show for a failed axios request (or any thrown value). */
export function getErrorMessage(error, fallback = DEFAULT_ERROR_MESSAGE) {
    if (!error) return fallback;

    const response = error.response;
    if (response) {
        const data = response.data;
        if (data && typeof data === 'object') {
            if (isText(data.detail)) return data.detail;
            if (isText(data.errorCode)) return data.errorCode;
            if (isText(data.message)) return data.message;
        }
        if (isText(data) && !data.trim().startsWith('<')) return data;
        return STATUS_MESSAGES[response.status] ?? fallback;
    }

    if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') return TIMEOUT_ERROR_MESSAGE;
    if (error.request || error.code === 'ERR_NETWORK') return NETWORK_ERROR_MESSAGE;
    return fallback;
}
