# Learning Platform: system spec

What the system does and how its parts fit. For how to work in each app see `backend/CLAUDE.md` and `frontend/CLAUDE.md`; for in-memory data see `docs/CACHING.md`.

## Purpose

One web system for a school's daily work and for math practice. A system admin onboards schools; each school's manager builds its structure (grades, classes, teachers, timetable); teachers send tests and messages; students practise with auto-generated questions, take timed tests and see their progress. There is also an AI tutor (ChatGPT) for every user.

## Parts

```
frontend/ (React, Vite)  ──REST + JWT──▶  backend/ (Spring Boot)  ──JPA──▶  MySQL
        ▲                                    │  ├─ SMS gateway (OTP codes)
        └──────────── SSE (/notifications) ──┘  ├─ SMTP (re-engagement and OTP email)
                                                └─ OpenAI Chat Completions (AI tutor)
```

Deployed on Render: frontend `schoolmanagmentsystem-z0m5.onrender.com`, backend `learning-project-server.onrender.com`. Both are built from this repository's committed code. One backend instance (all live state is in process memory).

## Roles

| Role (`enums/Role.java`) | Can |
|---|---|
| `SYSTEM_ADMIN` | add schools, assign and remove school managers, list schools, send messages to managers |
| `SCHOOLMANAGER` | add/remove grades and classes, assign/remove teachers and their teaching subjects, assign students to classes, build lessons (timetable), view class timetables, send messages |
| `TEACHER` | see own timetable, generate a test for chosen students (graded automatically), see results, send messages |
| `STUDENT` | practise questions by subject/topic, build a self-practice test, take teacher tests, see timetable, tests and statistics. A student not yet in a school sees a reduced menu. |

Everyone registers as a `STUDENT` with no school. The system admin turns a user into a school manager (`assignUserAsSchoolManager`, which refuses anyone who is no longer a student); a manager turns users into teachers of the school or puts students in a class. Roles are enforced by the API: URL rules per route group in `config/SecurityConfig.java`, and ownership checks (`@PreAuthorize` with `security/AccessGuard`) so a teacher reaches only their own classes and students, a student only their own tests and results, and a school manager only their own school. A system admin passes every ownership check.

## Domain model (main entities)

