-- V5 [s03]: 1-on-1 duels (duel, duel_question) and the daily challenge (daily_challenge, daily_challenge_attempt).
-- Generated from the entities with SchemaExportTool. Tables use IF NOT EXISTS, with their indexes and unique
-- keys inline, so the script is safe on a dev database where ddl-auto=update already created them.

create table if not exists duel (
        id bigint not null auto_increment,
        challenger_answered integer not null,
        challenger_correct integer not null,
        challenger_finished_at datetime(6),
        challenger_id varchar(255) not null,
        challenger_name varchar(255),
        created_at datetime(6) not null,
        finished_at datetime(6),
        level integer not null,
        mode enum ('COMPETITIVE','COOP') not null,
        opponent_answered integer not null,
        opponent_correct integer not null,
        opponent_finished_at datetime(6),
        opponent_id varchar(255) not null,
        opponent_name varchar(255),
        outcome varchar(16),
        school_code varchar(255),
        started_at datetime(6),
        status enum ('ACTIVE','DECLINED','FINISHED','PENDING') not null,
        sub_topic varchar(255),
        subject varchar(255),
        system_score integer,
        topic varchar(255),
        primary key (id),
        index idx_duel_challenger (challenger_id),
        index idx_duel_opponent (opponent_id)
    ) engine=InnoDB;

create table if not exists duel_question (
        id bigint not null auto_increment,
        answer varchar(255),
        challenger_answer varchar(255),
        challenger_answered_at datetime(6),
        challenger_correct bit,
        opponent_answer varchar(255),
        opponent_answered_at datetime(6),
        opponent_correct bit,
        position integer not null,
        question_text TEXT,
        duel_id bigint not null,
        primary key (id),
        constraint uk_duel_question_position unique (duel_id, position),
        constraint fk_duel_question_duel foreign key (duel_id) references duel (id)
    ) engine=InnoDB;

create table if not exists daily_challenge (
        id bigint not null auto_increment,
        answer varchar(255),
        challenge_date date not null,
        created_at datetime(6),
        grade_name varchar(64) not null,
        level integer not null,
        question_text TEXT,
        school_code varchar(255) not null,
        sub_topic varchar(255),
        subject varchar(255),
        topic varchar(255),
        primary key (id),
        constraint uk_daily_challenge_scope unique (school_code, grade_name, challenge_date)
    ) engine=InnoDB;

create table if not exists daily_challenge_attempt (
        id bigint not null auto_increment,
        answer varchar(255),
        answered_at datetime(6),
        challenge_date date not null,
        challenge_id bigint not null,
        correct bit not null,
        started_at datetime(6) not null,
        student_user_id varchar(255) not null,
        primary key (id),
        constraint uk_daily_attempt_student unique (challenge_id, student_user_id),
        index idx_daily_attempt_student_date (student_user_id, challenge_date)
    ) engine=InnoDB;
