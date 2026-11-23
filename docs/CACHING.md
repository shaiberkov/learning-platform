# Caching

Every place the backend keeps data in memory instead of reading MySQL, what invalidates it, and the gaps known today. Keep this file current: a change that reads or writes cached data updates it in the same commit.

The frontend keeps no data between visits. Screens built with TanStack Query (`frontend/src/lib/queryClient.js`: notification center, messages, settings, Ctrl+K search) hold their results in memory for the tab with `staleTime: 0`, so they still fetch on every mount and only show the previous result while the new one loads. The live channel is the SSE stream in `frontend/src/context/NotificationContext.jsx`. The service worker (`frontend/src/sw.js`) precaches the built app files and Google Fonts only, never API responses. Offline practice keeps questions saved in advance and queued answers in IndexedDB (`frontend/src/offline/practiceStore.js`) until they are synced.

## 1. Ehcache (Spring Cache over JCache)

Configured in `backend/src/main/resources/ehcache.xml` and enabled by `@EnableCaching` in `config/SecurityConfig.java`. All caches are heap-only, TTL 24 hours, and live in one process. Spring Boot picks JCache only when the JSR-107 API (`javax.cache:cache-api`) is on the classpath next to `org.ehcache:ehcache` (`jakarta` classifier); without it Boot silently falls back to an unbounded `ConcurrentMapCacheManager` that ignores `ehcache.xml` and creates any cache name on demand. `CacheConfigurationTest` guards this: the manager is `JCacheCacheManager`, the cache names are exactly the aliases, and an unknown name returns `null`. Values are stored by reference (no serialization).

| Cache | Filled by (`@Cacheable`) | Key | Evicted by | Size |
|---|---|---|---|---|
| `upcomingEvents` | `UpcomingEventService.getUpcomingEvents(role, userId)` | `role + '_' + userId` (e.g. `STUDENT_123`) | `TeacherService.generateTestForStudents`: the teacher's key (`@CacheEvict`) and each student's key (`cacheManager`); `SchoolYearService.closeYearAndPromote`: every moved student's `STUDENT_` key (`SchoolCacheEvictor`) | 1000 |
| `receivedMessages` | `MessageService.getAllRecivedMessages(userId)` | `userId` | `MessageService`, after commit: `sendMessage` (immediate send) and `deliverDueScheduledMessages` each recipient's key; `reply` the recipient's and the replier's key; `markAsRead` the reader's key; `AttendanceService.mark`: the school manager's key when it sends an unusual-absence alert. Scheduled (pending) copies are not in the cached list until delivered. | 1000 |
| `teacherSchedule` | `TeacherService.getTeacherSchedule(teacherId)` | `teacherId` | `TeacherService.addLessonToTeacher` (`@CacheEvict`); `TimetableService.saveLesson` / `deleteLesson`: old and new teacher (`SchoolCacheEvictor`) | 500 |
| `classRoomSchedule` | `ClassRoomService.getScheduleForClassRoom(schoolCode, classRoomName)` | `schoolCode + '_' + classRoomName` | `TeacherService.addLessonToTeacher`: the lesson's class; `TimetableService.saveLesson` / `deleteLesson`: old and new class | 100 |
| `studentSchedule` | `StudentService.getStudentSchedule(schoolCode, studentId)` | `studentId` | `TeacherService.addLessonToTeacher`: each student in the class; `TimetableService.saveLesson` / `deleteLesson`: each student of the old and new class; `SchoolYearService.closeYearAndPromote`: every moved student | 1000 |
| `studentTestsStatus` | `StudentService.getStudentTestsStatus(userId)` (lists only published teacher tests) | `userId` | `TestResultService.checkTeacherTest` (`@CacheEvict`); `TeacherService.generateTestForStudents`: each student; `ExamAttemptService` when it grades an attempt (submit, late submit, auto-submit): the student; `ExamAuthoringService` when an exam is published (on create, clone, or by `ExamSchedulerJob`): each student (`cacheManager`); `SchoolYearService.closeYearAndPromote`: every moved student | 1000 |
| `schoolDTO` | `SchoolService.getSchoolDTO(SchoolManagerId)` | `SchoolManagerId` | `BulkImportService.commit` and `SchoolYearService.closeYearAndPromote`: the school's manager (`SchoolCacheEvictor`) | 100 |
| `aiPromptCache` | `AiGateway.complete(..., cacheable = true)` by hand (`cacheManager.getCache`), for question generation, rubric grading and insight summaries; never for the tutor chat | SHA-256 of provider + model + messages + settings | nothing: an identical prompt is meant to return the same answer for 24 hours. "Generate again" in the question studio skips the cache (`fresh=true`) | 500 |
| `allSchools` | `SystemAdminService.getAllSchoolsDTO()` | (no args) | `SystemAdminService`: `assignUserAsSchoolManager`, `addNewSchoolToSystem`, `assignSchoolManagerToSchool`, `removeSchoolManagerFromSchool` (`allEntries = true`) | 100 |
| `schoolFeatureFlags` | `FeatureFlagService.flagsForSchool(schoolCode)` (a `Map<String, Boolean>` of effective flags, read through `security/FeatureFlags`) | `schoolCode` | `FeatureFlagService.setFlag` (`@CacheEvict`, same key); `school_feature_flag` has no other writer | 200 |

