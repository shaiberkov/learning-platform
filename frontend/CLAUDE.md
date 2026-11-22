# frontend/CLAUDE.md

React 19 + Vite 6, plain JavaScript (`.jsx`, no TypeScript), Tailwind 4 (daisyUI is installed but not loaded), axios, react-router 7. Hebrew UI.

## Commands (run from `frontend/`)

| What | Command |
|---|---|
| Install | `npm install` |
| Dev server | `npm run dev` (`http://localhost:5173`, the only dev origin the backend's CORS allows) |
| Lint | `npm run lint` |
| Production build | `npm run build` → `dist/` |
| Unit tests | `npm test` (Vitest + Testing Library, `src/**/*.test.{js,jsx}`) |
| E2E | `E2E_BASE_URL=http://localhost:5173 npm run e2e` (Playwright, `e2e/`; skipped without a stack, see `e2e/README.md`) |

Unit tests cover helpers and small components; still verify a screen change against the backend in the browser.

## Which API it talks to

`src/constants/base.constants.js` sets `API_BASE_URL` from `VITE_API_URL`: `npm run dev` reads `http://localhost:8080` from `.env.development`; a build without the variable uses the deployed server (`https://learning-project-server.onrender.com`). Override per machine in `.env.local` (see `.env.example`); never edit the file to switch. Every endpoint is a constant in `src/constants/pages.constants.js` built as `${API_BASE_URL}${BASE_API}/...`; components import those constants instead of writing URLs. A new or renamed backend endpoint changes `backend/.../constants/ControllerConstants.java` and this file together.

## Layout

- `src/routes/AppRoutes.jsx`: every route. Routes for a role exist only inside a `user?.role === '...'` block, so a page for a role goes in that role's block. A student without a `schoolCode` gets a reduced set. `components/ProtectedRoute.jsx` (with `allowedRoles`) exists but is not used by `AppRoutes`.
- `src/context/UserContext.jsx`: reads the JWT from the `token` cookie (`universal-cookie`), validates it against the session endpoint on load and sets `user` (`userId`, `role`, `schoolCode`, ...). Without a token it sends the user to `/login`, except on the public pages it lists. A new public page is added to that list too.
- `src/context/NotificationContext.jsx`: one `EventSource` to `/notifications/connect?userId=...`, handling `SYSTEM_MESSAGE` and `NEW_TEST` events. New live event types are handled here, matching `backend/.../notification/NotificationType.java`.
- `src/features/auth/`: Login → TwoFactorAuthForm (sets the `token` cookie) → dashboard; Register; ResetPassword.
- `src/pages/<role>/`: screens per role (`admin`, `manager`, `teacher`, `student`, plus `footer` static pages).
- `src/components/`: shared pieces (SideBar, WeeklySchedule, TestConfigurator, TestSession, ChatComponent, messages/).
- `src/constants/`: routes, API paths, messages, styles and assets. Hebrew strings shown to users live here or inline, matching the file being edited.

## Rules

- Requests: axios with `Authorization: Bearer <token>` (`AUTH_HEADER` + `BEARER_PREFIX` constants). Read `response.data.success`; show `response.data.errorCode` as the message (it is Hebrew text from the server).
- Failed requests (HTTP 4xx/5xx with an RFC 7807 body, network errors) are handled once: the interceptor in `src/api/setupHttp.js` (installed in `main.jsx`) shows the Hebrew message through `components/ui/ErrorToaster.jsx` and sets `error.userMessage` / `error.fieldErrors` for the component's own `catch`. Do not add generic `alert(...)`s; use `getErrorMessage` / `getFieldErrors` from `src/api/httpErrors.js` when a screen needs the text, and `{ silentError: true }` in the axios config for requests that must not toast.
- Right-to-left: `index.html` is `lang="he" dir="rtl"`, and each page's root element still carries `dir="rtl"`. A new screen sets it on its root too.
- Styling: the design system lives in `src/App.css` ("Spatial Academy"). Colors are CSS tokens (`--ink`, `--accent`, `--mint`, `--amber`, `--danger`, `--surface`...) redefined per theme under `[data-theme="light"|"dark"]` on `<html>`, and exposed to Tailwind as `text-ink`, `bg-accent-soft`, `border-line` and so on. Never use raw Tailwind palette colors (`bg-white`, `text-gray-700`, `green-500`): they do not follow the theme.
- Build screens from the component classes in `App.css`: `glass` (+ `tile` for hover lift), `eyebrow`, `display`, `title`, `field` / `field-wrap` / `label` / `field-error`, `btn btn-primary|btn-ghost|btn-danger|btn-mint`, `choice is-active`, `notice`, `chip`, `row-card`, `data-table`, `modal-backdrop` + `modal`, `reveal` (staggered entrance via `--i`). Numbers, timers and grades use `num` (JetBrains Mono).
- Shared UI pieces in `src/components/ui/`: `FormShell` (management forms), `AuthShell` (auth screens), `DashboardHero` + `QuickTile` (bento dashboards), `ProsePage` (static pages), `Prism` (CSS-only 3D prism), `ProgressRing`, `OrbitLoader`, `ThemeDial` (theme toggle, stores `lp-theme` in localStorage; `index.html` applies it before first paint).
- react-select instances use `classNamePrefix="lp-select"`, themed in `App.css`. The practice canvas stays white in both themes on purpose (it is drawn on with black ink).
- No client cache: screens fetch on mount. The SSE context is the only place server pushes arrive.
