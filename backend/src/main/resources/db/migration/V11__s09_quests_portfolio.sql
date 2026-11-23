-- V11 [s09]: story quests (progress + cosmetic rewards) and the personal portfolio (projects + share link).
-- Matches QuestProgressEntity, StudentCosmeticEntity, PortfolioProjectEntity and PortfolioShareEntity.
-- Tables use IF NOT EXISTS so a dev database where ddl-auto=update already created them is left alone.

create table if not exists student_quest_progress (
        id bigint not null auto_increment,
        chapter_correct integer not null,
        chapter_index integer not null,
        completed_at datetime(6),
        pending_answer varchar(255),
        pending_question varchar(500),
        quest_code varchar(40) not null,
        started_at datetime(6) not null,
        student_user_id varchar(255) not null,
        total_correct integer not null,
        updated_at datetime(6),
        wrong_attempts integer not null,
        primary key (id),
        constraint uk_quest_progress_student_quest unique (student_user_id, quest_code),
        index idx_quest_progress_student (student_user_id)
    ) engine=InnoDB;

create table if not exists student_cosmetic (
        id bigint not null auto_increment,
        equipped bit not null,
        item_code varchar(40) not null,
        kind varchar(16) not null,
        student_user_id varchar(255) not null,
        unlocked_at datetime(6) not null,
        primary key (id),
        constraint uk_student_cosmetic_item unique (student_user_id, item_code),
        index idx_student_cosmetic_student (student_user_id)
    ) engine=InnoDB;

create table if not exists portfolio_project (
        id bigint not null auto_increment,
        created_at datetime(6) not null,
        description varchar(1000),
        grade_name varchar(20),
        student_user_id varchar(255) not null,
        title varchar(120) not null,
        primary key (id),
        index idx_portfolio_project_student (student_user_id)
    ) engine=InnoDB;

create table if not exists portfolio_share (
        id bigint not null auto_increment,
        created_at datetime(6) not null,
        display_name varchar(60),
        student_user_id varchar(255) not null,
        token varchar(64) not null,
        primary key (id),
        constraint uk_portfolio_share_student unique (student_user_id),
        constraint uk_portfolio_share_token unique (token)
    ) engine=InnoDB;
