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
- Exams (teacher tests taken under the server clock): `ExamAttemptEntity` (`exam_attempt`: start, deadline, seed, saved answers, status, score, tab-leave count), `ExamAttemptAnswerEntity` (`exam_attempt_answer`: graded answer per question, the input of item analysis), `ExamIntegrityEventEntity` (`exam_integrity_event`: tab and focus events). `TestQuestionEntity` carries the question `kind` and its options, key, tolerance, points and position; `TeacherTestEntity` carries title, publish state and time, template flag, A/B version label and group, source test and shuffle settings.
- `MessageEntity` (sender, recipient, title, body), `Session`.
- `AuditLogEntity` (`audit_log`: actor, impersonator, action, entity type and id, before, after, time, IP, school) and `SchoolFeatureFlagEntity` (`school_feature_flag`: school code, feature, on/off).

## Main flows

**Login.** `POST /user/login` checks the password (salted SHA-256) and sends a one-time code by SMS (valid 2 minutes, held in memory, single use). `verify-otp` sets two httpOnly cookies and returns no token in the body: `access_token` (JWT, 15 minutes, path `/`) and `refresh_token` (random 256-bit value, 7 days, path `/Learning-App/user`, stored only as a SHA-256 hash in `refresh_tokens`). Page scripts never see either token. `POST /user/refresh-token` rotates the refresh token (the old one is revoked; presenting a revoked one again after a 30-second grace revokes the whole family) and sets new cookies, or answers 401. `POST /user/logout` revokes the family and clears both cookies. A password reset revokes all of the user's refresh tokens. The frontend sends `withCredentials` and `X-Requested-With` on every axios request, validates the session through the session endpoint on page load, and refreshes once and retries on a 401. Cookie flags come from `auth.cookie.*` (production: `Secure`, `SameSite=None`; dev: `SameSite=Lax` over http). A state-changing request that carries an auth cookie without `X-Requested-With` gets 403 (CSRF guard, `CookieCsrfGuardFilter`). `Authorization: Bearer` is still accepted for Swagger and API clients. Password reset works the same way with a separate OTP.

**Rate limits.** `RateLimitFilter` (Bucket4j, in memory) limits login, OTP send, OTP verify, password reset (forgot + reset) and the AI chat endpoints per client IP and per user (JWT user, otherwise the `userId` parameter). Over the limit the API answers 429 with a Hebrew `errorCode` and `Retry-After`. Limits are set under `rate-limit.*` in `application.properties`.

**Practice.** The student picks subject → topic → exercise. `student/generate-question` (`StudentService.generateQuestionForPractice`, only for a student in a class) gets a question from `MathQuestionFactory`, which has 31 generators across integers, fractions, equations, complex numbers, vectors, matrices, analytic geometry, calculus and Hebrew word problems; `submit-answer` checks the answer and updates the student's progress and question history.

**Tests.** A student builds a practice test (subject, topic, difficulty, count, time limit) and the server grades it on submit. A teacher generates a test for selected students with a start time; each student gets a `NEW_TEST` notification over SSE and sees it in the tests table until it is taken and graded.

**Exams.** A teacher builds an exam in `/exam-builder` (`exam/create-exam`) from authored questions: multi-select, numeric with a tolerance, matching, ordering and short open answer (formulas typed with MathLive and shown with KaTeX; the server reads LaTeX numbers such as `rac{7}{2}`). Each kind is graded on the server (`service/exam/ExamGrader`), all or nothing. An exam can be published now, at a chosen time (`jobs/ExamSchedulerJob` publishes it and sends `NEW_TEST`), saved as a template, cloned (`exam/clone-exam`), or split into versions A and B with different question order. The student starts it with `exam/start-attempt`: the server records the start, fixes the deadline (scheduled start + time limit), and returns the questions without keys in an order shuffled per attempt from a stored seed (the same order on reload). Answers are autosaved (`save-answers`); `submit-attempt` after the deadline plus 30 seconds is rejected and the attempt is graded from what was saved; the scheduler grades attempts nobody submitted. Leaving the tab sends `integrity-event`, and the teacher sees the counts and the log in `/teacher-exams/:id/monitor`. `/teacher-exams/:id/analysis` shows the difficulty index (share correct) and the discrimination index (upper minus lower 27% by score) per question and flags questions that are too hard, too easy, weak or negatively discriminating. Grading also writes a `TeacherTestResultEntity`, so the student's tests table and statistics keep working. The old `teacher/check-teacher-test` endpoint is retired (it had no timer), and `test/get-test` returns no questions for teacher tests.

