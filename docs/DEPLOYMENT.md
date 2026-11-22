# Deployment, environments and monitoring

Both apps deploy to Render from this repository. Nothing in this file is automated yet on the Render / GitHub / Sentry side: those steps need the account owner and are listed here as a checklist.

## Configuration

Settings come from Spring profiles plus environment variables (backend) and build-time `VITE_*` variables (frontend). No secret is committed; the full lists with comments are in `backend/.env.example` and `frontend/.env.example`.

| Backend variable | Needed | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | no | defaults to `production` (`application.properties`); add `json-logs` for JSON console logs: `production,json-logs` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | yes | MySQL (`production` profile) |
| `JPA_DDL_AUTO` | no | default `validate`; `none` only as an emergency fallback (see `docs/BACKUP.md`) |
| `GPT_TOKEN`, `SMS_TOKEN` | for those features | OpenAI and SMS provider keys |
| `SENTRY_DSN`, `SENTRY_ENVIRONMENT`, `SENTRY_TRACES_SAMPLE_RATE` | no | error reporting; empty DSN = off |

| Frontend variable (build time) | Purpose |
|---|---|
| `VITE_API_URL` | API server; unset = `https://learning-project-server.onrender.com`. `npm run dev` uses `http://localhost:8080` from `.env.development` |
| `VITE_SENTRY_DSN`, `VITE_SENTRY_ENVIRONMENT`, `VITE_SENTRY_TRACES_SAMPLE_RATE` | error reporting; empty DSN = the SDK is never loaded |

`VITE_*` values are baked into the JavaScript bundle at build time: they are public. Never put a secret in one.

## Monitoring

- **Health**: `GET /actuator/health` (public) returns `{"status":"UP"}` with the database check; `/actuator/health/liveness` and `/actuator/health/readiness` are the probe groups. Use it as Render's health check path.
- **Info / metrics**: `GET /actuator/info` and `GET /actuator/metrics[/<name>]` need `Authorization: Bearer <JWT of a SYSTEM_ADMIN>`. Example: `/actuator/metrics/http.server.requests`.
- **Logs**: plain text by default. With the `json-logs` profile, console logs are JSON (Elastic Common Schema), ready for a log service.
- **Errors**: Sentry (backend `io.sentry:sentry-spring-boot-starter-jakarta`, frontend `@sentry/react`), active only when a DSN is set. PII is not sent (`send-default-pii=false`).
- **Uniform API errors**: validation and unexpected failures come back as RFC 7807 `application/problem+json` with a Hebrew `detail`; the frontend shows them in one toast component.

## CI

`.github/workflows/ci.yml` runs on every push to `main` and every pull request:
- backend: `./mvnw test` (unit tests + Testcontainers MySQL integration tests, which also prove the Flyway migrations match the entities) and `package`;
- frontend: `npm ci`, lint (new code blocking, whole app report-only until the old lint errors are fixed), `npm test`, `npm run build`.

Playwright E2E (`frontend/e2e`) is not in CI yet: it needs a running stack (see "Preview environments" below) and test-account secrets.

## Checklist for the account owner (not done)

### 1. Connect Render to this repository
The two Render services were created from the old repositories (`Learning-Project-Server`, `SchoolManagmentSystem`). For each service, in the Render dashboard:
1. Settings → Build & Deploy → Repository: connect this monorepo.
2. Root Directory: `backend` for the API (Docker runtime, `backend/Dockerfile`), `frontend` for the static site.
3. Static site: Build Command `npm ci && npm run build`, Publish Directory `dist`, and a rewrite rule `/*` → `/index.html` (client-side routes).
4. Environment: copy the variables above; for the API set Health Check Path `/actuator/health`.
5. Auto-deploy only from `main`, and enable "Wait for CI to pass" (or deploy hooks triggered from the CI workflow).
6. Before the first deploy with Flyway: do the schema check in `docs/BACKUP.md` ("Before the first production deploy").

### 2. GitHub
1. Settings → Branches: protect `main`, require the `Backend tests` and `Frontend lint, test, build` checks.
2. Settings → Secrets and variables → Actions: only if E2E runs in CI later (`E2E_BASE_URL`, `E2E_*_TOKEN`).

### 3. Staging
1. Create a second MySQL database (`learning_staging`) and restore a recent backup into it (`scripts/restore-db.sh`), or let Flyway build it empty.
2. Duplicate both Render services as `*-staging`, deploying from a `staging` branch, with their own `DB_URL`, `SENTRY_ENVIRONMENT=staging`, and the static site's `VITE_API_URL` pointing at the staging API.
3. Add the staging frontend origin to `SecurityConstants.ALLOWED_ORIGINS` (CORS) in the same change.

### 4. Preview environments per branch
Render "Preview Environments" (Blueprint, `render.yaml`) or "Pull Request Previews" on the static site create a deploy per pull request. Needs a `render.yaml` describing both services, a shared staging database (never production), and a CORS rule that accepts the preview origins. Then the Playwright suite can run against each preview with `E2E_BASE_URL`.

### 5. Sentry
Create two projects (Java/Spring Boot and React), put the DSNs in `SENTRY_DSN` (Render API) and `VITE_SENTRY_DSN` (Render static site build environment), set `SENTRY_ENVIRONMENT` per environment.

### 6. Backups
Schedule `scripts/backup-db.sh` daily on a machine that can reach the production database and keep the dumps off that machine; run the restore test monthly (`docs/BACKUP.md`).
