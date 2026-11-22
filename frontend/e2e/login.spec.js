import { expect, test } from '@playwright/test';
import { requireEnv } from './helpers.js';

test.describe('login page', () => {
    requireEnv();

    test('shows the login form', async ({ page }) => {
        await page.goto('/login');
        await expect(page.getByRole('heading', { name: 'התחברות' })).toBeVisible();
        await expect(page.getByRole('button', { name: 'התחבר' })).toBeDisabled();
    });
});

test.describe('login with a test account', () => {
    requireEnv('E2E_USER_ID', 'E2E_PASSWORD');

    test('rejects a wrong password with the server message', async ({ page }) => {
        await page.goto('/login');
        await page.getByPlaceholder('תעודת זהות').fill(process.env.E2E_USER_ID);
        await page.locator('input[name="password"]').fill('definitely-not-the-password');
        await page.getByRole('button', { name: 'התחבר' }).click();
        await expect(page.getByText(/סיסמה/).first()).toBeVisible();
        await expect(page).toHaveURL(/\/login$/);
    });

    test('correct credentials move on to the SMS step', async ({ page }) => {
        // Sends a real SMS to the account's phone: use a test account.
        await page.goto('/login');
        await page.getByPlaceholder('תעודת זהות').fill(process.env.E2E_USER_ID);
        await page.locator('input[name="password"]').fill(process.env.E2E_PASSWORD);
        await page.getByRole('button', { name: 'התחבר' }).click();
        await expect(page).toHaveURL(/\/two-factor-auth/);
    });
});
