# End-to-end tests (Playwright)

Specs drive a running stack in a real browser. Without `E2E_BASE_URL` every spec is skipped, so `npm run e2e` is safe to run anywhere.

```
npx playwright install chromium          # once per machine
E2E_BASE_URL=http://localhost:5173 npm run e2e
```

| Variable | Used by | Notes |
|---|---|---|
| `E2E_BASE_URL` | all | the frontend URL (local dev server or a staging deploy, never production) |
| `E2E_USER_ID`, `E2E_PASSWORD` | `login.spec.js` | a test account; the last login test sends it a real SMS |
| `E2E_TEACHER_TOKEN`, `E2E_STUDENT_TOKEN` | `exam.spec.js` | JWTs of a test teacher and test student, copied from the `token` cookie after logging in by hand (login needs an SMS code) |
| `E2E_STUDENT_ID` | `exam.spec.js` | the test student's id, the teacher assigns the exam to it |

The exam spec is a skeleton: it follows today's screens (`TestConfigurator`, `TestsTable`, `TestSession`) and its selectors need updating when they change. A teacher exam only opens at its start time, so the "takes and submits" step skips itself when nothing is open.