**Messages.** The sender chooses a recipient type (a class, a grade, all students, all teachers, teachers of a grade, all managers); a strategy in `strategy/message/` resolves the users. Each recipient gets a saved message and a `SYSTEM_MESSAGE` over SSE (and, in production, an SMS).

**Timetable.** The manager adds lessons to a teacher for a class; overlaps are rejected. Class, teacher and student timetables are cached and evicted when a lesson is added.

**Adaptive learning.** Four tables back it (`review_item`, `topic_mastery`, `sub_topic_hint`, `test_answer_record`), plus `question_entity.difficulty_level` and `practice_question_entity.hints_used` / `points_earned`. Endpoints are in `ControllerConstants.Adaptive` under `/student` and `/teacher`.
- *Mastery (Elo).* Each student has a rating per sub-topic (starts at 1000). Generator levels 1-5 are fixed ratings 850-1450; after every practice or exam answer the rating moves by K·(outcome − expected), K 48 for the first 10 answers then 32. Practice picks the level closest to rating + 50 (`EloRating.pickLevel`). The skill map (`/student/skill-map`) colours a sub-topic red below 1000, yellow below 1200, green from 1200; the teacher's class view (`/teacher/class-skill-map`) shows only classes in the teacher's `teacher_classroom` rows.
- *Mistakes notebook (SM-2).* A wrong practice answer, a practice answer reached only through the full solution, and every wrong exam answer enter `review_item`, due the same day. Reviewing (`/student/review/answer`) grades a correct answer as quality 4 and a wrong one as 1 and reschedules with SM-2 (1 day, 6 days, then interval × easiness). Due counts use the Israel calendar day.
- *Hints.* Per sub-topic: hint 1, hint 2 and solution steps, edited by teachers (`/teacher/hint-template`), with built-in defaults (`HintService`). The student opens them in order (`/student/practice-hint`); the step is stored on the question and caps its points at 10 / 7 / 4 / 0 and its Elo outcome at 1 / 0.75 / 0.5 / 0. The full solution ends with the question's own answer.
- *Exam feedback.* Grading a practice or teacher test writes one `test_answer_record` per question. The client then posts the seconds spent per question (`/student/exam-question-times`, time accrues to the last focused answer field) and reads `/student/exam-feedback`: score, breakdown by sub-topic with average time, the class's accuracy per sub-topic and the teacher test's class average (only when at least 3 other students contributed), and a recommendation to practise the weakest sub-topic below 80%.

**Dashboard feed.** `upcoming-events` returns next lessons, tests and events for the user's role, built by the strategy for that role.

**Audit log.** Role changes (promote to manager or teacher, remove a teacher or manager, which delete and recreate the user row), teacher and student class changes, grade changes, added lessons, recorded teacher-test scores, impersonation and feature-flag changes call `AuditService.record`. `GET /audit-log/list` (filters: action, actor, entity type and id, dates, paging) serves the "יומן פעולות" screen: a system admin sees everything, a school manager only entries of their own school.

**System admin tools.** Impersonation (`POST /impersonation/start?targetUserId=`, admin only; `POST /impersonation/stop` with the impersonation token) issues a 30-minute token that acts as the user and carries the admin's id; the frontend shows a banner while it is active and every audit entry records the impersonator. `GET /system-admin/health` reports uptime, database status and latency, request and error counters, AI call counters and whether `GPT_TOKEN` is set, memory and active users. Per-school feature flags (`AI_CHAT`, `PRACTICE_TESTS`, `TEACHER_TESTS`, `MESSAGES`, all on by default) are set at `/system-admin/feature-flags` and checked by `security/FeatureFlags`; a disabled feature answers 403 with a Hebrew message.

## API

About 60 endpoints in 17 controllers, all under `/Learning-App`. The full list with parameters is in Swagger (`/swagger-ui.html` on a running backend); the path constants are in `backend/.../constants/ControllerConstants.java` and their frontend mirror in `frontend/src/constants/pages.constants.js`. Most responses are `BasicResponse { success, errorCode, data }`, where `errorCode` is a Hebrew message for the user. Requests that fail validation (missing or invalid parameters), framework errors and unexpected server errors return HTTP 4xx/5xx with an RFC 7807 `application/problem+json` body: `{ type, title, status, detail, errors?, success: false, errorCode }`, where `detail` (= `errorCode`) is Hebrew and `errors` maps each invalid field to its Hebrew message. `GET /actuator/health` is public; other Actuator endpoints need a `SYSTEM_ADMIN` token.

## Background work

Exam publishing and auto-submission every 30 seconds (`ExamSchedulerJob`), re-engagement email to inactive users (`InactiveUsersJob`), JWT validation cache cleared every 2 hours, holidays refreshed daily, and a self-ping that keeps the Render free tier from sleeping.
