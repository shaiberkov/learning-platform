-- V3 [s01]: shared study rooms and focus-mode sessions (StudyRoomEntity, FocusSessionEntity).
-- Tables use IF NOT EXISTS, so a dev database where ddl-auto=update already created them is fine.

create table if not exists study_room (
        id bigint not null auto_increment,
        active bit not null,
        created_at datetime(6) not null,
        created_by_user_id varchar(255) not null,
        last_activity_at datetime(6) not null,
        name varchar(60) not null,
        school_code varchar(255) not null,
        timer_started_at datetime(6) not null,
        primary key (id),
        index idx_study_room_school_active (school_code, active)
    ) engine=InnoDB;

create table if not exists focus_session (
        id bigint not null auto_increment,
        actual_minutes integer not null,
        completed bit not null,
        ended_at datetime(6) not null,
        planned_minutes integer not null,
        started_at datetime(6) not null,
        student_user_id varchar(255) not null,
        task varchar(80),
        primary key (id),
        index idx_focus_session_student_time (student_user_id, started_at)
    ) engine=InnoDB;
