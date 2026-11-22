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
| `TEACHER` | see own timetable, generate a test for chosen students (graded automatically), see results, send messages, write lesson pages, give homework and follow the submission board, open class challenges, set students' extra exam time |
| `STUDENT` | practise questions by subject/topic, build a self-practice test, take teacher tests, see timetable, tests and statistics, read lessons (learn, practice, review), submit homework, follow a daily study plan before exams, see their own XP, streak and badges, and turn on read-aloud or the dyslexia font. A student not yet in a school sees a reduced menu. |

Everyone registers as a `STUDENT` with no school. The system admin turns a user into a school manager (`assignUserAsSchoolManager`, which refuses anyone who is no longer a student); a manager turns users into teachers of the school or puts students in a class. Roles are not enforced by the API today (see the Security section of the root `CLAUDE.md`).

## Domain model (main entities)

- `UserEntity` (base) → `StudentEntity`, `TeacherEntity`, `SchoolManagerEntity`, `SystemAdminEntity`.
- `SchoolEntity` (identified by `schoolCode`) → `SchoolGradeEntity` → `ClassRoomEntity` → students.
- `LessonEntity` (teacher, class, subject, day and hours) makes up `ScheduleEntity` timetables for classes, teachers and students.
- Questions: `QuestionEntity`, `PracticeQuestionEntity`, `TestQuestionEntity`, `StudentQuestionHistoryEntity`, `StudentProgressEntity`, `StudentStatisticsEntity`.
- Tests: `PracticeTestEntity` + `PracticeTestResultEntity` (student's own), `TeacherTestEntity` + `TeacherTestResultEntity` (assigned by a teacher), `TestEntity` / `TestResultEntity`.
- `MessageEntity` (sender, recipient, title, body), `Session`.
- Engagement: `StudentEngagementEntity` (XP total, streak), `StudentDailyActivityEntity` (XP per day), `StudentBadgeEntity`, `ClassChallengeEntity`.
- `StudyPlanTaskEntity`: one day's study task for a student ahead of a teacher test.
- `LessonContentEntity` (a teacher's lesson page: text, video embed, worked examples, linked to a subject/topic/sub-topic) and `LessonProgressEntity` (learned / practiced / reviewed per student). Different from `LessonEntity`, which is a timetable slot.
- `HomeworkEntity` (title, instructions, due date, assigned students) and `HomeworkSubmissionEntity` (content, time, late flag).
- `StudentAccommodationEntity`: extra exam time percent, read-aloud and dyslexia-font preferences.

These entities reference students and teachers by `userId` string columns, not foreign keys. Their tables (`student_engagement`, `student_daily_activity`, `student_badge`, `class_challenge`, `study_plan_task`, `lesson_content`, `lesson_content_example`, `lesson_progress`, `homework`, `homework_student`, `homework_submission`, `student_accommodation`) are created by `ddl-auto=update` in dev; production (`ddl-auto=none`) needs them created by hand or by a migration.

## Main flows

**Login.** `POST /user/login` checks the password (salted SHA-256) and sends a one-time code by SMS (valid 2 minutes, held in memory). `verify-otp` returns a JWT; the frontend stores it in the `token` cookie and validates it on every page load through the session endpoint. Password reset works the same way with a separate OTP.

**Practice.** The student picks subject → topic → exercise. `student/generate-question` (`StudentService.generateQuestionForPractice`, only for a student in a class) gets a question from `MathQuestionFactory`, which has 31 generators across integers, fractions, equations, complex numbers, vectors, matrices, analytic geometry, calculus and Hebrew word problems; `submit-answer` checks the answer and updates the student's progress and question history.

**Tests.** A student builds a practice test (subject, topic, difficulty, count, time limit) and the server grades it on submit. A teacher generates a test for selected students with a start time; each student gets a `NEW_TEST` notification over SSE and sees it in the tests table until it is taken and graded.

**Messages.** The sender chooses a recipient type (a class, a grade, all students, all teachers, teachers of a grade, all managers); a strategy in `strategy/message/` resolves the users. Each recipient gets a saved message and a `SYSTEM_MESSAGE` over SSE (and, in production, an SMS).

**Timetable.** The manager adds lessons to a teacher for a class; overlaps are rejected. Class, teacher and student timetables are cached and evicted when a lesson is added.

**Gamification.** Practice answers, practice and teacher tests, lesson steps, study-plan tasks and homework report to `EngagementEvents`, which adds XP, moves the daily streak (Israel time) and awards badges for real achievements: first activity, 3/7/30-day streaks, XP milestones, mastery of a sub-topic (skill level 5), a perfect test, five on-time homework, a finished lesson. It runs in its own transaction and swallows failures, so it never breaks the student's work. `engagement/get-summary` returns only the student's own data: level, streak, this week's XP against last week's, badges, and the class challenges. A teacher opens a class challenge (a combined XP target between two dates); students see only the class total. There is no leaderboard by design.

**Study plan.** `study-plan/get-plan` finds the student's upcoming, unsubmitted teacher tests and, the first time, writes one task per day from today until the day before the exam (at most 14 days; learn first, practice in between, review last; 10/15/20 minutes by difficulty, +5 when the exam is three days away or less). Tasks are kept, so adherence (done / due so far) is tracked per exam. Completing a task gives XP.

**Lesson pages.** A teacher writes a lesson (`lesson-content/*`) for a subject, topic and optional sub-topic, with text, a YouTube/Vimeo link (normalised server-side to an embed URL; any other host is rejected) and worked examples. Students of every class the teacher teaches see it and go learn → practice (opens `/exercises/<subject>/<topic>/<sub-topic>`) → review; each step is saved once and gives XP.

**Homework.** A teacher gives homework to whole classes they teach; the assigned students are fixed when it is created and get a `NEW_HOMEWORK` notification. Students submit (or resubmit) text; after the due date it is still accepted and flagged late. `HomeworkReminderJob` runs every 15 minutes and sends one `HOMEWORK_REMINDER` to students who have not submitted, once the due date is within 24 hours. The teacher board shows per student: submitted, late, missing or pending.

**Accommodations.** A teacher sets a student's extra exam time (0-100%); the exam screen (`TestSession`) adds it to every timer (practice and teacher tests), rounding up. The student turns on read-aloud (Web Speech API, `he-IL`, math symbols read as Hebrew words) and a dyslexia-friendly reading mode (`html.dyslexia-mode` in `App.css`: plain sans font, wider spacing). The server does not enforce exam end times, so extra time needs no server change.

**Dashboard feed.** `upcoming-events` returns next lessons, tests and events for the user's role, built by the strategy for that role.

## API

About 60 endpoints in 17 controllers, all under `/Learning-App`. The full list with parameters is in Swagger (`/swagger-ui.html` on a running backend); the path constants are in `backend/.../constants/ControllerConstants.java` and their frontend mirror in `frontend/src/constants/pages.constants.js`. Most responses are `BasicResponse { success, errorCode, data }`, where `errorCode` is a Hebrew message for the user.

## Background work

Homework reminders every 15 minutes (`HomeworkReminderJob`), re-engagement email to inactive users (`InactiveUsersJob`), JWT validation cache cleared every 2 hours, holidays refreshed daily, and a self-ping that keeps the Render free tier from sleeping.