Not cached: the adaptive-learning reads (mistakes notebook, skill maps, hints, exam feedback) query MySQL on every call, because each practice answer changes them. A future cache there must be evicted from `StudentService.submitAnswer`, `ReviewService.answer`, `HintService.saveTemplate` and `ExamFeedbackService.recordExamAnswers`.

### Not cached on purpose

Engagement (XP, streaks, badges, class challenges), study plans, lesson pages and their progress, homework and submissions, and accommodations are read from MySQL on every request. They change with almost every student action, and a stale extra-time value or homework status would be wrong in a way users notice. Their writes touch none of the caches above (homework and study plans are not part of `upcomingEvents`). If one of them gets a cache, add it to table 1 with its evictions.

Not cached on purpose: the school dashboard (`SchoolAnalyticsService`), the timetable builder's lesson list, school years and their archive, and the parent screens (`ParentService`) read MySQL on every request. Messages to parents go through `MessageService.sendMessage`, which already evicts `receivedMessages` per recipient.

## 2. Hand-rolled in-memory maps

Not Spring caches: plain `ConcurrentHashMap`s in singleton beans. Lost on restart, never shared between instances.

| Where | Holds | Cleared |
|---|---|---|
| `UserService.otpStorage` (static) | password-reset OTP per user | removed after a successful reset |
| `UserService.otpLoginStorage` (static) | login OTP per user (code, time, failed attempts; valid 2 minutes) | overwritten by the next send; removed after a successful verify, an expired code, or 5 wrong codes |
| `UserService.passwordVerifiedAt` (static) | time the password step last succeeded per user; `send-otp` and `verify-otp` require it within 5 minutes | after a successful verify or 5 wrong codes; stale entries are dropped when read |
| `SessionService.tokenCache` | JWT validation results | whole map every 2 hours (`clearTokenCache`) |
| `NotificationService.emitters` | one `SseEmitter` per connected user | on completion, timeout, error, a failed send, or a failed heartbeat (every 25s); a callback removes only its own emitter, not a newer one of the same user |
| `ActiveUserService.activeUsers` | who is online | `disconnectUser` |
| `LiveQuizService.sessions` | running live in-class quizzes: class roster, current question with its answer, students' answers, scoreboard (by 6-digit code) | `end`, a new `start` by the same teacher, or 3 hours without activity (checked on the next access) |
| `StatisticsService.userStats` | per-student statistics | currently unused (its writes are commented out) |
| `filter/MetricsFilter` | request counts and response times | never |
| `security/RateLimiter.buckets` | Bucket4j token buckets per (rule, IP) and (rule, userId) for login, OTP, password reset and AI chat | idle buckets dropped every 10 minutes once their period has passed (`evictIdleBuckets`); a restart resets all limits |
| `filter/ErrorCountingFilter` | request, 4xx, 401/403 and 5xx counters for the system health screen | never (restart) |
| `utils/gptApi/AiUsageMetrics` | OpenAI request and failure counters for the system health screen | never (restart) |

Refresh tokens are not cached: they live in the `refresh_tokens` table (hashed) and every refresh reads it.

Not cached on purpose (read from MySQL on every request): the notification center (`NotificationCenterService`: list, unread count, preferences), paged inbox and sent-message read receipts, message threads and attachments, the `.ics` calendar feed (`CalendarService`) and global search (`SearchService`).

<!-- [s01] study rooms -->
**Study rooms [s01].** `StudyRoomService.presence` (who is in each room, with their task and last heartbeat) and `StudyRoomService.chats` (break-chat messages, at most 100 per room) are in-memory maps, lost on restart and per process. A member who sends no heartbeat for 15 seconds drops out; the chat shows only messages of the current break; both maps are cleared for a room when it is closed (empty and idle for 2 hours, checked when the school's rooms are listed). The rooms themselves (`study_room`) and focus sessions (`focus_session`) are read from MySQL on every request, and the focus insights are computed per request from `test_answer_record`, `focus_session` and `student_daily_activity`; no Ehcache cache is involved.
<!-- [s01] end -->

## 3. Known gaps

Recorded, not fixed. Fix one only when the task is about it.

1. ~~`studentSchedule` has no alias in `ehcache.xml`.~~ Fixed: the alias exists (heap 1000). `SchoolCacheEvictor` also skips a cache that is missing instead of throwing.
2. **`schoolDTO` is evicted only by the bulk import and the year close.** Adding grades, classes, teachers or a manager through the older single-item endpoints still leaves the manager's view stale for up to 24 hours.
3. **Writes outside the evicting methods.** Anything that changes lessons, tests or messages without going through the methods in the "Evicted by" column leaves the matching cache stale. Removing a lesson, a teacher or a student from a class evicts nothing.
4. **Single instance only.** Everything above is per process (a live quiz too: a restart ends every running quiz). A second instance on Render would serve different data from each and break SSE delivery.

Not cached on purpose: the question bank, gradebooks and attendance (`QuestionBankService`, `GradebookService`, `AttendanceService`) read MySQL on every request, so their writes have nothing to evict.

## Checklist for a change that touches data

- Does the method read something one of the caches above holds? Then every write to that data must evict it.
- New `@Cacheable`? Add the alias to `ehcache.xml`, a row to table 1, and the eviction on every write path.
- Keys: build the evict key exactly like the `@Cacheable` key (same order, same separator, same role prefix).
- Cache DTOs or `BasicResponse`s, never JPA entities (lazy collections break outside the session).
- New in-memory map? Add a row to table 2 and say how it is cleared.
