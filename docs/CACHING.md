# Caching

Every place the backend keeps data in memory instead of reading MySQL, what invalidates it, and the gaps known today. Keep this file current: a change that reads or writes cached data updates it in the same commit.

The frontend keeps no data between visits. Screens built with TanStack Query (`frontend/src/lib/queryClient.js`: notification center, messages, settings, Ctrl+K search) hold their results in memory for the tab with `staleTime: 0`, so they still fetch on every mount and only show the previous result while the new one loads. The live channel is the SSE stream in `frontend/src/context/NotificationContext.jsx`. The service worker (`frontend/src/sw.js`) precaches the built app files and Google Fonts only, never API responses. Offline practice keeps questions saved in advance and queued answers in IndexedDB (`frontend/src/offline/practiceStore.js`) until they are synced.

## 1. Ehcache (Spring Cache over JCache)

Configured in `backend/src/main/resources/ehcache.xml` and enabled by `@EnableCaching` in `config/SecurityConfig.java`. All caches are heap-only, TTL 24 hours, and live in one process.

| Cache | Filled by (`@Cacheable`) | Key | Evicted by | Size |
|---|---|---|---|---|
| `upcomingEvents` | `UpcomingEventService.getUpcomingEvents(role, userId)` | `role + '_' + userId` (e.g. `STUDENT_123`) | `TeacherService.generateTestForStudents`: the teacher's key (`@CacheEvict`) and each student's key (`cacheManager`) | 1000 |
| `receivedMessages` | `MessageService.getAllRecivedMessages(userId)` | `userId` | `MessageService`, after commit: `sendMessage` (immediate send) and `deliverDueScheduledMessages` each recipient's key; `reply` the recipient's and the replier's key; `markAsRead` the reader's key. Scheduled (pending) copies are not in the cached list until delivered. | 1000 |
| `teacherSchedule` | `TeacherService.getTeacherSchedule(teacherId)` | `teacherId` | `TeacherService.addLessonToTeacher` (`@CacheEvict`) | 500 |
| `classRoomSchedule` | `ClassRoomService.getScheduleForClassRoom(schoolCode, classRoomName)` | `schoolCode + '_' + classRoomName` | `TeacherService.addLessonToTeacher`: the lesson's class | 100 |
| `studentSchedule` | `StudentService.getStudentSchedule(schoolCode, studentId)` | `studentId` | `TeacherService.addLessonToTeacher`: each student in the class | **missing from `ehcache.xml`** |
| `studentTestsStatus` | `StudentService.getStudentTestsStatus(userId)` | `userId` | `TestResultService.checkTeacherTest` (`@CacheEvict`); `TeacherService.generateTestForStudents`: each student | 1000 |
| `schoolDTO` | `SchoolService.getSchoolDTO(SchoolManagerId)` | `SchoolManagerId` | nothing | 100 |
| `allSchools` | `SystemAdminService.getAllSchoolsDTO()` | (no args) | `SystemAdminService`: `assignUserAsSchoolManager`, `addNewSchoolToSystem`, `assignSchoolManagerToSchool`, `removeSchoolManagerFromSchool` (`allEntries = true`) | 100 |

## 2. Hand-rolled in-memory maps

Not Spring caches: plain `ConcurrentHashMap`s in singleton beans. Lost on restart, never shared between instances.

| Where | Holds | Cleared |
|---|---|---|
| `UserService.otpLoginStorage` (static) | login OTP + its time, per user (valid 2 minutes) | overwritten by the next login; never removed |
| `UserService.otpStorage` (static) | password-reset OTP per user | removed after a successful reset |
| `SessionService.tokenCache` | JWT validation results | whole map every 2 hours (`clearTokenCache`) |
| `NotificationService.emitters` | one `SseEmitter` per connected user | on completion, timeout, error, a failed send, or a failed heartbeat (every 25s); a callback removes only its own emitter, not a newer one of the same user |
| `ChatGptService.userConversations` (static) | AI tutor history per user | reset to empty by the `clear-conversation` endpoint |
| `ActiveUserService.activeUsers` | who is online | `disconnectUser` |
| `StatisticsService.userStats` | per-student statistics | currently unused (its writes are commented out) |
| `filter/MetricsFilter` | request counts and response times | never |

Not cached on purpose (read from MySQL on every request): the notification center (`NotificationCenterService`: list, unread count, preferences), paged inbox and sent-message read receipts, message threads and attachments, the `.ics` calendar feed (`CalendarService`) and global search (`SearchService`).

## 3. Known gaps

Recorded, not fixed. Fix one only when the task is about it.

1. **`studentSchedule` has no alias in `ehcache.xml`.** JCache does not create caches on demand, so `@Cacheable("studentSchedule")` and `cacheManager.getCache("studentSchedule")` (which returns `null`) can fail at runtime. Adding the alias is the fix.
2. **`schoolDTO` is never evicted.** Adding grades, classes, teachers or a manager to a school leaves the manager's view stale for up to 24 hours.
3. **Writes outside the evicting methods.** Anything that changes lessons, tests or messages without going through the methods in the "Evicted by" column leaves the matching cache stale. Removing a lesson, a teacher or a student from a class evicts nothing.
4. **Single instance only.** Everything above is per process. A second instance on Render would serve different data from each and break SSE delivery.

## Checklist for a change that touches data

- Does the method read something one of the caches above holds? Then every write to that data must evict it.
- New `@Cacheable`? Add the alias to `ehcache.xml`, a row to table 1, and the eviction on every write path.
- Keys: build the evict key exactly like the `@Cacheable` key (same order, same separator, same role prefix).
- Cache DTOs or `BasicResponse`s, never JPA entities (lazy collections break outside the session).
- New in-memory map? Add a row to table 2 and say how it is cleared.
