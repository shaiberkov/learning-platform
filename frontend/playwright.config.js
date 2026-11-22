import { defineConfig, devices } from '@playwright/test';

// End-to-end tests (frontend/e2e). They drive a running stack and are skipped unless one is given:
//
//   E2E_BASE_URL=http://localhost:5173 npm run e2e          (frontend already running, backend behind it)
//
// First time on a machine: `npx playwright install chromium`.
// Logging in needs an SMS code, so flows behind login use a JWT taken from a real session
// (cookie `token` in the browser dev tools) in E2E_TEACHER_TOKEN / E2E_STUDENT_TOKEN. See e2e/README.md.
export default defineConfig({
    testDir: './e2e',
    timeout: 60_000,
    fullyParallel: false,
    retries: process.env.CI ? 1 : 0,
    reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
    use: {
        baseURL: process.env.E2E_BASE_URL,
        locale: 'he-IL',
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure',
    },
    projects: [
        { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    ],
});
