# frontend/CLAUDE.md

React 19 + Vite 6, plain JavaScript (`.jsx`, no TypeScript), Tailwind 4 with daisyUI, axios, react-router 7. Hebrew UI.

## Commands (run from `frontend/`)

| What | Command |
|---|---|
| Install | `npm install` |
| Dev server | `npm run dev` (`http://localhost:5173`, the only dev origin the backend's CORS allows) |
| Lint | `npm run lint` |
| Production build | `npm run build` → `dist/` |

There are no frontend tests. Verify a change by running it against the backend and checking the screen in the browser.

## Which API it talks to

`src/constants/base.constants.js` sets `API_BASE_URL`: the deployed server (`https://learning-project-server.onrender.com`) in commits, `http://localhost:8080` when working locally. Switch it locally, never commit the local value. Every endpoint is a constant in `src/constants/pages.constants.js` built as `${API_BASE_URL}${BASE_API}/...`; components import those constants instead of writing URLs. A new or renamed backend endpoint changes `backend/.../constants/ControllerConstants.java` and this file together.

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
- Right-to-left is set per screen: each page's root element carries `dir="rtl"` (`index.html` is `lang="en"` with no `dir`). A new screen sets it on its root too.
- Styling: Tailwind utility classes and daisyUI components, no new CSS files.
- No client cache: screens fetch on mount. The SSE context is the only place server pushes arrive.
