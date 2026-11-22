package org.example.learningprojectserver.constants;

import static org.example.learningprojectserver.constants.SharedConstants.SharedControllerConstants.BASE_API;

public final class ControllerConstants {
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
    }
    public static final class QuestionBank {
        private QuestionBank() {}

        public static final String QUESTION_BANK_BASE_PATH  = BASE_API + "/question-bank";
        public static final String ADD_BANK_QUESTION        = "/add";
        public static final String UPDATE_BANK_QUESTION     = "/update";
        public static final String DELETE_BANK_QUESTION     = "/delete";
        public static final String SEARCH_BANK_QUESTIONS    = "/search";
        public static final String SET_BANK_QUESTION_SHARED = "/set-shared";
        public static final String IMPORT_BANK_QUESTIONS    = "/import";
        public static final String GET_BANK_TAGS            = "/tags";
    }
    public static final class Gradebook {
        private Gradebook() {}

        public static final String GRADEBOOK_BASE_PATH         = BASE_API + "/gradebook";
        public static final String GET_GRADEBOOK_CLASSES       = "/classes";
        public static final String GET_GRADEBOOK               = "/table";
        public static final String ADD_GRADEBOOK_ASSIGNMENT    = "/add-assignment";
        public static final String DELETE_GRADEBOOK_ASSIGNMENT = "/delete-assignment";
        public static final String SET_GRADEBOOK_GRADE         = "/set-grade";
        public static final String SET_GRADEBOOK_WEIGHTS       = "/set-weights";
        public static final String EXPORT_GRADEBOOK            = "/export";
    }
    public static final class LiveQuiz {
        private LiveQuiz() {}

        public static final String LIVE_QUIZ_BASE_PATH    = BASE_API + "/live-quiz";
        public static final String START_LIVE_QUIZ        = "/start";
        public static final String PUSH_LIVE_QUESTION     = "/push-question";
        public static final String CLOSE_LIVE_QUESTION    = "/close-question";
        public static final String END_LIVE_QUIZ          = "/end";
        public static final String GET_LIVE_QUIZ_RESULTS  = "/results";
        public static final String GET_CURRENT_LIVE_QUIZ  = "/current";
        public static final String ANSWER_LIVE_QUESTION   = "/answer";
    }
    public static final class Attendance {
        private Attendance() {}

        public static final String ATTENDANCE_BASE_PATH   = BASE_API + "/attendance";
        public static final String GET_ATTENDANCE_LESSONS = "/lessons-for-day";
        public static final String GET_ATTENDANCE_SHEET   = "/sheet";
        public static final String MARK_ATTENDANCE        = "/mark";
        public static final String GET_ATTENDANCE_REPORT  = "/report";
    }

}
