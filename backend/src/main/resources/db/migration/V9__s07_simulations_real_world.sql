-- V9 [s07]: real-world connection cards (teacher override per school and topic) and the
-- record of which interactive simulations a student explored.
-- Tables use IF NOT EXISTS so a dev database that ran ddl-auto=update stays valid.

create table if not exists real_world_override (
        id bigint not null auto_increment,
        career_link varchar(1000) not null,
        everyday_example varchar(1000) not null,
        school_code varchar(64) not null,
        topic varchar(120) not null,
        updated_at datetime(6),
        updated_by_teacher_id varchar(255),
        primary key (id),
        constraint uk_real_world_override_school_topic unique (school_code, topic)
    ) engine=InnoDB;

create table if not exists simulation_exploration (
        id bigint not null auto_increment,
        explore_count integer not null,
        first_explored_at datetime(6) not null,
        last_explored_at datetime(6) not null,
        simulation_key varchar(64) not null,
        student_user_id varchar(255) not null,
        primary key (id),
        constraint uk_simulation_exploration_student_key unique (student_user_id, simulation_key)
    ) engine=InnoDB;
