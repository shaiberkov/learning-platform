# Database migrations, backups and restores

## Migrations (Flyway)

The schema is owned by Flyway. Migrations live in `backend/src/main/resources/db/migration` and run on startup, before Hibernate.

- `V1__baseline_schema.sql` is the schema the JPA entities mapped when Flyway was introduced, generated from the entities with `SchemaExportTool` (no database needed):
  ```
  cd backend
  ./mvnw test -Dtest=SchemaExportTool -Dschema.export=target/schema.sql
  ```
- `spring.flyway.baseline-on-migrate=true` (`application.properties`): a database that already has tables but no `flyway_schema_history` (the current production and every developer database) is recorded as version 1 and `V1` is **not** run on it. `V1` only runs on an empty schema: a new environment, the Testcontainers integration tests.
- Profiles: `production` runs Hibernate with `ddl-auto=validate` (startup fails if the schema and the entities disagree; emergency fallback `JPA_DDL_AUTO=none` in the environment). `dev` keeps `ddl-auto=update` for quick local work, but every schema change still needs a migration.
- **A schema change** (new entity, column, index): add `V<n>__short_description.sql` with the MySQL DDL, and run the integration tests (`./mvnw test`, needs Docker): they build the schema from the migrations and let Hibernate validate it. To see what Hibernate expects, run `SchemaExportTool` and diff against the previous output. Never edit a migration that already ran anywhere.
- Flyway 10 (community) supports MySQL 8.0 and later.

### Before the first production deploy with Flyway

`validate` compares the live production schema with the entities. Production was created by an older `ddl-auto=update`, so column types can differ (typical case: an `@Enumerated(STRING)` column that is `varchar` in production while Hibernate 6 expects `enum(...)`). Check once against a copy:

1. Take a backup (below) and restore it into a scratch database.
2. Run the backend against that copy with the production profile: `DB_URL=jdbc:mysql://localhost:3306/learning_restore_test ... ./mvnw spring-boot:run -Dspring-boot.run.profiles=production` (on a free port: `-Dspring-boot.run.arguments=--server.port=8081`).
3. If startup fails with `Schema-validation: wrong column type ...`, write `V2__align_production_schema.sql` with the `ALTER TABLE` statements and repeat. Until then, deploy with `JPA_DDL_AUTO=none`.

## Backups

`scripts/backup-db.sh` (Linux/macOS/Git Bash) and `scripts/backup-db.ps1` (Windows) run `mysqldump --single-transaction` and write `<backup_dir>/<db>_<YYYY-MM-DD_HHMMSS>.sql.gz`, then delete dumps older than `RETENTION_DAYS` (default 14). A dump only gets its final name after it is complete.

They need the MySQL client tools (`mysqldump`, `mysql`) on the machine running them and read everything from the environment, never from files in the repository:

| Variable | Meaning |
|---|---|
| `DB_URL` | optional `jdbc:mysql://host:port/db` (same value the backend uses); host, port and name are read from it |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | override `DB_URL` (defaults `127.0.0.1`, `3306`, `learning`) |
| `DB_USERNAME`, `DB_PASSWORD` | required; the password is passed as `MYSQL_PWD`, not on the command line |
| `BACKUP_DIR` | default `./backups` (first argument / `-BackupDir` wins) |
| `RETENTION_DAYS` | default `14` |

Keep the backup directory **outside** the repository (`backups/` is not ignored by git on purpose: it should not exist here) and copy it off the machine (another disk, cloud storage).

### Daily schedule

Linux (cron, 02:30 every night), with the variables in a root-only file such as `/etc/learning-backup.env`:
```
30 2 * * * set -a; . /etc/learning-backup.env; set +a; /opt/learning-platform/scripts/backup-db.sh /var/backups/learning >> /var/log/learning-backup.log 2>&1
```

Windows (Task Scheduler), with `DB_*` set as user environment variables of the account that runs the task:
```
schtasks /Create /SC DAILY /ST 02:30 /TN "learning-db-backup" /TR "powershell -NoProfile -ExecutionPolicy Bypass -File C:\learning-platform\scripts\backup-db.ps1 -BackupDir D:\backups\learning"
```

A managed MySQL host may also offer its own snapshots; these scripts are the portable copy you control.

## Restore

```
scripts/restore-db.sh  backups/learning_2026-11-23_023000.sql.gz [target_db]
.\scripts\restore-db.ps1 -Dump backups\learning_2026-11-23_023000.sql.gz [-TargetDb name] [-Force]
```

The target database defaults to `DB_NAME` and is created when missing. If it already has tables, the script asks you to type its name (`RESTORE_CONFIRM=yes` / `-Force` skips that).

Production restore, in order:
1. Stop the backend (Render: suspend the service) so nothing writes during the restore.
2. Take a fresh backup of the current state, even if it is broken. It is your way back.
3. Restore into a new database (`learning_restored`), check it (restore test below), then point `DB_URL` at it, or restore over the original name once you are sure.
4. Start the backend. Flyway sees `flyway_schema_history` from the dump and only runs migrations newer than the backup.

## Restore test (monthly, and after any change to these scripts)

A backup that was never restored is not a backup. `scripts/test-restore.sh [backup_dir]` automates it:

1. takes the newest dump in the backup directory,
2. restores it into the scratch database `<DB_NAME>_restore_test`,
3. prints exact row counts per table, live vs restored, and fails if a table is missing from the restore,
4. warns if `flyway_schema_history` did not come back,
5. drops the scratch database.

Then, by hand:
- Start the backend against the scratch copy with the production profile (see "Before the first production deploy") and log in with a known account. Startup proves Flyway and Hibernate accept the restored schema.
- Write the date, the dump file name and the result in the team's ops log.

On Windows without bash, do the same with `restore-db.ps1 -TargetDb learning_restore_test -Force`, compare `SELECT COUNT(*)` for a few key tables (`user_entity`, `school_entity`, `test_entity`, `teacher_test_result_entity`), then `DROP DATABASE learning_restore_test`.
