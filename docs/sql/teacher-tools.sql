-- Teacher tools (question bank, gradebook, attendance): tables for MySQL 8.
-- The dev profile creates these itself (ddl-auto=update); production runs ddl-auto=none, so run this once
-- there before deploying. Generated from the JPA entities with Hibernate's MySQL dialect.

create table bank_question (
    attempts integer not null,
    correct_attempts integer not null,
    shared bit not null,
    times_asked integer not null,
    created_at datetime(6),
    id bigint not null auto_increment,
    answer varchar(500) not null,
    options varchar(2000),
    question_text varchar(2000) not null,
    grade varchar(255),
    level varchar(255),
    owner_name varchar(255),
    owner_teacher_id varchar(255) not null,
    school_code varchar(255) not null,
    subject varchar(255),
    topic varchar(255),
    primary key (id)
) engine=InnoDB;

create index idx_bank_question_owner on bank_question (owner_teacher_id);
create index idx_bank_question_school on bank_question (school_code);

create table gradebook_assignment (
    assigned_date date,
    max_score float(53) not null,
    class_room_id bigint not null,
    id bigint not null auto_increment,
    teacher_id varchar(255) not null,
    title varchar(255) not null,
    category enum ('EXAM','HOMEWORK','PROJECT','QUIZ') not null,
    primary key (id)
) engine=InnoDB;

create index idx_gradebook_assignment_teacher_class on gradebook_assignment (teacher_id, class_room_id);

create table gradebook_grade (
    score float(53) not null,
    assignment_id bigint not null,
    id bigint not null auto_increment,
    student_user_id varchar(255) not null,
    primary key (id)
) engine=InnoDB;

alter table gradebook_grade
    add constraint uk_gradebook_grade_assignment_student unique (assignment_id, student_user_id);
alter table gradebook_grade
    add constraint fk_gradebook_grade_assignment foreign key (assignment_id) references gradebook_assignment (id);

create table gradebook_weight (
    weight integer not null,
    class_room_id bigint not null,
    id bigint not null auto_increment,
    teacher_id varchar(255) not null,
    category enum ('EXAM','HOMEWORK','PROJECT','QUIZ') not null,
    primary key (id)
) engine=InnoDB;

alter table gradebook_weight
    add constraint uk_gradebook_weight unique (teacher_id, class_room_id, category);

create table attendance_record (
    lesson_date date not null,
    class_room_id bigint,
    id bigint not null auto_increment,
    lesson_id bigint not null,
    marked_at datetime(6),
    class_room_name varchar(255),
    marked_by_teacher_id varchar(255),
    school_code varchar(255),
    student_name varchar(255),
    student_user_id varchar(255) not null,
    subject varchar(255),
    status enum ('ABSENT','EXCUSED','LATE','PRESENT') not null,
    primary key (id)
) engine=InnoDB;

create index idx_attendance_school_date on attendance_record (school_code, lesson_date);
create index idx_attendance_student_date on attendance_record (student_user_id, lesson_date);
alter table attendance_record
    add constraint uk_attendance_lesson_date_student unique (lesson_id, lesson_date, student_user_id);
