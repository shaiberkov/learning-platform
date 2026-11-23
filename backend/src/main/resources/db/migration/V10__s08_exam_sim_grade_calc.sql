-- V10 [s08]: past-exam bank and simulations (PastExamEntity, PastExamSectionEntity,
-- PastExamQuestionEntity, ExamSimAttemptEntity). The "what do I need?" calculator reads the
-- existing gradebook tables and adds none.
--
-- Safe on a database where ddl-auto=update already created these tables (dev machines):
-- tables use IF NOT EXISTS, and indexes and foreign keys are added only when missing.

create table if not exists past_exam (
        id bigint not null auto_increment,
        created_at datetime(6),
        description TEXT,
        duration_minutes integer not null,
        exam_year integer,
        session_label varchar(64),
        subject varchar(255),
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists past_exam_section (
        id bigint not null auto_increment,
        instructions TEXT,
        position integer not null,
        questions_to_count integer,
        title varchar(255) not null,
        exam_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists past_exam_question (
        id bigint not null auto_increment,
        answer varchar(512) not null,
        level integer not null,
        points integer not null,
        position integer not null,
        rubric TEXT,
        sub_topic varchar(255),
        text TEXT not null,
        topic varchar(255),
        section_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists exam_sim_attempt (
        id bigint not null auto_increment,
        deadline datetime(6) not null,
        predicted_margin integer,
        predicted_score integer,
        saved_answers_json TEXT,
        score integer,
        started_at datetime(6) not null,
        status enum ('AUTO_SUBMITTED','IN_PROGRESS','SUBMITTED') not null,
        student_user_id varchar(255) not null,
        submitted_at datetime(6),
        exam_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

-- Indexes and foreign keys

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'exam_sim_attempt' AND index_name = 'idx_exam_sim_attempt_student') = 0, 'create index idx_exam_sim_attempt_student on exam_sim_attempt (student_user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'past_exam_section' AND constraint_name = 'fk_past_exam_section_exam') = 0, 'alter table past_exam_section add constraint fk_past_exam_section_exam foreign key (exam_id) references past_exam (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'past_exam_question' AND constraint_name = 'fk_past_exam_question_section') = 0, 'alter table past_exam_question add constraint fk_past_exam_question_section foreign key (section_id) references past_exam_section (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'exam_sim_attempt' AND constraint_name = 'fk_exam_sim_attempt_exam') = 0, 'alter table exam_sim_attempt add constraint fk_exam_sim_attempt_exam foreign key (exam_id) references past_exam (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;
