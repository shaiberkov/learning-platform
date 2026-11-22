package org.example.learningprojectserver.constants;

import static org.example.learningprojectserver.constants.SharedConstants.SharedControllerConstants.BASE_API;

public final class ControllerConstants {
    public static final class AuditLog {
        private AuditLog() {}

        public static final String AUDIT_LOG_BASE_PATH = BASE_API + "/audit-log";
        public static final String LIST_AUDIT_LOG      = "/list";
        public static final String AUDIT_ACTIONS       = "/actions";
    }
    public static final class Impersonation {
        private Impersonation() {}

        public static final String IMPERSONATION_BASE_PATH = BASE_API + "/impersonation";
        public static final String START_IMPERSONATION     = "/start";
        public static final String STOP_IMPERSONATION      = "/stop";
    }
    public static final class AdminTools {
        private AdminTools() {}

        // Served under SystemAdmin.SYSTEM_ADMIN_BASE_PATH.
        public static final String SYSTEM_HEALTH    = "/health";
        public static final String FEATURE_FLAGS    = "/feature-flags";
        public static final String SET_FEATURE_FLAG = "/feature-flags/set";
    }
    public static final class ActiveUser {
        private ActiveUser() {}

        public static final String CONNECTED_USERS_BASE_PATH = BASE_API + "/Active-User";
        public static final String CONNECT_USER = "/connect-user";
        public static final String DISCONNECT_USER = "/disconnect-user";
    }
    public static final class ChatGpt {
        private ChatGpt() {}

        public static final String CHAT_BASE_PATH = BASE_API + "/Chat";
        public static final String WITH_MEMORY = "/get-response-from-chatGpt-with-memory";
        public static final String SINGLE_RESPONSE = "/get-single-response-from-chatGpt";
        public static final String CLEAR_CONVERSATION = "/clear-conversation";
    }
    public static final class Lesson {
        private Lesson() {}

        public static final String LESSON_BASE_PATH = BASE_API + "/lesson";
        public static final String TEACHER_LESSONS = "/teacher/lessons";
    }
    public static final class Message {
        private Message() {}

        public static final String MESSAGE_BASE_PATH = BASE_API + "/message";
        public static final String RECIPIENT_TYPES = "/recipient-types";
        public static final String SEND_MESSAGE = "/send-message";
        public static final String GET_ALL_RECEIVED_MESSAGES = "/get-all-recived-messages";
    }
    public static final class Notification {
        private Notification() {}

        public static final String NOTIFICATION_BASE_PATH = BASE_API + "/notifications";
        public static final String CONNECT = "/connect";
    }
    public static final class Question {
        private Question() {}
        public static final String QUESTION_BASE_PATH = BASE_API + "/question";
        public static final String GENERATE_QUESTION = "/generate-question";
        public static final String SUBMIT_ANSWER = "/submit-answer";
    }
    public static final class Schedule {
        private Schedule() {}
        public static final String SCHEDULE_BASE_PATH = BASE_API + "/schedule";
    }
    public static final class SchoolManager {
        private SchoolManager() {}

        public static final String SCHOOL_MANAGER_BASE_PATH             = BASE_API + "/school-manager";

        public static final String GET_TEACHER_DTO                      = "/get-teacher-dto";
        public static final String GET_ALL_CLASSES_BY_SCHOOL_CODE       = "/get-all-classes-name-by-school-code";
        public static final String GET_SCHOOL_CODE                      = "/get-school-code";
        public static final String GET_SCHEDULE_FOR_CLASSROOM           = "/get-schedule-for-classroom";

        public static final String ASSIGN_USER_AS_SCHOOL_TEACHER        = "/assign-user-as-school-teacher";
        public static final String REMOVE_TEACHER_FROM_SCHOOL           = "/remove-teacher-from-school";

        public static final String ADD_SCHOOL_GRADES                    = "/add-school-grades";
        public static final String REMOVE_SCHOOL_GRADES                 = "/remove-school-grades";

        public static final String ADD_CLASSES_TO_GRADE                 = "/add-classes-to-grade";
        public static final String ADD_ADDITIONAL_CLASS_TO_GRADE        = "/add-additional-class-to-grade";

