-- V6 [s04]: flashcards (AI-generated or manual, shareable with the class), each student's SM-2
-- schedule for them, and the personal study notebook (notes, highlights, bookmarks).
-- Matches FlashcardEntity, FlashcardReviewEntity and StudyNoteEntity. Tables use IF NOT EXISTS
-- so a dev database where ddl-auto=update already created them is not a problem.

create table if not exists flashcard (
        id bigint not null auto_increment,
        back varchar(1000) not null,
        class_room_id bigint,
        created_at datetime(6) not null,
        front varchar(500) not null,
        owner_user_id varchar(255) not null,
        source varchar(16) not null,
        subject varchar(255),
        topic varchar(255),
        primary key (id),
        index idx_flashcard_owner (owner_user_id),
        index idx_flashcard_class (class_room_id)
    ) engine=InnoDB;

create table if not exists flashcard_review (
        id bigint not null auto_increment,
        created_at datetime(6),
        due_date date not null,
        easiness float(53) not null,
        flashcard_id bigint not null,
        interval_days integer not null,
        lapses integer not null,
        last_reviewed_at datetime(6),
        repetitions integer not null,
        review_count integer not null,
        student_user_id varchar(255) not null,
        primary key (id),
        constraint uk_flashcard_review_student_card unique (student_user_id, flashcard_id),
        index idx_flashcard_review_student_due (student_user_id, due_date)
    ) engine=InnoDB;

create table if not exists study_note (
        id bigint not null auto_increment,
        body varchar(2000),
        created_at datetime(6) not null,
        kind varchar(16) not null,
        quote varchar(1000),
        student_user_id varchar(255) not null,
        sub_topic varchar(255),
        subject varchar(255),
        target_id bigint,
        target_title varchar(500),
        target_type varchar(16) not null,
        topic varchar(255),
        updated_at datetime(6),
        primary key (id),
        index idx_study_note_student_topic (student_user_id, topic),
        index idx_study_note_student_target (student_user_id, target_type, target_id)
    ) engine=InnoDB;
