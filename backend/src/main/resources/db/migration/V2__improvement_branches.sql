-- V2: the tables, columns and indexes added by the improvement branches (auth, audit, exams,
-- adaptive learning, engagement, AI, teacher tools, school administration, communication).
-- Generated from the entities with SchemaExportTool and diffed against V1.
--
-- Written to be safe on a database where ddl-auto=update already created some of these
-- (dev machines that ran a branch): tables use IF NOT EXISTS, and every column, index and
-- constraint is added only when information_schema does not have it yet.

-- New tables

create table if not exists ai_conversation_message (
        id bigint not null auto_increment,
        content TEXT,
        created_at datetime(6) not null,
        flag_reason varchar(40),
        flagged bit not null,
        question_id bigint,
        role varchar(16) not null,
        school_code varchar(255),
        user_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists ai_open_answer (
        id bigint not null auto_increment,
        ai_reasoning TEXT,
        ai_score integer,
        answer_text TEXT not null,
        final_score integer,
        reviewed_at datetime(6),
        status enum ('APPROVED','SUBMITTED','SUGGESTED') not null,
        student_name varchar(255),
        student_user_id varchar(255),
        submitted_at datetime(6) not null,
        teacher_comment TEXT,
        rubric_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists ai_question_draft (
        id bigint not null auto_increment,
        answer varchar(255),
        bank_question_id bigint,
        created_at datetime(6) not null,
        difficulty varchar(255),
        explanation TEXT,
        grade_level varchar(255),
        prompt TEXT,
        question_text TEXT,
        school_code varchar(255),
        status enum ('APPROVED','PENDING','REJECTED') not null,
        sub_topic varchar(255),
        subject varchar(255),
        teacher_user_id varchar(255) not null,
        topic varchar(255),
        primary key (id)
    ) engine=InnoDB;

create table if not exists ai_rubric (
        id bigint not null auto_increment,
        created_at datetime(6) not null,
        max_score integer not null,
        published bit not null,
        question_text TEXT not null,
        rubric TEXT not null,
        school_code varchar(255),
        teacher_user_id varchar(255) not null,
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists ai_usage (
        id bigint not null auto_increment,
        cached bit not null,
        completion_tokens integer not null,
        cost_usd float(53) not null,
        created_at datetime(6) not null,
        feature varchar(40) not null,
        model varchar(255),
        prompt_tokens integer not null,
        provider varchar(255),
        school_code varchar(255),
        user_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists archived_student_record (
        id bigint not null auto_increment,
        average_score float(53),
        class_name varchar(255),
        grade_name varchar(255),
        next_class_name varchar(255),
        next_grade_name varchar(255),
        outcome varchar(255),
        student_name varchar(255),
        student_user_id varchar(255),
        tests_count integer not null,
        school_year_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists attendance_record (
        id bigint not null auto_increment,
        class_room_id bigint,
        class_room_name varchar(255),
        lesson_date date not null,
        lesson_id bigint not null,
        marked_at datetime(6),
        marked_by_teacher_id varchar(255),
        school_code varchar(255),
        status enum ('ABSENT','EXCUSED','LATE','PRESENT') not null,
        student_name varchar(255),
        student_user_id varchar(255) not null,
        subject varchar(255),
        primary key (id)
    ) engine=InnoDB;

create table if not exists audit_log (
        id bigint not null auto_increment,
        action enum ('FEATURE_FLAG_CHANGED','IMPERSONATION_STARTED','IMPERSONATION_STOPPED','LESSON_ADDED','MANAGER_ASSIGNED_TO_SCHOOL','MANAGER_REMOVED_FROM_SCHOOL','SCHOOL_GRADES_ADDED','SCHOOL_GRADES_REMOVED','STUDENT_ASSIGNED_TO_CLASS','STUDENT_CLASS_CHANGED','TEACHER_ASSIGNED_TO_CLASSES','TEACHER_REMOVED_FROM_CLASS','TEACHER_REMOVED_FROM_SCHOOL','TEACHER_TEST_SCORE_RECORDED','USER_ASSIGNED_AS_TEACHER','USER_PROMOTED_TO_MANAGER') not null,
        actor_role varchar(255),
        actor_user_id varchar(255),
        actor_username varchar(255),
        after_value varchar(2000),
        before_value varchar(2000),
        created_at datetime(6) not null,
        entity_id varchar(255),
        entity_type varchar(64),
        impersonator_id varchar(255),
        ip varchar(64),
        school_code varchar(255),
        primary key (id)
    ) engine=InnoDB;

create table if not exists bank_question (
        id bigint not null auto_increment,
        answer varchar(500) not null,
        attempts integer not null,
        correct_attempts integer not null,
        created_at datetime(6),
        explanation TEXT,
        grade varchar(255),
        level varchar(255),
        options varchar(2000),
        owner_name varchar(255),
        owner_teacher_id varchar(255) not null,
        question_text varchar(2000) not null,
        school_code varchar(255) not null,
        shared bit not null,
        source varchar(16),
        sub_topic varchar(255),
        subject varchar(255),
        times_asked integer not null,
        topic varchar(255),
        primary key (id)
    ) engine=InnoDB;

create table if not exists calendar_token_entity (
        id bigint not null auto_increment,
        created_at datetime(6) not null,
        token varchar(64) not null,
        user_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists class_challenge (
        id bigint not null auto_increment,
        class_room_id bigint not null,
        end_date date,
        start_date date,
        target_xp integer not null,
        teacher_user_id varchar(255) not null,
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists exam_attempt (
        id bigint not null auto_increment,
        correct_count integer,
        deadline datetime(6),
        saved_answers_json TEXT,
        score integer,
        seed bigint not null,
        started_at datetime(6),
        status enum ('AUTO_SUBMITTED','IN_PROGRESS','SUBMITTED'),
        student_user_id varchar(255),
        submitted_at datetime(6),
        tab_leave_count integer not null,
        version bigint,
        test_id bigint,
        primary key (id)
    ) engine=InnoDB;

create table if not exists exam_attempt_answer (
        id bigint not null auto_increment,
        answer TEXT,
        correct bit not null,
        points_earned integer not null,
        question_id bigint,
        attempt_id bigint,
        primary key (id)
    ) engine=InnoDB;

create table if not exists exam_integrity_event (
        id bigint not null auto_increment,
        occurred_at datetime(6),
        type enum ('TAB_HIDDEN','TAB_VISIBLE','WINDOW_BLUR','WINDOW_FOCUS'),
        attempt_id bigint,
        primary key (id)
    ) engine=InnoDB;

create table if not exists gradebook_assignment (
        id bigint not null auto_increment,
        assigned_date date,
        category enum ('EXAM','HOMEWORK','PROJECT','QUIZ') not null,
        class_room_id bigint not null,
        max_score float(53) not null,
        teacher_id varchar(255) not null,
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists gradebook_grade (
        id bigint not null auto_increment,
        score float(53) not null,
        student_user_id varchar(255) not null,
        assignment_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists gradebook_weight (
        id bigint not null auto_increment,
        category enum ('EXAM','HOMEWORK','PROJECT','QUIZ') not null,
        class_room_id bigint not null,
        teacher_id varchar(255) not null,
        weight integer not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists homework (
        id bigint not null auto_increment,
        created_at datetime(6),
        description TEXT,
        due_date datetime(6) not null,
        reminder_sent bit not null,
        subject varchar(255),
        teacher_user_id varchar(255) not null,
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists homework_student (
        homework_id bigint not null,
        student_user_id varchar(255)
    ) engine=InnoDB;

create table if not exists homework_submission (
        id bigint not null auto_increment,
        content TEXT,
        late bit not null,
        student_user_id varchar(255) not null,
        submitted_at datetime(6),
        homework_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists lesson_content (
        id bigint not null auto_increment,
        body TEXT,
        created_at datetime(6),
        sub_topic varchar(255),
        subject varchar(255),
        teacher_user_id varchar(255) not null,
        title varchar(255) not null,
        topic varchar(255),
        updated_at datetime(6),
        video_embed_url varchar(255),
        primary key (id)
    ) engine=InnoDB;

create table if not exists lesson_content_example (
        lesson_content_id bigint not null,
        example_text TEXT,
        example_order integer not null,
        primary key (lesson_content_id, example_order)
    ) engine=InnoDB;

create table if not exists lesson_progress (
        id bigint not null auto_increment,
        learned bit not null,
        lesson_content_id bigint not null,
        practiced bit not null,
        reviewed bit not null,
        student_user_id varchar(255) not null,
        updated_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

create table if not exists message_attachment_entity (
        id bigint not null auto_increment,
        content_type varchar(255) not null,
        created_at datetime(6) not null,
        data LONGBLOB not null,
        file_name varchar(255) not null,
        size_bytes bigint not null,
        uploader_user_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists notification_entity (
        id bigint not null auto_increment,
        body varchar(2000),
        created_at datetime(6) not null,
        link varchar(255),
        read_at datetime(6),
        title varchar(255) not null,
        type enum ('HOMEWORK_REMINDER','LIVE_QUIZ_RESULTS','LIVE_QUIZ_STATE','NEW_HOMEWORK','NEW_TEST','SYSTEM_MESSAGE') not null,
        user_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists notification_preference_entity (
        id bigint not null auto_increment,
        email bit not null,
        in_app bit not null,
        push bit not null,
        user_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists parent_student (
        parent_id bigint not null,
        student_id bigint not null
    ) engine=InnoDB;

create table if not exists parent_entity (
        id bigint not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists push_subscription_entity (
        id bigint not null auto_increment,
        auth varchar(255) not null,
        created_at datetime(6) not null,
        endpoint varchar(700) not null,
        p256dh varchar(255) not null,
        user_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists refresh_tokens (
        id bigint not null auto_increment,
        created_at datetime(6) not null,
        expires_at datetime(6) not null,
        family_id varchar(36) not null,
        revoked_at datetime(6),
        token_hash varchar(64) not null,
        user_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists review_item (
        id bigint not null auto_increment,
        answer varchar(500),
        created_at datetime(6),
        due_date date not null,
        easiness float(53) not null,
        interval_days integer not null,
        lapses integer not null,
        last_reviewed_at datetime(6),
        question_id bigint not null,
        question_text varchar(1000),
        repetitions integer not null,
        review_count integer not null,
        source varchar(255),
        student_user_id varchar(255) not null,
        sub_topic varchar(255),
        subject varchar(255),
        topic varchar(255),
        primary key (id)
    ) engine=InnoDB;

create table if not exists school_feature_flag (
        id bigint not null auto_increment,
        enabled bit not null,
        feature enum ('AI_CHAT','MESSAGES','PRACTICE_TESTS','TEACHER_TESTS') not null,
        school_code varchar(255) not null,
        updated_at datetime(6),
        updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

create table if not exists school_year (
        id bigint not null auto_increment,
        active bit not null,
        closed bit not null,
        closed_at datetime(6),
        end_date date,
        name varchar(255) not null,
        start_date date,
        school_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists semester (
        id bigint not null auto_increment,
        end_date date,
        name varchar(255) not null,
        start_date date,
        school_year_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists student_accommodation (
        id bigint not null auto_increment,
        dyslexia_font bit not null,
        extra_time_percent integer not null,
        read_aloud bit not null,
        student_user_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists student_badge (
        id bigint not null auto_increment,
        code varchar(255) not null,
        description varchar(255),
        earned_at datetime(6),
        student_user_id varchar(255) not null,
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists student_daily_activity (
        id bigint not null auto_increment,
        activity_date date not null,
        student_user_id varchar(255) not null,
        xp integer not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists student_engagement (
        id bigint not null auto_increment,
        current_streak integer not null,
        last_active_date date,
        longest_streak integer not null,
        student_user_id varchar(255) not null,
        total_xp integer not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists study_plan_task (
        id bigint not null auto_increment,
        completed bit not null,
        completed_at datetime(6),
        description varchar(255),
        minutes integer not null,
        student_user_id varchar(255) not null,
        subject varchar(255),
        task_date date not null,
        task_type varchar(255),
        test_id bigint not null,
        topic varchar(255),
        primary key (id)
    ) engine=InnoDB;

create table if not exists sub_topic_hint (
        id bigint not null auto_increment,
        hint1 varchar(1000),
        hint2 varchar(1000),
        solution_steps varchar(4000),
        sub_topic varchar(255) not null,
        subject varchar(255) not null,
        topic varchar(255),
        updated_at datetime(6),
        updated_by_teacher_id varchar(255),
        primary key (id)
    ) engine=InnoDB;

create table if not exists teacher_insight (
        id bigint not null auto_increment,
        ai_generated bit not null,
        created_at datetime(6) not null,
        period_end datetime(6),
        period_start datetime(6),
        school_code varchar(255),
        stats_json MEDIUMTEXT,
        summary TEXT,
        teacher_user_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table if not exists test_answer_record (
        id bigint not null auto_increment,
        class_room_id bigint,
        correct bit not null,
        correct_answer varchar(500),
        created_at datetime(6),
        given_answer varchar(500),
        question_id bigint,
        question_text TEXT,
        seconds_spent integer,
        source varchar(255),
        student_user_id varchar(255) not null,
        sub_topic varchar(255),
        subject varchar(255),
        teacher_user_id varchar(255),
        test_id bigint not null,
        topic varchar(255),
        primary key (id)
    ) engine=InnoDB;

create table if not exists topic_mastery (
        id bigint not null auto_increment,
        attempts integer not null,
        correct_count integer not null,
        rating float(53) not null,
        student_user_id varchar(255) not null,
        sub_topic varchar(255) not null,
        subject varchar(255),
        topic varchar(255),
        updated_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

-- New columns on existing tables (all nullable)

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lesson_entity' AND column_name = 'room') = 0, 'alter table lesson_entity add column room varchar(255)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'message_entity' AND column_name = 'attachment_id') = 0, 'alter table message_entity add column attachment_id bigint', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'message_entity' AND column_name = 'batch_id') = 0, 'alter table message_entity add column batch_id varchar(36)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'message_entity' AND column_name = 'parent_message_id') = 0, 'alter table message_entity add column parent_message_id bigint', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'message_entity' AND column_name = 'pending') = 0, 'alter table message_entity add column pending bit', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'message_entity' AND column_name = 'read_at') = 0, 'alter table message_entity add column read_at datetime(6)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'message_entity' AND column_name = 'scheduled_for') = 0, 'alter table message_entity add column scheduled_for datetime(6)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'message_entity' AND column_name = 'thread_id') = 0, 'alter table message_entity add column thread_id varchar(36)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'practice_question_entity' AND column_name = 'hints_used') = 0, 'alter table practice_question_entity add column hints_used integer', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'practice_question_entity' AND column_name = 'points_earned') = 0, 'alter table practice_question_entity add column points_earned integer', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'question_entity' AND column_name = 'difficulty_level') = 0, 'alter table question_entity add column difficulty_level integer', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'teacher_test_entity' AND column_name = 'exam_group_id') = 0, 'alter table teacher_test_entity add column exam_group_id varchar(255)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'teacher_test_entity' AND column_name = 'publish_at') = 0, 'alter table teacher_test_entity add column publish_at datetime(6)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'teacher_test_entity' AND column_name = 'published') = 0, 'alter table teacher_test_entity add column published bit', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'teacher_test_entity' AND column_name = 'shuffle_options') = 0, 'alter table teacher_test_entity add column shuffle_options bit', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'teacher_test_entity' AND column_name = 'shuffle_questions') = 0, 'alter table teacher_test_entity add column shuffle_questions bit', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'teacher_test_entity' AND column_name = 'source_test_id') = 0, 'alter table teacher_test_entity add column source_test_id bigint', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'teacher_test_entity' AND column_name = 'template') = 0, 'alter table teacher_test_entity add column template bit', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'teacher_test_entity' AND column_name = 'title') = 0, 'alter table teacher_test_entity add column title varchar(255)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'teacher_test_entity' AND column_name = 'version_label') = 0, 'alter table teacher_test_entity add column version_label varchar(255)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'test_question_entity' AND column_name = 'answer_key_json') = 0, 'alter table test_question_entity add column answer_key_json TEXT', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'test_question_entity' AND column_name = 'kind') = 0, 'alter table test_question_entity add column kind varchar(255)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'test_question_entity' AND column_name = 'options_json') = 0, 'alter table test_question_entity add column options_json TEXT', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'test_question_entity' AND column_name = 'points') = 0, 'alter table test_question_entity add column points integer', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'test_question_entity' AND column_name = 'position') = 0, 'alter table test_question_entity add column position integer', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'test_question_entity' AND column_name = 'prompt') = 0, 'alter table test_question_entity add column prompt TEXT', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'test_question_entity' AND column_name = 'tolerance') = 0, 'alter table test_question_entity add column tolerance float(53)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

-- user_entity.role was a native ENUM without PARENT; store it as text like the entity maps it

alter table user_entity modify column role varchar(32) not null;

-- Indexes, unique constraints and foreign keys

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'ai_conversation_message' AND index_name = 'idx_ai_conv_user_question') = 0, 'create index idx_ai_conv_user_question on ai_conversation_message (user_id, question_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'ai_conversation_message' AND index_name = 'idx_ai_conv_created') = 0, 'create index idx_ai_conv_created on ai_conversation_message (created_at)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'ai_open_answer' AND index_name = 'idx_ai_answer_rubric') = 0, 'create index idx_ai_answer_rubric on ai_open_answer (rubric_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'ai_open_answer' AND index_name = 'idx_ai_answer_student') = 0, 'create index idx_ai_answer_student on ai_open_answer (student_user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'ai_question_draft' AND index_name = 'idx_ai_draft_teacher_status') = 0, 'create index idx_ai_draft_teacher_status on ai_question_draft (teacher_user_id, status)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'ai_rubric' AND index_name = 'idx_ai_rubric_teacher') = 0, 'create index idx_ai_rubric_teacher on ai_rubric (teacher_user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'ai_rubric' AND index_name = 'idx_ai_rubric_school') = 0, 'create index idx_ai_rubric_school on ai_rubric (school_code, published)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'ai_usage' AND index_name = 'idx_ai_usage_user_time') = 0, 'create index idx_ai_usage_user_time on ai_usage (user_id, created_at)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'ai_usage' AND index_name = 'idx_ai_usage_school_time') = 0, 'create index idx_ai_usage_school_time on ai_usage (school_code, created_at)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'archived_student_record' AND index_name = 'idx_archive_year') = 0, 'create index idx_archive_year on archived_student_record (school_year_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'attendance_record' AND index_name = 'idx_attendance_school_date') = 0, 'create index idx_attendance_school_date on attendance_record (school_code, lesson_date)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'attendance_record' AND index_name = 'idx_attendance_student_date') = 0, 'create index idx_attendance_student_date on attendance_record (student_user_id, lesson_date)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'attendance_record' AND index_name = 'uk_attendance_lesson_date_student') = 0, 'alter table attendance_record add constraint uk_attendance_lesson_date_student unique (lesson_id, lesson_date, student_user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_created_at') = 0, 'create index idx_audit_created_at on audit_log (created_at)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_school_code') = 0, 'create index idx_audit_school_code on audit_log (school_code)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_actor') = 0, 'create index idx_audit_actor on audit_log (actor_user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'bank_question' AND index_name = 'idx_bank_question_owner') = 0, 'create index idx_bank_question_owner on bank_question (owner_teacher_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'bank_question' AND index_name = 'idx_bank_question_school') = 0, 'create index idx_bank_question_school on bank_question (school_code)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'calendar_token_entity' AND index_name = 'uk_calendar_token_user') = 0, 'alter table calendar_token_entity add constraint uk_calendar_token_user unique (user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'calendar_token_entity' AND index_name = 'uk_calendar_token_token') = 0, 'alter table calendar_token_entity add constraint uk_calendar_token_token unique (token)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'class_room_entity' AND index_name = 'idx_class_room_name') = 0, 'create index idx_class_room_name on class_room_entity (name)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'exam_attempt' AND index_name = 'UKlg0xqs82o5cfe6yjmxryrwgob') = 0, 'alter table exam_attempt add constraint UKlg0xqs82o5cfe6yjmxryrwgob unique (test_id, student_user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'gradebook_assignment' AND index_name = 'idx_gradebook_assignment_teacher_class') = 0, 'create index idx_gradebook_assignment_teacher_class on gradebook_assignment (teacher_id, class_room_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'gradebook_grade' AND index_name = 'uk_gradebook_grade_assignment_student') = 0, 'alter table gradebook_grade add constraint uk_gradebook_grade_assignment_student unique (assignment_id, student_user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'gradebook_weight' AND index_name = 'uk_gradebook_weight') = 0, 'alter table gradebook_weight add constraint uk_gradebook_weight unique (teacher_id, class_room_id, category)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'homework' AND index_name = 'idx_homework_teacher') = 0, 'create index idx_homework_teacher on homework (teacher_user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'homework' AND index_name = 'idx_homework_due') = 0, 'create index idx_homework_due on homework (due_date)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'homework_submission' AND index_name = 'idx_submission_homework_student') = 0, 'create index idx_submission_homework_student on homework_submission (homework_id, student_user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'lesson_content' AND index_name = 'idx_lesson_content_teacher') = 0, 'create index idx_lesson_content_teacher on lesson_content (teacher_user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'lesson_progress' AND index_name = 'idx_lesson_progress_student') = 0, 'create index idx_lesson_progress_student on lesson_progress (student_user_id, lesson_content_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'message_entity' AND index_name = 'idx_message_batch') = 0, 'create index idx_message_batch on message_entity (batch_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'message_entity' AND index_name = 'idx_message_thread') = 0, 'create index idx_message_thread on message_entity (thread_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'message_entity' AND index_name = 'idx_message_pending_schedule') = 0, 'create index idx_message_pending_schedule on message_entity (pending, scheduled_for)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'notification_entity' AND index_name = 'idx_notification_user_id') = 0, 'create index idx_notification_user_id on notification_entity (user_id, id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'notification_entity' AND index_name = 'idx_notification_user_read') = 0, 'create index idx_notification_user_read on notification_entity (user_id, read_at)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'notification_preference_entity' AND index_name = 'uk_notification_pref_user') = 0, 'alter table notification_preference_entity add constraint uk_notification_pref_user unique (user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'push_subscription_entity' AND index_name = 'idx_push_subscription_user') = 0, 'create index idx_push_subscription_user on push_subscription_entity (user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'push_subscription_entity' AND index_name = 'idx_push_subscription_endpoint') = 0, 'create index idx_push_subscription_endpoint on push_subscription_entity (endpoint)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'refresh_tokens' AND index_name = 'idx_refresh_tokens_family') = 0, 'create index idx_refresh_tokens_family on refresh_tokens (family_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'refresh_tokens' AND index_name = 'idx_refresh_tokens_user') = 0, 'create index idx_refresh_tokens_user on refresh_tokens (user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'refresh_tokens' AND index_name = 'UKo2mlirhldriil2y7krapq4frt') = 0, 'alter table refresh_tokens add constraint UKo2mlirhldriil2y7krapq4frt unique (token_hash)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'review_item' AND index_name = 'idx_review_student_due') = 0, 'create index idx_review_student_due on review_item (student_user_id, due_date)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'review_item' AND index_name = 'UK2v35x3c8c0rycdtwfehprdrri') = 0, 'alter table review_item add constraint UK2v35x3c8c0rycdtwfehprdrri unique (student_user_id, question_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'school_feature_flag' AND index_name = 'uk_school_feature') = 0, 'alter table school_feature_flag add constraint uk_school_feature unique (school_code, feature)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'student_accommodation' AND index_name = 'idx_accommodation_student') = 0, 'alter table student_accommodation add constraint idx_accommodation_student unique (student_user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'student_badge' AND index_name = 'idx_badge_student') = 0, 'create index idx_badge_student on student_badge (student_user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'student_daily_activity' AND index_name = 'idx_daily_activity_student_date') = 0, 'create index idx_daily_activity_student_date on student_daily_activity (student_user_id, activity_date)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'student_engagement' AND index_name = 'idx_engagement_student') = 0, 'alter table student_engagement add constraint idx_engagement_student unique (student_user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'study_plan_task' AND index_name = 'idx_study_plan_student_test') = 0, 'create index idx_study_plan_student_test on study_plan_task (student_user_id, test_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sub_topic_hint' AND index_name = 'UKsy3hosghpju82w84xqkh5ybvc') = 0, 'alter table sub_topic_hint add constraint UKsy3hosghpju82w84xqkh5ybvc unique (subject, sub_topic)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'teacher_insight' AND index_name = 'idx_teacher_insight_teacher') = 0, 'create index idx_teacher_insight_teacher on teacher_insight (teacher_user_id, created_at)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'test_answer_record' AND index_name = 'idx_answer_record_test_student') = 0, 'create index idx_answer_record_test_student on test_answer_record (test_id, student_user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'test_answer_record' AND index_name = 'idx_answer_record_class_subtopic') = 0, 'create index idx_answer_record_class_subtopic on test_answer_record (class_room_id, sub_topic)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'test_answer_record' AND index_name = 'idx_answer_record_teacher_time') = 0, 'create index idx_answer_record_teacher_time on test_answer_record (teacher_user_id, created_at)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'topic_mastery' AND index_name = 'idx_mastery_student') = 0, 'create index idx_mastery_student on topic_mastery (student_user_id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'topic_mastery' AND index_name = 'UKq61wjstlrcgwiuimxntwbesla') = 0, 'alter table topic_mastery add constraint UKq61wjstlrcgwiuimxntwbesla unique (student_user_id, subject, sub_topic)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user_entity' AND index_name = 'idx_user_username') = 0, 'create index idx_user_username on user_entity (username)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'ai_open_answer' AND constraint_name = 'FKhv1xgxdy084cihxfs0kjv1x87') = 0, 'alter table ai_open_answer add constraint FKhv1xgxdy084cihxfs0kjv1x87 foreign key (rubric_id) references ai_rubric (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'archived_student_record' AND constraint_name = 'FKlm7pjp6p4mw9bau01knasgh78') = 0, 'alter table archived_student_record add constraint FKlm7pjp6p4mw9bau01knasgh78 foreign key (school_year_id) references school_year (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'exam_attempt' AND constraint_name = 'FKca2u0yfutgbbq6898i85ixm6q') = 0, 'alter table exam_attempt add constraint FKca2u0yfutgbbq6898i85ixm6q foreign key (test_id) references teacher_test_entity (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'exam_attempt_answer' AND constraint_name = 'FK2rdgn9mb4qhg4sv7yqgav4q56') = 0, 'alter table exam_attempt_answer add constraint FK2rdgn9mb4qhg4sv7yqgav4q56 foreign key (attempt_id) references exam_attempt (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'exam_integrity_event' AND constraint_name = 'FKs6m9na2thvtiuv8w2f387j2at') = 0, 'alter table exam_integrity_event add constraint FKs6m9na2thvtiuv8w2f387j2at foreign key (attempt_id) references exam_attempt (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'gradebook_grade' AND constraint_name = 'FK5hs77ohjbur37uusvjv4j26uo') = 0, 'alter table gradebook_grade add constraint FK5hs77ohjbur37uusvjv4j26uo foreign key (assignment_id) references gradebook_assignment (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'homework_student' AND constraint_name = 'FK1541vxm09nrpq99v32c5g71yc') = 0, 'alter table homework_student add constraint FK1541vxm09nrpq99v32c5g71yc foreign key (homework_id) references homework (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'homework_submission' AND constraint_name = 'FKnu0sc981pdy7tb31a1u5rgjoa') = 0, 'alter table homework_submission add constraint FKnu0sc981pdy7tb31a1u5rgjoa foreign key (homework_id) references homework (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'lesson_content_example' AND constraint_name = 'FK8cvr413n6ps5f9psjnbl25xmh') = 0, 'alter table lesson_content_example add constraint FK8cvr413n6ps5f9psjnbl25xmh foreign key (lesson_content_id) references lesson_content (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'parent_student' AND constraint_name = 'FK8bxqsbk4ij96554u4qgoeatrt') = 0, 'alter table parent_student add constraint FK8bxqsbk4ij96554u4qgoeatrt foreign key (student_id) references student_entity (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'parent_student' AND constraint_name = 'FK64jnwghyn3pcaksokjhytspoa') = 0, 'alter table parent_student add constraint FK64jnwghyn3pcaksokjhytspoa foreign key (parent_id) references parent_entity (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'parent_entity' AND constraint_name = 'FKlmr1v86k95pn1jyr5cj16gs3r') = 0, 'alter table parent_entity add constraint FKlmr1v86k95pn1jyr5cj16gs3r foreign key (id) references user_entity (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'school_year' AND constraint_name = 'FK51snoprhe68b1xxdl22yr0h0w') = 0, 'alter table school_year add constraint FK51snoprhe68b1xxdl22yr0h0w foreign key (school_id) references school_entity (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'semester' AND constraint_name = 'FK4k88c9a87iq730e36wcjgxt6l') = 0, 'alter table semester add constraint FK4k88c9a87iq730e36wcjgxt6l foreign key (school_year_id) references school_year (id)', 'DO 0');
PREPARE ddl_stmt FROM @ddl;
EXECUTE ddl_stmt;
DEALLOCATE PREPARE ddl_stmt;