        public static final String ASSIGN_TEACHER_TO_CLASSES            = "/assign-teacher-to-classes";
        public static final String REMOVE_TEACHER_FROM_CLASS            = "/remove-teacher-from-class";

        public static final String GET_CLASS_NAMES_BY_SCHOOL_AND_GRADE  = "/get-class-names-by-school-and-grade";
        public static final String GET_GRADES                           = "/grades";

        public static final String ADD_LESSON_TO_TEACHER                = "/add-lesson-to-teacher";
        public static final String ADD_TEACHING_SUBJECT_TO_TEACHER      = "/add-teaching-subject-to-teacher";
        public static final String REMOVE_TEACHING_SUBJECT_FROM_TEACHER = "/remove-teaching-subject-from-teacher";

        public static final String ASSIGN_STUDENT_TO_CLASS              = "/assign-student-to-class";
        public static final String CHANGE_STUDENT_CLASS                 = "/change-student-class";

        public static final String GET_ALL_TEACHERS                     = "/get-all-teachers";
        public static final String GET_ALL_STUDENTS                     = "/get-all-students";

        public static final String GET_TEACHER_TEACHING_SUBJECTS        = "/get-teacher-teaching-subjects";
        public static final String GET_SCHOOL                           = "/get-school";
    }
    public static final class Session {
        private Session() {}
        public static final String SESSION_BASE_PATH = BASE_API + "/validate-token";
        public static final String VALIDATE_TOKEN = "/validate-token";
    }
    public static final class UserStatistic {
        private UserStatistic() {}

        public static final String USER_STATISTIC_BASE_PATH = BASE_API + "/User-Statistic";
        public static final String GET_USER_STATISTIC = "/get-user-statistic";
    }
    public static final class Student {
        private Student() {}

        public static final String STUDENT_BASE_PATH          = BASE_API + "/student";
        public static final String GET_STUDENT_SCHEDULE       = "/get-student-schedule";
        public static final String GENERATE_QUESTION          = "/generate-question";
        public static final String SUBMIT_ANSWER              = "/submit-answer";
        public static final String GENERATE_PRACTICE_TEST     = "/generate-practice-test";
        public static final String CHECK_PRACTICE_TEST        = "/check-practice-test";
        public static final String START_TEST                 = "/start-test";
        public static final String GET_STUDENT_TESTS_STATUS   = "/get-student-tests-status";
    }
    public static final class SystemAdmin {
        private SystemAdmin() {}

        public static final String SYSTEM_ADMIN_BASE_PATH             = BASE_API + "/system-admin";
        public static final String ASSIGN_SCHOOL_MANAGER              = "/assign-school-manager";
        public static final String ADD_NEW_SCHOOL_TO_SYSTEM           = "/add-new-school-to-system";
        public static final String ASSIGN_SCHOOL_MANAGER_TO_SCHOOL    = "/assign-school-manager-to-school";
        public static final String REMOVE_SCHOOL_MANAGER_FROM_SCHOOL  = "/remove-school-manager-from-school";
        public static final String GET_ALL_SCHOOLS                    = "/get-all-schools";
    }
    public static final class Teacher {
        private Teacher() {}

        public static final String TEACHER_BASE_PATH             = BASE_API + "/teacher";
        public static final String SCHEDULE_FOR_TEACHER          = "/schedule-for-teacher";
        public static final String GENERATE_TEST_FOR_STUDENTS    = "/generate-test-for-students";
        public static final String CHECK_TEACHER_TEST            = "/check-teacher-test";
    }
    public static final class Test {
        private Test() {}

        public static final String TEST_BASE_PATH = BASE_API + "/test";
        public static final String GET_TEST       = "/get-test";
    }
    public static final class Exam {
        private Exam() {}

        public static final String EXAM_BASE_PATH  = BASE_API + "/exam";
        public static final String START_ATTEMPT   = "/start-attempt";
        public static final String SAVE_ANSWERS    = "/save-answers";
        public static final String SUBMIT_ATTEMPT  = "/submit-attempt";
        public static final String INTEGRITY_EVENT = "/integrity-event";
        public static final String CREATE_EXAM     = "/create-exam";
        public static final String CLONE_EXAM      = "/clone-exam";
        public static final String TEACHER_EXAMS   = "/teacher-exams";
        public static final String EXAM_ATTEMPTS   = "/attempts";
        public static final String ITEM_ANALYSIS   = "/item-analysis";
    }
    public static final class UpcomingEvents {
        private UpcomingEvents() {}

