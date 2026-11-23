-- V12 [s10]: weekly reflection journal, pre-exam mood check-ins and the student's consent to
-- alert the school staff on a distress pattern. Matches StudentReflectionEntity,
-- StudentMoodCheckinEntity and StudentWellbeingConsentEntity.
-- IF NOT EXISTS keeps it safe on a dev database where ddl-auto=update created them first.

create table if not exists student_reflection (
        id bigint not null auto_increment,
        achieved bit,
        created_at datetime(6) not null,
        goal_progress integer not null,
        goal_target integer,
        goal_type varchar(30) not null,
        next_goal TEXT,
        stuck_on TEXT,
        student_user_id varchar(255) not null,
        updated_at datetime(6),
        week_start date not null,
        went_well TEXT,
        primary key (id),
        constraint uk_reflection_student_week unique (student_user_id, week_start)
    ) engine=InnoDB;

create table if not exists student_mood_checkin (
        id bigint not null auto_increment,
        checkin_date date not null,
        created_at datetime(6) not null,
        distress bit not null,
        escalated bit not null,
        exam_label varchar(255),
        mood varchar(20) not null,
        student_user_id varchar(255) not null,
        primary key (id),
        index idx_mood_checkin_student_date (student_user_id, checkin_date)
    ) engine=InnoDB;

create table if not exists student_wellbeing_consent (
        student_user_id varchar(255) not null,
        consented bit not null,
        consented_at datetime(6),
        policy_version varchar(20),
        revoked_at datetime(6),
        primary key (student_user_id)
    ) engine=InnoDB;
