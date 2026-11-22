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
| `SCHOOLMANAGER` | add/remove grades and classes, assign/remove teachers and their teaching subjects, assign students to classes, build lessons (timetable), view class timetables, send messages, read the school's absence report (and receive unusual-absence alerts) |
| `TEACHER` | see own timetable, generate a test for chosen students (graded automatically), see results, send messages, keep a question bank, keep a gradebook per class, run a live quiz in class, mark attendance and read absence reports |
| `STUDENT` | practise questions by subject/topic, build a self-practice test, take teacher tests, answer a live class quiz, see timetable, tests and statistics. A student not yet in a school sees a reduced menu. |

Everyone registers as a `STUDENT` with no school. The system admin turns a user into a school manager (`assignUserAsSchoolManager`, which refuses anyone who is no longer a student); a manager turns users into teachers of the school or puts students in a class. Roles are not enforced by the API today (see the Security section of the root `CLAUDE.md`).

## Domain model (main entities)

- `UserEntity` (base) → `StudentEntity`, `TeacherEntity`, `SchoolManagerEntity`, `SystemAdminEntity`.
- `SchoolEntity` (identified by `schoolCode`) → `SchoolGradeEntity` → `ClassRoomEntity` → students.
- `LessonEntity` (teacher, class, subject, day and hours) makes up `ScheduleEntity` timetables for classes, teachers and students.
- Questions: `QuestionEntity`, `PracticeQuestionEntity`, `TestQuestionEntity`, `StudentQuestionHistoryEntity`, `StudentProgressEntity`, `StudentStatisticsEntity`.
- Tests: `PracticeTestEntity` + `PracticeTestResultEntity` (student's own), `TeacherTestEntity` + `TeacherTestResultEntity` (assigned by a teacher), `TestEntity` / `TestResultEntity`.
- `MessageEntity` (sender, recipient, title, body), `Session`.
- Teacher tools: `BankQuestionEntity` (question bank: text, answer, optional options, subject/topic/level/grade tags, owner, school, shared flag, usage counters), `GradebookAssignmentEntity` + `GradebookGradeEntity` + `GradebookWeightEntity` (a class gradebook per teacher), `AttendanceRecordEntity` (one student, one timetable lesson, one date). Production creates their tables by hand: `docs/sql/teacher-tools.sql`.

## Main flows

**Login.** `POST /user/login` checks the password (salted SHA-256) and sends a one-time code by SMS (valid 2 minutes, held in memory). `verify-otp` returns a JWT; the frontend stores it in the `token` cookie and validates it on every page load through the session endpoint. Password reset works the same way with a separate OTP.

**Practice.** The student picks subject → topic → exercise. `student/generate-question` (`StudentService.generateQuestionForPractice`, only for a student in a class) gets a question from `MathQuestionFactory`, which has 31 generators across integers, fractions, equations, complex numbers, vectors, matrices, analytic geometry, calculus and Hebrew word problems; `submit-answer` checks the answer and updates the student's progress and question history.

**Tests.** A student builds a practice test (subject, topic, difficulty, count, time limit) and the server grades it on submit. A teacher generates a test for selected students with a start time; each student gets a `NEW_TEST` notification over SSE and sees it in the tests table until it is taken and graded.

**Messages.** The sender chooses a recipient type (a class, a grade, all students, all teachers, teachers of a grade, all managers); a strategy in `strategy/message/` resolves the users. Each recipient gets a saved message and a `SYSTEM_MESSAGE` over SSE (and, in production, an SMS).

**Timetable.** The manager adds lessons to a teacher for a class; overlaps are rejected. Class, teacher and student timetables are cached and evicted when a lesson is added.

**Question bank.** A teacher writes questions or imports them from CSV/Excel (`/question-bank/import`: header row in Hebrew or English, up to 1,000 rows, UTF-8 or Windows-1255), tags them by subject, topic, level and grade, and may share each one with the other teachers of the same school. Search filters by tag and text over the teacher's own questions plus the shared ones. Each question counts how many times it was asked in a live quiz, how many answers it got and how many were correct.

**Gradebook.** Per teacher and class: students x columns. Columns are the teacher's system tests given to the class (read-only, best score per student from the test results) plus assignments the teacher adds (exam, homework, quiz, project; a maximum score; a date) and fills in by hand. Category weights (default exams 60%, homework 40%) must sum to 100; the final grade is the weighted mean of the category averages the student has. `/gradebook/export` returns the table as a right-to-left .xlsx.

**Live quiz.** The teacher opens a quiz for a class (`/live-quiz/start`, 6-digit code), sends questions from the bank or typed on the spot, and closes each one to reveal the answer. Students answer from `/live-quiz` on their phones, one answer per question. State moves over the existing SSE stream (`LIVE_QUIZ_STATE` to students, `LIVE_QUIZ_RESULTS` to the teacher), and both screens also poll. Sessions are in memory only.

**Attendance.** The teacher picks a date, sees that day's lessons from the timetable, and marks each student present, absent, late or excused. When a save newly makes a student's absences in the last 30 days reach 3 (then 6, 9...), or makes half or more of a class (4+ students) absent from a lesson, the school manager gets a message (saved, plus `SYSTEM_MESSAGE` over SSE). Reports per student over a date range: a teacher sees the lessons they marked, a manager the whole school. Dates follow Israel time.

**Dashboard feed.** `upcoming-events` returns next lessons, tests and events for the user's role, built by the strategy for that role.

## API

About 85 endpoints in 21 controllers, all under `/Learning-App`. The full list with parameters is in Swagger (`/swagger-ui.html` on a running backend); the path constants are in `backend/.../constants/ControllerConstants.java` and their frontend mirror in `frontend/src/constants/pages.constants.js`. Most responses are `BasicResponse { success, errorCode, data }`, where `errorCode` is a Hebrew message for the user.

## Background work

Re-engagement email to inactive users (`InactiveUsersJob`), JWT validation cache cleared every 2 hours, holidays refreshed daily, and a self-ping that keeps the Render free tier from sleeping.