        public static final String UPCOMING_EVENTS_BASE_PATH = BASE_API + "/upcoming-events";
        public static final String GET_UPCOMING_EVENTS       = "/get-upcoming-events";
    }
    public static final class User {
        private User() {}

        public static final String USER_BASE_PATH            = BASE_API + "/user";

        public static final String ADD_USER                  = "/add-user";
        public static final String LOGIN                     = "/login";
        public static final String SEND_OTP                  = "/send-otp";
        public static final String VERIFY_OTP                = "/verify-otp";
        public static final String FORGOT_PASSWORD           = "/forgot-password";
        public static final String RESET_PASSWORD            = "/reset-password";
        public static final String GET_USER_PHONE            = "/user/phone";
        public static final String REFRESH_TOKEN             = "/refresh-token";
        public static final String LOGOUT                    = "/logout";
    }
    /** Adaptive learning: mistakes notebook, mastery and skill map, hints, exam feedback. */
    public static final class Adaptive {
        private Adaptive() {}

        public static final String STUDENT_ADAPTIVE_BASE_PATH = BASE_API + "/student";
        public static final String REVIEW_DUE                 = "/review/due";
        public static final String REVIEW_SUMMARY             = "/review/summary";
        public static final String REVIEW_ANSWER              = "/review/answer";
        public static final String SKILL_MAP                  = "/skill-map";
        public static final String PRACTICE_HINT              = "/practice-hint";
        public static final String EXAM_FEEDBACK              = "/exam-feedback";
        public static final String EXAM_QUESTION_TIMES        = "/exam-question-times";

        public static final String TEACHER_ADAPTIVE_BASE_PATH = BASE_API + "/teacher";
        public static final String TEACHER_CLASSES            = "/classes";
        public static final String CLASS_SKILL_MAP            = "/class-skill-map";
        public static final String HINT_CATALOG               = "/hint-catalog";
        public static final String HINT_TEMPLATE              = "/hint-template";
    }
    public static final class Engagement {
        private Engagement() {}

        public static final String ENGAGEMENT_BASE_PATH      = BASE_API + "/engagement";
        public static final String GET_SUMMARY               = "/get-summary";
        public static final String CREATE_CLASS_CHALLENGE    = "/create-class-challenge";
        public static final String TEACHER_CHALLENGES        = "/teacher-challenges";
    }
    public static final class StudyPlan {
        private StudyPlan() {}

        public static final String STUDY_PLAN_BASE_PATH      = BASE_API + "/study-plan";
        public static final String GET_PLAN                  = "/get-plan";
        public static final String COMPLETE_TASK             = "/complete-task";
    }
    public static final class LessonContent {
        private LessonContent() {}

        public static final String LESSON_CONTENT_BASE_PATH  = BASE_API + "/lesson-content";
        public static final String CREATE                    = "/create";
        public static final String UPDATE                    = "/update";
        public static final String DELETE                    = "/delete";
        public static final String TEACHER_LESSONS           = "/teacher-lessons";
        public static final String STUDENT_LESSONS           = "/student-lessons";
        public static final String GET                       = "/get";
        public static final String UPDATE_PROGRESS           = "/update-progress";
    }
    public static final class Homework {
        private Homework() {}

        public static final String HOMEWORK_BASE_PATH        = BASE_API + "/homework";
        public static final String CREATE                    = "/create";
        public static final String TEACHER_CLASSES           = "/teacher-classes";
        public static final String TEACHER_BOARD             = "/teacher-board";
        public static final String STUDENT_HOMEWORK          = "/student-homework";
        public static final String SUBMIT                    = "/submit";
    }
    public static final class Accommodation {
        private Accommodation() {}

        public static final String ACCOMMODATION_BASE_PATH   = BASE_API + "/accommodation";
        public static final String GET                       = "/get";
        public static final String TEACHER_STUDENTS          = "/teacher-students";
        public static final String SET_EXTRA_TIME            = "/set-extra-time";
        public static final String SET_PREFERENCES           = "/set-preferences";
    }


}
