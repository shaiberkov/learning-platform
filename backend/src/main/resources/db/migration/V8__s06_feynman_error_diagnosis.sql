-- V8 [s06]: "explain it to me" (Feynman) history and the mistake-type diagnosis on the mistakes notebook.
-- Safe on a database where ddl-auto=update already created these (IF NOT EXISTS / information_schema checks), like V2.

create table if not exists feynman_explanation (
        id bigint not null auto_increment,
        created_at datetime(6) not null,
        evaluated_by varchar(16) not null,
        explanation_text TEXT not null,
        feedback TEXT,
        gaps TEXT,
        input_mode varchar(10) not null,
        prompt_key varchar(80) not null,
        prompt_text varchar(500),
        school_code varchar(255),
        score integer,
        student_user_id varchar(255) not null,
        topic varchar(255),
        primary key (id)
    ) engine=InnoDB;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'feynman_explanation' AND index_name = 'idx_feynman_student_created') = 0, 'create index idx_feynman_student_created on feynman_explanation (student_user_id, created_at)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_item' AND column_name = 'given_answer') = 0, 'alter table review_item add column given_answer varchar(500)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_item' AND column_name = 'error_type') = 0, 'alter table review_item add column error_type enum (''CALCULATION'',''CONCEPTUAL'',''MISREAD'',''UNCLASSIFIED'')', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_item' AND column_name = 'error_type_source') = 0, 'alter table review_item add column error_type_source varchar(10)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_item' AND column_name = 'error_note') = 0, 'alter table review_item add column error_note varchar(500)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;
