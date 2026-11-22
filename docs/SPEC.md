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

Everyone registers as a `STUDENT` with no school. The system admin turns a user into a school manager (`assignUserAsSchoolManager`, which refuses anyone who is no longer a student); a manager turns users into teachers of the school or puts students in a class. Roles are not enforced by the API today (see the Security section of the root `CLAUDE.md`).

## Domain model (main entities)

- `UserEntity` (base) → `StudentEntity`, `TeacherEntity`, `SchoolManagerEntity`, `SystemAdminEntity`.
- `SchoolEntity` (identified by `schoolCode`) → `SchoolGradeEntity` → `ClassRoomEntity` → students.
- `LessonEntity` (teacher, class, subject, day and hours) makes up `ScheduleEntity` timetables for classes, teachers and students.
- Questions: `QuestionEntity`, `PracticeQuestionEntity`, `TestQuestionEntity`, `StudentQuestionHistoryEntity`, `StudentProgressEntity`, `StudentStatisticsEntity`.
- Tests: `PracticeTestEntity` + `PracticeTestResultEntity` (student's own), `TeacherTestEntity` + `TeacherTestResultEntity` (assigned by a teacher), `TestEntity` / `TestResultEntity`.
- `MessageEntity` (sender, recipient, title, body), `Session`.
- AI: `AiUsageEntity` (one row per AI call: user, school, feature, tokens, estimated cost), `AiConversationMessageEntity` (tutor chat, kept `ai.retention.days`), `AiQuestionDraftEntity` → `BankQuestionEntity` (a `QuestionEntity` subclass: the teacher's question bank), `AiRubricEntity` + `AiOpenAnswerEntity` (open questions graded by rubric), `TestAnswerRecordEntity` (each answer of a checked teacher test) and `TeacherInsightEntity` (weekly insight).

## Main flows

**Login.** `POST /user/login` checks the password (salted SHA-256) and sends a one-time code by SMS (valid 2 minutes, held in memory). `verify-otp` returns a JWT; the frontend stores it in the `token` cookie and validates it on every page load through the session endpoint. Password reset works the same way with a separate OTP.

**Practice.** The student picks subject → topic → exercise. `student/generate-question` (`StudentService.generateQuestionForPractice`, only for a student in a class) gets a question from `MathQuestionFactory`, which has 31 generators across integers, fractions, equations, complex numbers, vectors, matrices, analytic geometry, calculus and Hebrew word problems; `submit-answer` checks the answer and updates the student's progress and question history.

**Tests.** A student builds a practice test (subject, topic, difficulty, count, time limit) and the server grades it on submit. A teacher generates a test for selected students with a start time; each student gets a `NEW_TEST` notification over SSE and sees it in the tests table until it is taken and graded.

**Messages.** The sender chooses a recipient type (a class, a grade, all students, all teachers, teachers of a grade, all managers); a strategy in `strategy/message/` resolves the users. Each recipient gets a saved message and a `SYSTEM_MESSAGE` over SSE (and, in production, an SMS).

**Timetable.** The manager adds lessons to a teacher for a class; overlaps are rejected. Class, teacher and student timetables are cached and evicted when a lesson is added.

**AI.** Everything goes through `service/ai/AiGateway`: the provider (`AiProvider`, today `OpenAiProvider`, chosen by `ai.provider` / `ai.model`) needs `GPT_TOKEN` in the environment, and without it every AI screen answers with a Hebrew "not enabled" message. The gateway enforces a per-user daily quota, serves repeated one-shot prompts from the `aiPromptCache`, and records tokens and estimated cost per school. Routes under `/ai/**` need a valid JWT (the user is taken from it, never from a parameter) and `/ai/staff/**` needs a staff role: the first routes where `SecurityConfig` enforces roles.
- *Tutor* (`POST /ai/tutor/stream`, SSE events `delta`/`replace`/`done`/`error`): Socratic, knows the practice question, its hidden answer and the student's typed answer; replies that give the answer away are withheld. `ExamGuard` turns it off while a teacher test is open for the student and for test questions. Photos go to the vision model; voice is the browser's Hebrew speech recognition, with `POST /ai/tutor/transcribe` as fallback.
- *Safety*: `ContentSafetyFilter` runs before and after the model: distress gets a caring answer with the ERAN hotline (never sent to the model), explicit content and prompt injection are refused, ID numbers, phones and e-mails are redacted, student text is wrapped as data. Refused messages are flagged; school staff see only flagged messages (`/ai/staff/safety/flagged`). `AiRetentionJob` deletes conversations after `ai.retention.days` (30); students can delete theirs at any time.
- *Question generation*: a teacher prompt → JSON questions (text, exact answer, explanation) → drafts queue → edit / approve / reject → `BankQuestionEntity`.
- *Rubric grading*: the teacher publishes an open question with a rubric; students answer (`/ai/open-questions`), the AI suggests a score and reasoning, the teacher approves or changes it in one click; students see only approved scores.
- *Insights*: `checkTeacherTest` also stores each answer (`TestAnswerRecorder`); `AiInsightsJob` (Sundays 07:00) computes struggling students by topic and suspicious questions (most students chose the same wrong answer) and asks the AI for a summary from pseudonymised numbers (S1, S2...).

**Dashboard feed.** `upcoming-events` returns next lessons, tests and events for the user's role, built by the strategy for that role.

## API

About 60 endpoints in 17 controllers, all under `/Learning-App`. The full list with parameters is in Swagger (`/swagger-ui.html` on a running backend); the path constants are in `backend/.../constants/ControllerConstants.java` and their frontend mirror in `frontend/src/constants/pages.constants.js`. Most responses are `BasicResponse { success, errorCode, data }`, where `errorCode` is a Hebrew message for the user.

## Background work

Re-engagement email to inactive users (`InactiveUsersJob`), JWT validation cache cleared every 2 hours, holidays refreshed daily, a self-ping that keeps the Render free tier from sleeping, weekly AI teacher insights (`AiInsightsJob`, Sunday 07:00 Israel time) and the nightly AI retention cleanup (`AiRetentionJob`, 03:30).
