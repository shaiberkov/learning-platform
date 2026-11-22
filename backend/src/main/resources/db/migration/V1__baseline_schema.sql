-- V1 baseline: the schema as the JPA entities map it on 2026-11-23 (Hibernate 6.6, MySQLDialect).
-- Generated with: ./mvnw test -Dtest=SchemaExportTool -Dschema.export=target/schema.sql
--
-- Existing databases (dev, production) already have these tables: spring.flyway.baseline-on-migrate=true
-- records them as version 1 without running this file. It only runs on an empty schema
-- (a new environment, the Testcontainers integration tests). Never edit it after it shipped:
-- schema changes go in V2__..., V3__... .

    create table answered_questions (
        history_id integer not null,
        is_answered_correct bit,
        question_id bigint not null,
        primary key (history_id, question_id)
    ) engine=InnoDB;

    create table class_room_entity (
        id bigint not null auto_increment,
        name varchar(255) not null,
        grade_id bigint,
        school_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table lesson_entity (
        id bigint not null auto_increment,
        day_of_week enum ('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY'),
        end_time time(6),
        start_time time(6),
        subject varchar(255),
        schedule_id bigint,
        teacher_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table message_entity (
        id bigint not null auto_increment,
        content varchar(255) not null,
        sent_at datetime(6) not null,
        title varchar(255) not null,
        receiver_id bigint not null,
        sender_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table practice_question_entity (
        id bigint not null,
        student_progress_id bigint,
        question_history_id integer,
        primary key (id)
    ) engine=InnoDB;

    create table practice_test_entity (
        id bigint not null,
        student_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table practice_test_result_entity (
        id bigint not null,
        student_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table question_entity (
        question_type varchar(31) not null,
        id bigint not null auto_increment,
        answer varchar(255),
        question_text varchar(255),
        sub_topic varchar(255),
        subject varchar(255),
        topic varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table schedule_entity (
        id bigint not null auto_increment,
        class_room_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table school_entity (
        id bigint not null auto_increment,
        school_code varchar(255) not null,
        school_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table school_grade_entity (
        id bigint not null auto_increment,
        grade_name varchar(255) not null,
        school_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table school_manager_entity (
        id bigint not null,
        school_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table session (
        id bigint not null auto_increment,
        last_activity datetime(6) not null,
        user_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table skill_levels_by_sub_topic (
        progress_id bigint not null,
        skill_level integer,
        skill_levels_by_sub_topic_key varchar(255) not null,
        primary key (progress_id, skill_levels_by_sub_topic_key)
    ) engine=InnoDB;

    create table student_entity (
        id bigint not null,
        class_room_id bigint,
        school_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table student_progress_entity (
        id bigint not null auto_increment,
        student_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table student_question_history_entity (
        id integer not null auto_increment,
        student_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table sub_topic_incorrect_streak (
        progress_id bigint not null,
        incorrect_streak integer,
        sub_topic_incorrect_streak_key varchar(255) not null,
        primary key (progress_id, sub_topic_incorrect_streak_key)
    ) engine=InnoDB;

    create table sub_topic_success_streak (
        progress_id bigint not null,
        success_streak integer,
        sub_topic_success_streak_key varchar(255) not null,
        primary key (progress_id, sub_topic_success_streak_key)
    ) engine=InnoDB;

    create table system_admin_entity (
        id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table teacher_classroom (
        teacher_id bigint not null,
        classroom_id bigint not null
    ) engine=InnoDB;

    create table teacher_student (
        teacher_id bigint not null,
        student_id bigint not null
    ) engine=InnoDB;

    create table teacher_subject (
        teacher_id bigint not null,
        subject_name varchar(255)
    ) engine=InnoDB;

    create table teacher_test_student (
        test_id bigint not null,
        student_id bigint not null
    ) engine=InnoDB;

    create table teacher_entity (
        id bigint not null,
        school_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table teacher_test_entity (
        id bigint not null,
        teacher_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table teacher_test_result_entity (
        id bigint not null,
        student_id bigint,
        teacher_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table test_entity (
        test_type varchar(31) not null,
        id bigint not null auto_increment,
        difficulty varchar(255),
        question_count integer not null,
        start_time varchar(255),
        subject varchar(255),
        time_limit_minutes integer not null,
        topic varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table test_question_entity (
        id bigint not null,
        test_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table test_result_entity (
        test_result_type varchar(31) not null,
        id bigint not null auto_increment,
        correct_answers_count integer not null,
        finish_time varchar(255),
        incorrect_answers_count integer not null,
        score integer not null,
        test_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table test_result_entity_correct_questions (
        test_result_entity_id bigint not null,
        correct_questions_id bigint not null
    ) engine=InnoDB;

    create table test_result_entity_incorrect_questions (
        test_result_entity_id bigint not null,
        incorrect_questions_id bigint not null
    ) engine=InnoDB;

    create table user_entity (
        id bigint not null auto_increment,
        email varchar(255) not null,
        otp varchar(255),
        otp_timestamp bigint,
        password_hash varchar(255) not null,
        phone_number varchar(255) not null,
        profile_picture varchar(255),
        role enum ('SCHOOLMANAGER','STUDENT','SYSTEM_ADMIN','TEACHER') not null,
        salt varchar(255) not null,
        user_id varchar(255) not null,
        username varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table schedule_entity
       add constraint UKo43365v6vrf3hijh6nf7dvvbf unique (class_room_id);

    create index idx_school_code
       on school_entity (school_code);

    alter table school_manager_entity
       add constraint UKtk0359a462203eywxr93m320o unique (school_id);

    alter table student_progress_entity
       add constraint UK1im7mbfkf7kxugmaoilkotnit unique (student_id);

    alter table student_question_history_entity
       add constraint UKhcugsmn1nfj6r06wwodfjw40b unique (student_id);

    alter table test_result_entity_correct_questions
       add constraint UKdmhh085sff9n1070uddaqk2eg unique (correct_questions_id);

    alter table test_result_entity_incorrect_questions
       add constraint UKb3ym9o1f39dtp9kag00r6lvq1 unique (incorrect_questions_id);

    create index idx_user_userId
       on user_entity (user_id);

    alter table answered_questions
       add constraint FKgfcug6avsv5lo8cpxo44cy0o5
       foreign key (question_id)
       references question_entity (id);

    alter table answered_questions
       add constraint FKj8tpbi049njadu5hy3rxgwuwk
       foreign key (history_id)
       references student_question_history_entity (id);

    alter table class_room_entity
       add constraint FKowm6ay7xyxqtwmx4erih576xx
       foreign key (grade_id)
       references school_grade_entity (id);

    alter table class_room_entity
       add constraint FK1d2h4rrsj33ai65ibcyvcw4b
       foreign key (school_id)
       references school_entity (id);

    alter table lesson_entity
       add constraint FK7l2kbns8avi34m63v01c3x14w
       foreign key (schedule_id)
       references schedule_entity (id);

    alter table lesson_entity
       add constraint FK8usnmq691qt9kyjuw8h4tunei
       foreign key (teacher_id)
       references teacher_entity (id);

    alter table message_entity
       add constraint FKf1eboma4d9p0wlj48qwpd2ban
       foreign key (receiver_id)
       references user_entity (id);

    alter table message_entity
       add constraint FKchngvnhlot2wncosjrnmd1qjp
       foreign key (sender_id)
       references user_entity (id);

    alter table practice_question_entity
       add constraint FKk20hg3c9oso751bhn8rysw51l
       foreign key (student_progress_id)
       references student_progress_entity (id);

    alter table practice_question_entity
       add constraint FKc38pkrjrh5yilu3261n12c9s5
       foreign key (question_history_id)
       references student_question_history_entity (id);

    alter table practice_question_entity
       add constraint FKe4lelv3x24behtwi88dyncbp1
       foreign key (id)
       references question_entity (id);

    alter table practice_test_entity
       add constraint FKl8p3w7tsusfihtrmg7b23x4o2
       foreign key (student_id)
       references student_entity (id);

    alter table practice_test_entity
       add constraint FKh22auaw3tlrwvsywn9gih3vgw
       foreign key (id)
       references test_entity (id);

    alter table practice_test_result_entity
       add constraint FKj59uheba6uvkow00wk57vwfs4
       foreign key (student_id)
       references student_entity (id);

    alter table practice_test_result_entity
       add constraint FKknossgcuirk4ipcy6oq8ni16i
       foreign key (id)
       references test_result_entity (id);

    alter table schedule_entity
       add constraint FK1utegwr2a5thahbavsg7k74tk
       foreign key (class_room_id)
       references class_room_entity (id);

    alter table school_grade_entity
       add constraint FKo3v5by2d17u9hfoq6knslxexi
       foreign key (school_id)
       references school_entity (id);

    alter table school_manager_entity
       add constraint FKhfxwdgc2w6uwdul166elb0a3n
       foreign key (school_id)
       references school_entity (id);

    alter table school_manager_entity
       add constraint FK2b78yurxj6kobo7vr5fopqiit
       foreign key (id)
       references user_entity (id);

    alter table session
       add constraint FKbaw0ponuf3a87k9ntm83fmmbq
       foreign key (user_id)
       references user_entity (id);

    alter table skill_levels_by_sub_topic
       add constraint FK38xg2437wiibdd35ykkbjapim
       foreign key (progress_id)
       references student_progress_entity (id);

    alter table student_entity
       add constraint FKm9xinpb8vad0xpj7se5sdt81d
       foreign key (class_room_id)
       references class_room_entity (id);

    alter table student_entity
       add constraint FKcg70hnfpyhy621r1rg0gsg26k
       foreign key (school_id)
       references school_entity (id);

    alter table student_entity
       add constraint FKamme9p4hfocenc7rrhcha6lbj
       foreign key (id)
       references user_entity (id);

    alter table student_progress_entity
       add constraint FK1140bbc6ykjiya49yij57f25w
       foreign key (student_id)
       references student_entity (id);

    alter table student_question_history_entity
       add constraint FKjhhnt8hrfd113jvuosqoimr99
       foreign key (student_id)
       references student_entity (id);

    alter table sub_topic_incorrect_streak
       add constraint FK9y6lxh3cafb91pxhfybud24bi
       foreign key (progress_id)
       references student_progress_entity (id);

    alter table sub_topic_success_streak
       add constraint FKt5bqttjj60h1isdo80d57xwit
       foreign key (progress_id)
       references student_progress_entity (id);

    alter table system_admin_entity
       add constraint FKl4euxu1aw6432u3eyfxtdpl2d
       foreign key (id)
       references user_entity (id);

    alter table teacher_classroom
       add constraint FKkv69w7ot54gg9vcnqvbdembeu
       foreign key (classroom_id)
       references class_room_entity (id);

    alter table teacher_classroom
       add constraint FKf72r4ht8na8awtd6uv5j26tj1
       foreign key (teacher_id)
       references teacher_entity (id);

    alter table teacher_student
       add constraint FKgp0q3lsf369nsyk1a07we20tj
       foreign key (student_id)
       references student_entity (id);

    alter table teacher_student
       add constraint FKfls3d0gvpbvh6lsn853i6bjr3
       foreign key (teacher_id)
       references teacher_entity (id);

    alter table teacher_subject
       add constraint FKop3gtsvvvn152t19urjy09iy8
       foreign key (teacher_id)
       references teacher_entity (id);

    alter table teacher_test_student
       add constraint FKoswtjw9fnv8s5aa7h6aktce14
       foreign key (student_id)
       references student_entity (id);

    alter table teacher_test_student
       add constraint FKqk9ngb1k56smocchy0uj1k67s
       foreign key (test_id)
       references teacher_test_entity (id);

    alter table teacher_entity
       add constraint FKt95rlvdqj8sr9v80ckbu8i8av
       foreign key (school_id)
       references school_entity (id);

    alter table teacher_entity
       add constraint FKfkoiplpfghobaor1oyfqnsye4
       foreign key (id)
       references user_entity (id);

    alter table teacher_test_entity
       add constraint FKr0pkfvs7yfwcdqomb6nckdwnv
       foreign key (teacher_id)
       references teacher_entity (id);

    alter table teacher_test_entity
       add constraint FKqcf4sr2wgmrc5qrfinimstk3k
       foreign key (id)
       references test_entity (id);

    alter table teacher_test_result_entity
       add constraint FKnqwj6t20b9dxrafcw5t2nnh53
       foreign key (student_id)
       references student_entity (id);

    alter table teacher_test_result_entity
       add constraint FKk9jrtuyesx6pkja07sl362t4m
       foreign key (teacher_id)
       references teacher_entity (id);

    alter table teacher_test_result_entity
       add constraint FKdlmu56olkce56101qw4npu6tc
       foreign key (id)
       references test_result_entity (id);

    alter table test_question_entity
       add constraint FKet1xhwl5pn7it1dn77fgo2w8n
       foreign key (test_id)
       references test_entity (id);

    alter table test_question_entity
       add constraint FKelucnd8fliyjqgch8046hpja2
       foreign key (id)
       references question_entity (id);

    alter table test_result_entity
       add constraint FKoydyh9xnrp04wkwvajvmobxe1
       foreign key (test_id)
       references test_entity (id);

    alter table test_result_entity_correct_questions
       add constraint FKc3lvxccyf6jmclbswvbbjqrhe
       foreign key (correct_questions_id)
       references test_question_entity (id);

    alter table test_result_entity_correct_questions
       add constraint FK9mr4hmgj3qhcscvqgm0gtfd9h
       foreign key (test_result_entity_id)
       references test_result_entity (id);

    alter table test_result_entity_incorrect_questions
       add constraint FKngqm2rvjik87hmu8ph2if26d0
       foreign key (incorrect_questions_id)
       references test_question_entity (id);

    alter table test_result_entity_incorrect_questions
       add constraint FK3df4parkqd7f0yh5rnuhnb5m8
       foreign key (test_result_entity_id)
       references test_result_entity (id);
