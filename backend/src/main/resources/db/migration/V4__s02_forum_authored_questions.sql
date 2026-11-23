-- V4 [s02]: class forum (questions with teacher-approved answers) and student-authored practice questions.
-- Generated from the entities with SchemaExportTool. IF NOT EXISTS keeps it safe on a dev database where
-- ddl-auto=update already created the tables (the indexes are created with the tables for the same reason).

create table if not exists class_forum_thread (
        id bigint not null auto_increment,
        answer_count integer not null,
        author_name varchar(255),
        author_user_id varchar(255) not null,
        body TEXT,
        class_room_id bigint not null,
        created_at datetime(6) not null,
        exercise_text varchar(1000),
        last_activity_at datetime(6) not null,
        resolved bit not null,
        school_code varchar(255),
        title varchar(200) not null,
        topic varchar(100),
        primary key (id),
        index idx_forum_thread_class (class_room_id)
    ) engine=InnoDB;

create table if not exists class_forum_post (
        id bigint not null auto_increment,
        approved bit not null,
        approved_by_name varchar(255),
        author_name varchar(255),
        author_role varchar(16),
        author_user_id varchar(255) not null,
        body TEXT not null,
        created_at datetime(6) not null,
        thread_id bigint not null,
        primary key (id),
        index idx_forum_post_thread (thread_id)
    ) engine=InnoDB;

create table if not exists peer_question (
        id bigint not null auto_increment,
        author_name varchar(255),
        author_user_id varchar(255) not null,
        bank_question_id bigint,
        class_room_id bigint not null,
        correct_answer varchar(500) not null,
        created_at datetime(6) not null,
        distractors varchar(2000) not null,
        explanation TEXT,
        question_text varchar(2000) not null,
        review_note varchar(500),
        reviewed_at datetime(6),
        reviewer_teacher_id varchar(255),
        school_code varchar(255),
        status enum ('APPROVED','PENDING','REJECTED') not null,
        topic varchar(100),
        primary key (id),
        index idx_peer_question_class_status (class_room_id, status),
        index idx_peer_question_author (author_user_id)
    ) engine=InnoDB;
