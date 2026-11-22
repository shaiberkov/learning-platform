# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

## The project

A school management and math-practice platform. One repository, two apps:

| Folder | What | Stack | Details |
|---|---|---|---|
| `backend/` | REST API + SSE notifications | Java 17, Spring Boot 3.4, Spring Data JPA, MySQL, Ehcache 3 (JCache), JJWT | `backend/CLAUDE.md` |
| `frontend/` | Web app, one dashboard per role | React 19, Vite 6, Tailwind 4 + daisyUI, axios, react-router 7 | `frontend/CLAUDE.md` |

`docs/SPEC.md` describes the system: roles, domain model, API surface, the main flows. `docs/CACHING.md` maps every server cache. Both repos were merged here with their full history (`Learning-Project-Server` → `backend/`, `SchoolManagmentSystem` → `frontend/`), so `git log -- backend/...` and `git blame` work on old code.

Four roles, in this order of power: `SYSTEM_ADMIN` → `SCHOOLMANAGER` → `TEACHER` → `STUDENT` (`backend/.../enums/Role.java`). The UI and the API both speak Hebrew to users; keep user-facing strings in Hebrew and code in English.

## Working across both apps

- The API contract lives in two places that must agree: the backend paths in `backend/.../constants/ControllerConstants.java` (all under `BASE_API = "/Learning-App"`) and their mirror in `frontend/src/constants/pages.constants.js` (`${API_BASE_URL}${BASE_API}/...`). A change to an endpoint, its parameters or its response shape changes both sides in the same commit.
- Responses are mostly `BasicResponse` (`success`, `errorCode`, `data`). The frontend reads `success` and shows `errorCode` as the message, so `errorCode` holds Hebrew text, not a code.
- Run locally: backend with the `dev` profile on `:8080` (local MySQL, database `learning`), frontend with `npm run dev` on `:5173` pointed at `http://localhost:8080`. `.claude/launch.json` has both (`backend-dev`, `frontend`). The backend's `-Dspring-boot.run.profiles=dev` overrides `spring.profiles.active=production` in `application.properties`, so that file need not change. The frontend reads `VITE_API_URL`: `npm run dev` gets `http://localhost:8080` from `frontend/.env.development`, a build without it uses the deployed server. Never commit `spring.profiles.active` switched to local: production (Render) builds from what is committed. Environment variables for both apps are listed in `backend/.env.example` and `frontend/.env.example`; deployment, monitoring and CI are in `docs/DEPLOYMENT.md`, migrations and backups in `docs/BACKUP.md`.
- CORS allows only `http://localhost:5173` and the deployed frontend (`SecurityConstants.ALLOWED_ORIGINS`). A new dev port needs an entry there.

## Security

- Roles are enforced per route group in `SecurityConfig.java` (`hasRole` / `hasAnyRole`, including `PARENT` for `/parent/**`; `/user/**`, `/validate-token/**`, Swagger, `/actuator/health`, the SSE stream `/notifications/connect`, the Web Push public key and the `.ics` feed `/calendar/feed/**` stay `permitAll()`). The session is an httpOnly `access_token` cookie (an `Authorization: Bearer` header still works for Swagger and tests). Ownership is checked on each controller method with `@PreAuthorize("@authz....")` (`security/AccessGuard`): a new endpoint that takes a `userId`, `schoolCode`, `teacherId`, `studentId` or `testId` needs a matching check, or any logged-in user can pass someone else's id. A failed check answers 403 with an RFC 7807 body carrying `success: false` and the Hebrew `errorCode` (`security/AccessDeniedAdvice`).
- Sensitive writes (role changes, class and timetable changes, recorded scores, impersonation, feature flags) call `AuditService.record(...)` (`audit_log` table). Per-school feature flags are checked through `security/FeatureFlags` (`@features.enabledForCurrentUser('...')` on an endpoint).
- Secrets come from the environment (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GPT_TOKEN`, `SMS_TOKEN`, read through `Environment` / `spring-dotenv` from `backend/.env`). Never put a secret in a committed file: the repository is public and GitHub push protection rejects pushes that contain keys. `application-dev.properties` reads the local MySQL password as `${DB_PASSWORD}` from `backend/.env`. An OpenAI key and that password were scrubbed from the history when the repositories were merged.

## Caching

All server caching is mapped in `docs/CACHING.md`. Rules:
- Any change that reads or writes data checks `docs/CACHING.md` first and updates it in the same change. A PostToolUse hook (`.claude/hooks/cache_reminder.py`) reminds you after edits to service, repository and entity code.
- Every cache name used in code (`@Cacheable`, `@CacheEvict`, `cacheManager.getCache(...)`) must have an `<cache alias>` in `backend/src/main/resources/ehcache.xml`. JCache does not create missing caches.
- A write that changes cached data evicts the affected keys in the same method: `@CacheEvict` for one key, `cacheManager.getCache(...).evict(key)` when the keys come from a loop. Keys are built exactly as the `@Cacheable` builds them (for example `'STUDENT_' + userId` for `upcomingEvents`).
- Cached values are DTOs or `BasicResponse`s, never JPA entities.
- The frontend has no client cache: every screen fetches on mount.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- When graphify-out/graph.json exists, run `graphify query "<question>"` before your first grep, search or source-file read for a codebase question, and put this rule in every subagent prompt that explores code. Read raw files only for lines the graph or an earlier read already located. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- A committed graphify-out/graph.json is rewritten whole on every build, so a large diff to it in a pull request is expected. Do not flag it as churn or suggest skipping the graph rebuild or its hooks.
