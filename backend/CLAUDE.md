# backend/CLAUDE.md

Spring Boot 3.4 on Java 17, Maven (`./mvnw`), MySQL through Spring Data JPA. Package root: `src/main/java/org/example/learningprojectserver/`.

## Commands (run from `backend/`)

| What | Command |
|---|---|
| Run locally | `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` (API on `:8080`, Swagger at `/swagger-ui.html`) |
| Tests | `./mvnw test` |
| Build the jar | `./mvnw clean package -DskipTests` |
| Docker | `docker build -t learning-server .` then `docker run -p 8080:8080 --env-file .env learning-server` |

The `dev` profile (`application-dev.properties`) talks to `localhost:3306/learning` with `ddl-auto=update`. The `production` profile reads `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` and uses `ddl-auto=validate`: the schema is owned by Flyway (`src/main/resources/db/migration`), so a new column or table needs a `V<n>__*.sql` migration (see `docs/BACKUP.md`). `application.properties` sets `spring.profiles.active=production`, which the run command's `-Dspring-boot.run.profiles=dev` overrides. It must stay `production` in commits, because Render builds from the repo.

## Layout and layers

`controller → service → repository`, nothing skips a layer.

- `controller/`: thin. Read parameters, call one service method, return its result. Paths come from `constants/ControllerConstants.java`, never string literals.
- `service/`: business logic and transactions (`@Transactional` on writes that touch more than one row). Services return `BasicResponse<T>` (`success`, `errorCode`, `data`) or DTOs. `errorCode` is the Hebrew message the frontend shows.
- `repository/`: Spring Data interfaces. Prefer a JPQL query that returns a DTO or a `projection/` interface over loading entities and mapping in Java.
- `entities/`: 24 JPA entities. `UserEntity` is the base; `StudentEntity`, `TeacherEntity`, `SchoolManagerEntity`, `SystemAdminEntity` extend it. Entities never leave the service layer.
- `dto/`, `response/`, `mappers/`: API shapes. A new entity → DTO conversion is a class implementing `mappers/Mapper<I, O>`, not an inline lambda in a service.
- `constants/`: controller paths (`ControllerConstants`), security matchers and CORS (`SecurityConstants`), `BASE_API` (`SharedConstants`). User-facing Hebrew messages are written inline in the services; match that.
- `strategy/`: `message/` decides who receives a broadcast message (one strategy per recipient type, picked by `MessageRecipientStrategyFactory`); `UpcomingEvents/` builds the dashboard feed per role.
- `service/MathQuestion/`: 31 generators in 9 topic packages, picked by `MathQuestionFactory`. A new question type is one class in the right topic package plus one entry in the factory; it must produce the question text and the exact answer string the checker compares against.
- `notification/`: services publish a `NotificationEvent` through `NotificationEventPublisher`; `NotificationEventListener` (`@Async`) stores it in the notification center (`NotificationCenterService`), pushes it over SSE through `NotificationService`, and sends email / Web Push (`WebPushService`, VAPID keys from the environment) per the user's preferences. Business code never talks to an `SseEmitter` directly.
- i18n: new user-facing texts go in `resources/i18n/messages.properties` (Hebrew) with the same key in `_ar` and `_en`, and are read through `utils/LocalizedMessages` (request locale from `Accept-Language`, Hebrew outside a request). `MessageBundlesTest` fails when a key is missing in one language.
- `filter/`: `MetricsFilter` → `LoggingFilter` → `JwtAuthenticationFilter`, registered in `config/SecurityConfig.java`.
- Background work: `jobs/InactiveUsersJob` (`@Scheduled`, re-engagement email), `SessionService.clearTokenCache` every 2h, `HolidayUtils` daily at midnight Israel time, and `utils/SelfPingTask` (its own executor, started on `ApplicationReadyEvent`) keeping Render's free tier awake.
- In-memory state: OTPs (`UserService`), JWT validation results (`SessionService.tokenCache`), SSE emitters (`NotificationService`), ChatGPT conversations (`ChatGptService`), active users and live statistics. All of it is lost on restart and is per process, so the server runs as a single instance. See `docs/CACHING.md`.

## Rules

- Auth: login → OTP by e-mail (2 minutes; every code goes to the one inbox in `UserService.OTP_EMAIL`, SMS is no longer used) → httpOnly cookies `access_token` (JWT, 15 min) + `refresh_token` (rotating, hashed in `refresh_tokens`), via `security/AuthCookies` and `service/AuthTokenService`. Login, OTP, password reset and AI chat are rate limited (`filter/RateLimitFilter`, `rate-limit.*` properties). Roles are enforced per route group in `SecurityConfig`; ownership with `@PreAuthorize("@authz.…")` (`security/AccessGuard`) on controller methods. A new endpoint that takes another user's id, a school code or a test id adds the matching check. A system admin can impersonate a user for support (`/impersonation/start|stop`; the token carries `impersonatorId` and replaces only the access cookie; both ends are audited).
- External calls (email in `utils/ApiEmailProcessor`, injected as `utils/EmailSender`, OpenAI in `utils/gptApi/` and `ChatGptService`) are slow and can fail: keep them `@Async` or outside the transaction, and never let their failure roll back the user's write.
- Secrets only through `Environment.getProperty(...)` (`GPT_TOKEN`, `EMAIL_APP_PASSWORD`) or `${...}` in the properties files. Never a literal key in code.
- Caching: read `docs/CACHING.md` before changing anything that reads or writes cached data. Every cache name in code needs an alias in `src/main/resources/ehcache.xml`.
- Repository queries can be tested without MySQL: `CommunicationQueriesTest` is a `@DataJpaTest` on in-memory H2 (test dependency).
- Tests live in `src/test/java/...`. Service tests use Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock` repositories, `@InjectMocks` service) and need no database. Tests that need the whole app on MySQL extend `integration/AbstractMySqlIntegrationTest` (Testcontainers, profile `it`, skipped without Docker); they must not call endpoints that send e-mail. CI (`.github/workflows/ci.yml`) runs them all.
- Errors: request parameters and bodies carry Bean Validation constraints with Hebrew messages (`@NotBlank(message = "...")`, `@Valid` on `@RequestBody`). Failures, framework errors and unexpected exceptions become RFC 7807 `ProblemDetail` responses in `exception/GlobalExceptionHandler` (Hebrew `detail`, `errors` per field, plus `success=false` / `errorCode` for the old shape). Business outcomes still return `BasicResponse` with HTTP 200.
- Monitoring: Actuator `/actuator/health` is public, `info` and `metrics` need a `SYSTEM_ADMIN` JWT. Sentry is on only when `SENTRY_DSN` is set; the `json-logs` profile switches console logs to JSON.
