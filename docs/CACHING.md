# Caching

Every place the backend keeps data in memory instead of reading MySQL, what invalidates it, and the gaps known today. Keep this file current: a change that reads or writes cached data updates it in the same commit.

The frontend has no client-side cache. Each screen fetches when it mounts; the only live channel is the SSE stream in `frontend/src/context/NotificationContext.jsx`.

## 1. Ehcache (Spring Cache over JCache)

Configured in `backend/src/main/resources/ehcache.xml` and enabled by `@EnableCaching` in `config/SecurityConfig.java`. All caches are heap-only, TTL 24 hours, and live in one process.

| Cache | Filled by (`@Cacheable`) | Key | Evicted by | Size |
|---|---|---|---|---|
| `upcomingEvents` | `UpcomingEventService.getUpcomingEvents(role, userId)` | `role + '_' + userId` (e.g. `STUDENT_123`) | `TeacherService.generateTestForStudents`: the teacher's key (`@CacheEvict`) and each student's key (`cacheManager`); `SchoolYearService.closeYearAndPromote`: every moved student's `STUDENT_` key (`SchoolCacheEvictor`) | 1000 |
| `receivedMessages` | `MessageService.getAllRecivedMessages(userId)` | `userId` | `MessageService.sendMessage`: each recipient's key | 1000 |
| `teacherSchedule` | `TeacherService.getTeacherSchedule(teacherId)` | `teacherId` | `TeacherService.addLessonToTeacher` (`@CacheEvict`); `TimetableService.saveLesson` / `deleteLesson`: old and new teacher (`SchoolCacheEvictor`) | 500 |
| `classRoomSchedule` | `ClassRoomService.getScheduleForClassRoom(schoolCode, classRoomName)` | `schoolCode + '_' + classRoomName` | `TeacherService.addLessonToTeacher`: the lesson's class; `TimetableService.saveLesson` / `deleteLesson`: old and new class | 100 |
| `studentSchedule` | `StudentService.getStudentSchedule(schoolCode, studentId)` | `studentId` | `TeacherService.addLessonToTeacher`: each student in the class; `TimetableService.saveLesson` / `deleteLesson`: each student of the old and new class; `SchoolYearService.closeYearAndPromote`: every moved student | 1000 |
| `studentTestsStatus` | `StudentService.getStudentTestsStatus(userId)` | `userId` | `TestResultService.checkTeacherTest` (`@CacheEvict`); `TeacherService.generateTestForStudents`: each student; `SchoolYearService.closeYearAndPromote`: every moved student | 1000 |
| `schoolDTO` | `SchoolService.getSchoolDTO(SchoolManagerId)` | `SchoolManagerId` | `BulkImportService.commit` and `SchoolYearService.closeYearAndPromote`: the school's manager (`SchoolCacheEvictor`) | 100 |
| `allSchools` | `SystemAdminService.getAllSchoolsDTO()` | (no args) | `SystemAdminService`: `assignUserAsSchoolManager`, `addNewSchoolToSystem`, `assignSchoolManagerToSchool`, `removeSchoolManagerFromSchool` (`allEntries = true`) | 100 |

Not cached on purpose: the school dashboard (`SchoolAnalyticsService`), the timetable builder's lesson list, school years and their archive, and the parent screens (`ParentService`) read MySQL on every request. Messages to parents go through `MessageService.sendMessage`, which already evicts `receivedMessages` per recipient.

## 2. Hand-rolled in-memory maps

Not Spring caches: plain `ConcurrentHashMap`s in singleton beans. Lost on restart, never shared between instances.

| Where | Holds | Cleared |
|---|---|---|
| `UserService.otpLoginStorage` (static) | login OTP + its time, per user (valid 2 minutes) | overwritten by the next login; never removed |
| `UserService.otpStorage` (static) | password-reset OTP per user | removed after a successful reset |
| `SessionService.tokenCache` | JWT validation results | whole map every 2 hours (`clearTokenCache`) |
| `NotificationService.emitters` | one `SseEmitter` per connected user | on completion, timeout, error, or a failed send |
| `ChatGptService.userConversations` (static) | AI tutor history per user | reset to empty by the `clear-conversation` endpoint |
| `ActiveUserService.activeUsers` | who is online | `disconnectUser` |
| `StatisticsService.userStats` | per-student statistics | currently unused (its writes are commented out) |
| `filter/MetricsFilter` | request counts and response times | never |

## 3. Known gaps

Recorded, not fixed. Fix one only when the task is about it.

1. ~~`studentSchedule` has no alias in `ehcache.xml`.~~ Fixed: the alias exists (heap 1000). `SchoolCacheEvictor` also skips a cache that is missing instead of throwing.
2. **`schoolDTO` is evicted only by the bulk import and the year close.** Adding grades, classes, teachers or a manager through the older single-item endpoints still leaves the manager's view stale for up to 24 hours.
3. **Writes outside the evicting methods.** Anything that changes lessons, tests or messages without going through the methods in the "Evicted by" column leaves the matching cache stale. Removing a lesson, a teacher or a student from a class evicts nothing.
4. **Single instance only.** Everything above is per process. A second instance on Render would serve different data from each and break SSE delivery.

## Checklist for a change that touches data

- Does the method read something one of the caches above holds? Then every write to that data must evict it.
- New `@Cacheable`? Add the alias to `ehcache.xml`, a row to table 1, and the eviction on every write path.
- Keys: build the evict key exactly like the `@Cacheable` key (same order, same separator, same role prefix).
- Cache DTOs or `BasicResponse`s, never JPA entities (lazy collections break outside the session).
- New in-memory map? Add a row to table 2 and say how it is cleared.
