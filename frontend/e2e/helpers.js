import { test } from '@playwright/test';

/**
 * Skips the enclosing describe block unless a stack is given (E2E_BASE_URL) and every named
 * environment variable is set. Call it at describe level: the skip is decided before a browser
 * starts, so `npm run e2e` passes (all skipped) on a machine without a stack or browsers.
 */
export function requireEnv(...names) {
    test.skip(!process.env.E2E_BASE_URL, 'E2E_BASE_URL is not set: no running stack to test against');
    const missing = names.filter((name) => !process.env[name]);
    test.skip(missing.length > 0, `Missing ${missing.join(', ')}`);
}

/** Signs the browser in by planting the JWT cookie the app reads (UserContext validates it on load). */
export async function signInWithToken(page, token) {
    const url = new URL(process.env.E2E_BASE_URL);
    await page.context().addCookies([{ name: 'token', value: token, domain: url.hostname, path: '/' }]);
}
