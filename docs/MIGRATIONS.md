# Schema migrations

The schema is managed by Flyway (`backend/src/main/resources/db/migration`, settings in
`application.properties`). Production runs `ddl-auto=validate`: Hibernate checks the schema Flyway built and
the app refuses to start when an entity and the database disagree. Dev still runs `ddl-auto=update`, after
Flyway.

| Version | What |
|---|---|
| `V1__baseline_schema.sql` | the schema of the entities before the improvement branches. Existing databases are recorded as version 1 without running it (`baseline-on-migrate=true`). |
| `V2__improvement_branches.sql` | everything the improvement branches added (below). |
| `V5__s03_duels_daily_challenge.sql` | [s03] tables `duel`, `duel_question`, `daily_challenge`, `daily_challenge_attempt`. |

## What V2 adds

- New tables: `refresh_tokens` (auth), `audit_log`, `school_feature_flag`, `exam_attempt`, `exam_attempt_answer`,
  `exam_integrity_event`, `review_item`, `topic_mastery`, `sub_topic_hint`, `test_answer_record`,
  `student_engagement`, `student_daily_activity`, `student_badge`, `class_challenge`, `study_plan_task`,
  `lesson_content`, `lesson_content_example`, `lesson_progress`, `homework`, `homework_student`,
  `homework_submission`, `student_accommodation`, `ai_usage`, `ai_conversation_message`, `ai_question_draft`,
  `ai_rubric`, `ai_open_answer`, `teacher_insight`, `bank_question`, `gradebook_assignment`, `gradebook_grade`,
  `gradebook_weight`, `attendance_record`, `parent_entity`, `parent_student`, `school_year`, `semester`,
  `archived_student_record`, `notification_entity`, `notification_preference_entity`,
  `push_subscription_entity`, `calendar_token_entity`, `message_attachment_entity`.
- New nullable columns: `lesson_entity.room`; `message_entity.read_at`, `batch_id`, `thread_id`,
  `parent_message_id`, `attachment_id`, `scheduled_for`, `pending`; `practice_question_entity.hints_used`,
  `points_earned`; `question_entity.difficulty_level`; `teacher_test_entity.title`, `published` (null =
  published), `publish_at`, `template`, `version_label`, `exam_group_id`, `source_test_id`,
  `shuffle_questions`, `shuffle_options`; `test_question_entity.kind`, `prompt`, `options_json`,
  `answer_key_json`, `tolerance`, `points`, `position`.
- `user_entity.role` changes from a native `ENUM` (which could not hold `PARENT`: saving a parent failed with
  "Data truncated") to `VARCHAR(32)`.
- Indexes and unique constraints of the new entities, plus `idx_user_username`, `idx_class_room_name`,
  `idx_message_batch`, `idx_message_thread`, `idx_message_pending_schedule`.

V2 is safe on a database where `ddl-auto=update` already created some of this (a dev machine that ran one of
the branches): tables use `IF NOT EXISTS`, and every column, index and constraint is added only when
`information_schema` does not list it. On such a database, check that the existing tables match the entities
(the app's `validate` run in a staging copy shows any difference).

## Adding a migration

1. Change the entity.
2. Export the schema the entities map to: `./mvnw test -Dtest=SchemaExportTool -Dschema.export=target/schema.sql`.
3. Write `V<n>__<what>.sql` with the difference (new tables, `ALTER TABLE ... ADD COLUMN`, indexes). Never edit
   a migration that has shipped.
4. Run the Testcontainers integration tests (they build an empty MySQL from all migrations and start the app
   with `validate`).

Before the first production deploy with Flyway, follow `docs/BACKUP.md` ("Before the first production deploy").
