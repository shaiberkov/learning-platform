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

- `src/routes/AppRoutes.jsx`: every route, each page loaded with `React.lazy` (one chunk per page, `PageSkeleton` while it loads). `/messages`, `/settings` exist for every role; `/accessibility` is public. Routes for a role exist only inside a `user?.role === '...'` block, so a page for a role goes in that role's block. A student without a `schoolCode` gets a reduced set. `components/ProtectedRoute.jsx` (with `allowedRoles`) exists but is not used by `AppRoutes`.
- `src/context/UserContext.jsx`: the tokens are httpOnly cookies the page cannot read; it calls the session endpoint on every navigation (refreshing once through `/user/refresh-token` if needed) and sets `user` (`userId`, `role`, `schoolCode`, ...). Without a token it sends the user to `/login`, except on the public pages it lists. A new public page is added to that list too.
- `src/context/NotificationContext.jsx`: one `EventSource` to `/notifications/connect?userId=...`, handling `SYSTEM_MESSAGE`, `NEW_TEST`, `NEW_HOMEWORK` and `HOMEWORK_REMINDER` events. New live event types are handled here, matching `backend/.../notification/NotificationType.java`.
- `src/features/auth/`: Login → TwoFactorAuthForm (sets the `token` cookie) → dashboard; Register; ResetPassword.
- `src/pages/<role>/`: screens per role (`admin`, `manager`, `teacher`, `student`, plus `footer` static pages).
- `src/components/`: shared pieces (SideBar, WeeklySchedule, TestConfigurator, TestSession, ChatComponent, messages/).
- `src/constants/`: routes, API paths, messages, styles and assets. Hebrew strings shown to users live here or inline, matching the file being edited.

## Rules

- Requests: plain axios. `src/Utils/httpSetup.js` (imported by `main.jsx`) sets `withCredentials`, the `X-Requested-With` header the server requires on cookie-authenticated writes, and a 401 → refresh → retry interceptor. Do not read or store tokens in JS; old screens still build `Bearer undefined` headers, which the interceptor drops. Errors with a body (for example 429 rate limits) carry a Hebrew `errorCode`: show it with `serverErrorMessage(error)`. Read `response.data.success`; show `response.data.errorCode` as the message (it is Hebrew text from the server).
- Failed requests (HTTP 4xx/5xx with an RFC 7807 body, network errors) are handled once: the interceptor in `src/api/setupHttp.js` (installed in `main.jsx`) shows the Hebrew message through `components/ui/ErrorToaster.jsx` and sets `error.userMessage` / `error.fieldErrors` for the component's own `catch`. Do not add generic `alert(...)`s; use `getErrorMessage` / `getFieldErrors` from `src/api/httpErrors.js` when a screen needs the text, and `{ silentError: true }` in the axios config for requests that must not toast.
- Right-to-left: `index.html` is `lang="he" dir="rtl"`, and each page's root element still carries `dir="rtl"`. A new screen sets it on its root too.
- Styling: the design system lives in `src/App.css` ("Spatial Academy"). Colors are CSS tokens (`--ink`, `--accent`, `--mint`, `--amber`, `--danger`, `--surface`...) redefined per theme under `[data-theme="light"|"dark"]` on `<html>`, and exposed to Tailwind as `text-ink`, `bg-accent-soft`, `border-line` and so on. Never use raw Tailwind palette colors (`bg-white`, `text-gray-700`, `green-500`): they do not follow the theme.
- Build screens from the component classes in `App.css`: `glass` (+ `tile` for hover lift), `eyebrow`, `display`, `title`, `field` / `field-wrap` / `label` / `field-error`, `btn btn-primary|btn-ghost|btn-danger|btn-mint`, `choice is-active`, `notice`, `chip`, `row-card`, `data-table`, `modal-backdrop` + `modal`, `reveal` (staggered entrance via `--i`). Numbers, timers and grades use `num` (JetBrains Mono).
- Shared UI pieces in `src/components/ui/`: `FormShell` (management forms), `AuthShell` (auth screens), `DashboardHero` + `QuickTile` (bento dashboards), `ProsePage` (static pages), `Prism` (CSS-only 3D prism), `ProgressRing`, `OrbitLoader`, `ThemeDial` (theme toggle, stores `lp-theme` in localStorage; `index.html` applies it before first paint).
- react-select instances use `classNamePrefix="lp-select"`, themed in `App.css`. The practice canvas stays white in both themes on purpose (it is drawn on with black ink).
- Data fetching: older screens use axios in `useEffect`; new screens use TanStack Query with the helpers in `src/lib/api.js` (`apiGet`, `apiPost`, `apiPostForm`, which send the JWT and throw the server's Hebrew `errorCode`). `staleTime` is 0, so screens still fetch on mount. The SSE context is the only place server pushes arrive; it reconnects with backoff and fetches missed notifications.
- i18n: `src/i18n/` (react-i18next), languages `he` (default), `ar`, `en`; `<html lang/dir>` and axios `Accept-Language` follow the choice. Use `t()` and `i18n.dir()` in shell components; most pages are still Hebrew-only.
- Accessibility (IS 5568 / WCAG 2.1 AA): every control has a visible label or `aria-label`, dialogs close with Esc and return focus, loading uses skeletons (`ui/Skeleton.jsx`) with a polite status, empty lists use `ui/EmptyState.jsx`. Check public pages with axe-core (dev dependency).
- PWA: `vite.config.js` (`vite-plugin-pwa`, injectManifest) + `src/sw.js` (precache, navigation fallback, Web Push). The service worker runs only in production builds. Icons come from `scripts/generate-pwa-icons.mjs`.