- `UserEntity` (base) → `StudentEntity`, `TeacherEntity`, `SchoolManagerEntity`, `SystemAdminEntity`.
- `SchoolEntity` (identified by `schoolCode`) → `SchoolGradeEntity` → `ClassRoomEntity` → students.
- `LessonEntity` (teacher, class, subject, day and hours) makes up `ScheduleEntity` timetables for classes, teachers and students.
- Questions: `QuestionEntity`, `PracticeQuestionEntity`, `TestQuestionEntity`, `StudentQuestionHistoryEntity`, `StudentProgressEntity`, `StudentStatisticsEntity`.
- Tests: `PracticeTestEntity` + `PracticeTestResultEntity` (student's own), `TeacherTestEntity` + `TeacherTestResultEntity` (assigned by a teacher), `TestEntity` / `TestResultEntity`.
- `MessageEntity` (sender, recipient, title, body), `Session`.
- `AuditLogEntity` (`audit_log`: actor, impersonator, action, entity type and id, before, after, time, IP, school) and `SchoolFeatureFlagEntity` (`school_feature_flag`: school code, feature, on/off).

## Main flows

**Login.** `POST /user/login` checks the password (salted SHA-256) and sends a one-time code by SMS (valid 2 minutes, held in memory, single use). `verify-otp` sets two httpOnly cookies and returns no token in the body: `access_token` (JWT, 15 minutes, path `/`) and `refresh_token` (random 256-bit value, 7 days, path `/Learning-App/user`, stored only as a SHA-256 hash in `refresh_tokens`). Page scripts never see either token. `POST /user/refresh-token` rotates the refresh token (the old one is revoked; presenting a revoked one again after a 30-second grace revokes the whole family) and sets new cookies, or answers 401. `POST /user/logout` revokes the family and clears both cookies. A password reset revokes all of the user's refresh tokens. The frontend sends `withCredentials` and `X-Requested-With` on every axios request, validates the session through the session endpoint on page load, and refreshes once and retries on a 401. Cookie flags come from `auth.cookie.*` (production: `Secure`, `SameSite=None`; dev: `SameSite=Lax` over http). A state-changing request that carries an auth cookie without `X-Requested-With` gets 403 (CSRF guard, `CookieCsrfGuardFilter`). `Authorization: Bearer` is still accepted for Swagger and API clients. Password reset works the same way with a separate OTP.

**Rate limits.** `RateLimitFilter` (Bucket4j, in memory) limits login, OTP send, OTP verify, password reset (forgot + reset) and the AI chat endpoints per client IP and per user (JWT user, otherwise the `userId` parameter). Over the limit the API answers 429 with a Hebrew `errorCode` and `Retry-After`. Limits are set under `rate-limit.*` in `application.properties`.

**Practice.** The student picks subject → topic → exercise. `student/generate-question` (`StudentService.generateQuestionForPractice`, only for a student in a class) gets a question from `MathQuestionFactory`, which has 31 generators across integers, fractions, equations, complex numbers, vectors, matrices, analytic geometry, calculus and Hebrew word problems; `submit-answer` checks the answer and updates the student's progress and question history.

**Tests.** A student builds a practice test (subject, topic, difficulty, count, time limit) and the server grades it on submit. A teacher generates a test for selected students with a start time; each student gets a `NEW_TEST` notification over SSE and sees it in the tests table until it is taken and graded.

**Messages.** The sender chooses a recipient type (a class, a grade, all students, all teachers, teachers of a grade, all managers); a strategy in `strategy/message/` resolves the users. Each recipient gets a saved message and a `SYSTEM_MESSAGE` over SSE (and, in production, an SMS).

**Timetable.** The manager adds lessons to a teacher for a class; overlaps are rejected. Class, teacher and student timetables are cached and evicted when a lesson is added.

**Dashboard feed.** `upcoming-events` returns next lessons, tests and events for the user's role, built by the strategy for that role.

**Audit log.** Role changes (promote to manager or teacher, remove a teacher or manager, which delete and recreate the user row), teacher and student class changes, grade changes, added lessons, recorded teacher-test scores, impersonation and feature-flag changes call `AuditService.record`. `GET /audit-log/list` (filters: action, actor, entity type and id, dates, paging) serves the "יומן פעולות" screen: a system admin sees everything, a school manager only entries of their own school.

**System admin tools.** Impersonation (`POST /impersonation/start?targetUserId=`, admin only; `POST /impersonation/stop` with the impersonation token) issues a 30-minute token that acts as the user and carries the admin's id; the frontend shows a banner while it is active and every audit entry records the impersonator. `GET /system-admin/health` reports uptime, database status and latency, request and error counters, AI call counters and whether `GPT_TOKEN` is set, memory and active users. Per-school feature flags (`AI_CHAT`, `PRACTICE_TESTS`, `TEACHER_TESTS`, `MESSAGES`, all on by default) are set at `/system-admin/feature-flags` and checked by `security/FeatureFlags`; a disabled feature answers 403 with a Hebrew message.

## API

About 60 endpoints in 17 controllers, all under `/Learning-App`. The full list with parameters is in Swagger (`/swagger-ui.html` on a running backend); the path constants are in `backend/.../constants/ControllerConstants.java` and their frontend mirror in `frontend/src/constants/pages.constants.js`. Most responses are `BasicResponse { success, errorCode, data }`, where `errorCode` is a Hebrew message for the user.

## Background work

Re-engagement email to inactive users (`InactiveUsersJob`), JWT validation cache cleared every 2 hours, holidays refreshed daily, and a self-ping that keeps the Render free tier from sleeping.
