import axios from 'axios';
import { LOGOUT, LOGIN, REFRESH_TOKEN, USER_BASE_PATH, VERIFY_OTP } from '../constants/pages.constants.js';
import { AUTH_HEADER } from '../constants/shared.constant.js';

/*
 * Global axios setup for cookie auth. Imported once from main.jsx.
 * - The tokens live in httpOnly cookies set by the server; scripts never read or store them.
 * - withCredentials sends those cookies to the API (a different origin).
 * - X-Requested-With is required by the server on cookie-authenticated writes (CSRF guard).
 * - On a 401 the access token is renewed once through /user/refresh-token and the request retried.
 */
// Remove the JS-readable "token" cookie that older versions stored, so no token lingers in the browser.
document.cookie = 'token=; Max-Age=0; path=/';

axios.defaults.withCredentials = true;
axios.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';

const REFRESH_URL = `${USER_BASE_PATH}${REFRESH_TOKEN}`;
const NO_RETRY_URLS = [REFRESH_URL, `${USER_BASE_PATH}${LOGOUT}`, `${USER_BASE_PATH}${LOGIN}`, `${USER_BASE_PATH}${VERIFY_OTP}`];

// Older screens still build "Authorization: Bearer <cookie value>" headers; with no readable
// cookie that becomes "Bearer undefined". Drop it so the httpOnly cookie is used instead.
axios.interceptors.request.use((config) => {
    const header = config.headers?.[AUTH_HEADER];
    if (typeof header === 'string' && /^Bearer\s*(undefined|null)?\s*$/.test(header)) {
        delete config.headers[AUTH_HEADER];
    }
    return config;
});

let refreshInFlight = null;

/** Renews the access cookie. Parallel callers share one request. Resolves true on success. */
export const refreshSession = () => {
    if (!refreshInFlight) {
        // silentError: a failed refresh just means "not signed in"; the error toaster stays quiet.
        refreshInFlight = axios.post(REFRESH_URL, null, { silentError: true })
            .then((response) => response.data?.success === true)
            .catch(() => false)
            .finally(() => { refreshInFlight = null; });
    }
    return refreshInFlight;
};

axios.interceptors.response.use(
    (response) => response,
    async (error) => {
        const config = error.config;
        const status = error.response?.status;
        const url = config?.url?.split('?')[0];
        if (status !== 401 || !config || config._retried || NO_RETRY_URLS.includes(url)) {
            return Promise.reject(error);
        }
        config._retried = true;
        if (await refreshSession()) {
            return axios(config);
        }
        return Promise.reject(error);
    }
);

/** The Hebrew message the server sent with an error response (e.g. 429 rate limit), if any. */
export const serverErrorMessage = (error) => error?.response?.data?.errorCode || '';
