# Schema migrations

There is no Flyway or Liquibase yet. Dev runs with `ddl-auto=update`, which creates new tables and adds new
columns but never changes an existing column's type. Production runs with `ddl-auto=none`, so every schema
change below must be applied to the production database **before** the code that needs it is deployed.

## School administration and parents (branch `improve/g09-school`)

Apply in this order. All statements are MySQL 8.

### 1. `user_entity.role`: allow `PARENT` (required, blocking)

Hibernate 6 created `role` as a native `ENUM('SCHOOLMANAGER','STUDENT','SYSTEM_ADMIN','TEACHER')` (confirmed on
the local dev database). Saving a user with `role = 'PARENT'` fails with `Data truncated for column 'role'`, which
breaks linking a parent and rolls back any bulk import that contains a parent row. The entity now maps the
column as `VARCHAR(32)`, so a fresh schema gets the right type, but existing databases need:

```sql
ALTER TABLE user_entity MODIFY role VARCHAR(32) NOT NULL;
```

In the `dev` profile `DevSchemaPatcher` runs this once at startup when it finds the old ENUM.

### 2. `lesson_entity.room` (required, blocking)

`LessonEntity` now maps `room`. Without the column every lesson query fails, which breaks the existing
teacher, class and student schedules and the upcoming-events feed, not only the timetable builder.

```sql
ALTER TABLE lesson_entity ADD COLUMN room VARCHAR(255) NULL;
```

### 3. Parents

```sql
CREATE TABLE parent_entity (
    id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_parent_entity_user FOREIGN KEY (id) REFERENCES user_entity (id)
);

CREATE TABLE parent_student (
    parent_id  BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    CONSTRAINT fk_parent_student_parent  FOREIGN KEY (parent_id)  REFERENCES parent_entity (id),
    CONSTRAINT fk_parent_student_student FOREIGN KEY (student_id) REFERENCES student_entity (id)
);
```

### 4. School years, semesters and the year-close archive

```sql
CREATE TABLE school_year (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    school_id  BIGINT NOT NULL,
    name       VARCHAR(255) NOT NULL,
    start_date DATE NULL,
    end_date   DATE NULL,
    active     BIT NOT NULL,
    closed     BIT NOT NULL,
    closed_at  DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_school_year_school FOREIGN KEY (school_id) REFERENCES school_entity (id)
);

CREATE TABLE semester (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    school_year_id BIGINT NOT NULL,
    name           VARCHAR(255) NOT NULL,
    start_date     DATE NULL,
    end_date       DATE NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_semester_school_year FOREIGN KEY (school_year_id) REFERENCES school_year (id)
);

CREATE TABLE archived_student_record (
    id               BIGINT NOT NULL AUTO_INCREMENT,
    school_year_id   BIGINT NOT NULL,
    student_user_id  VARCHAR(255) NULL,
    student_name     VARCHAR(255) NULL,
    grade_name       VARCHAR(255) NULL,
    class_name       VARCHAR(255) NULL,
    tests_count      INT NOT NULL,
    average_score    DOUBLE NULL,
    outcome          VARCHAR(255) NULL,
    next_grade_name  VARCHAR(255) NULL,
    next_class_name  VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_archive_school_year FOREIGN KEY (school_year_id) REFERENCES school_year (id)
);
CREATE INDEX idx_archive_year ON archived_student_record (school_year_id);
```

When Flyway is introduced, these become one versioned migration (for example `V2__school_admin_and_parents.sql`).
