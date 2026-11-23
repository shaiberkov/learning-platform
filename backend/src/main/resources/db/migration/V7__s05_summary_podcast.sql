-- V7 [s05]: personal summary sheet before a test and the study podcast (spoken summary).
-- Matches StudySheetEntity, StudyPodcastEntity and StudyPodcastAudioEntity. IF NOT EXISTS keeps it
-- safe on a dev database where ddl-auto=update already created the tables.

create table if not exists study_sheet (
        id bigint not null auto_increment,
        ai_generated bit not null,
        content TEXT not null,
        created_at datetime(6) not null,
        student_user_id varchar(255) not null,
        test_id bigint,
        title varchar(255) not null,
        topics varchar(1000),
        primary key (id),
        index idx_study_sheet_student (student_user_id, created_at)
    ) engine=InnoDB;

create table if not exists study_podcast (
        id bigint not null auto_increment,
        ai_generated bit not null,
        created_at datetime(6) not null,
        has_audio bit not null,
        script TEXT not null,
        student_user_id varchar(255) not null,
        test_id bigint,
        title varchar(255) not null,
        topics varchar(1000),
        primary key (id),
        index idx_study_podcast_student (student_user_id, created_at)
    ) engine=InnoDB;

create table if not exists study_podcast_audio (
        podcast_id bigint not null,
        audio LONGBLOB not null,
        content_type varchar(64) not null,
        created_at datetime(6) not null,
        primary key (podcast_id)
    ) engine=InnoDB;
