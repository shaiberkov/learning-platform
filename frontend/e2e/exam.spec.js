import { expect, test } from '@playwright/test';
import { requireEnv, signInWithToken } from './helpers.js';

// The exam lifecycle: a teacher creates an exam for a student, the student takes and submits it,
// and the grade shows up in the student's tests table. Runs serially; each step needs the previous one.
// Skeleton: selectors follow the current screens (TestConfigurator, TestsTable, TestSession) and
// will need updating when those screens change.
test.describe.serial('exam lifecycle', () => {
    requireEnv('E2E_TEACHER_TOKEN', 'E2E_STUDENT_ID', 'E2E_STUDENT_TOKEN');

    test('teacher creates an exam', async ({ page }) => {
        await signInWithToken(page, process.env.E2E_TEACHER_TOKEN);
        await page.goto('/test-configurator');
        await expect(page.getByRole('heading', { name: /יצירת מבחן/ })).toBeVisible();

        // subject -> topic -> difficulty -> question count: pick the first real option of each select in turn.
        for (let i = 0; i < 4; i++) {
            const select = page.locator('select').nth(i);
            await expect(select).toBeVisible();
            await select.selectOption({ index: 1 });
        }

        // Start time (react-datepicker): open it and take the first selectable time slot, which starts the
        // exam soon enough for the next step. Adjust if the date picker's format or layout changes.
        await page.getByPlaceholder('בחר תאריך ושעה').click();
        await page.locator('.react-datepicker__time-list-item:not(.react-datepicker__time-list-item--disabled)').first().click();

        await page.getByPlaceholder('123456789,987654321,...').fill(process.env.E2E_STUDENT_ID);

        await page.getByRole('button', { name: 'צור מבחן' }).click();
        await expect(page.locator('.notice')).toBeVisible();
    });

    test('student takes and submits the exam', async ({ page }) => {
        await signInWithToken(page, process.env.E2E_STUDENT_TOKEN);
        await page.goto('/teacher-tests');
        await expect(page.getByRole('columnheader', { name: /ציון/ })).toBeVisible();

        // The newest exam that can be started (it must already be inside its start window).
        const start = page.getByRole('button', { name: /התחל/ }).first();
        test.skip(!(await start.isVisible()), 'No exam is open for this student right now');
        await start.click();

        // Answer every question with "0"; correctness does not matter for the flow.
        const answers = page.locator('input[type="text"], input[type="number"]');
        for (let i = 0; i < await answers.count(); i++) {
            await answers.nth(i).fill('0');
        }
        await page.getByRole('button', { name: 'הגש מבחן' }).click();
        await page.getByRole('button', { name: 'הגש', exact: true }).click();
    });

    test('student sees the grade', async ({ page }) => {
        await signInWithToken(page, process.env.E2E_STUDENT_TOKEN);
        await page.goto('/teacher-tests');
        await expect(page.locator('table .num').first()).toBeVisible();
    });
});
