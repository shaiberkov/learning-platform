package org.example.learningprojectserver.constants;

import java.util.List;

import static org.example.learningprojectserver.constants.ControllerConstants.ActiveUser.CONNECTED_USERS_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.AuditLog.AUDIT_LOG_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Impersonation.IMPERSONATION_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Impersonation.START_IMPERSONATION;
import static org.example.learningprojectserver.constants.ControllerConstants.Teacher.CHECK_TEACHER_TEST;
import static org.example.learningprojectserver.constants.ControllerConstants.Ai.AI_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.AiStaff.AI_STAFF_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Calendar.CALENDAR_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Calendar.FEED_PREFIX;
import static org.example.learningprojectserver.constants.ControllerConstants.Search.SEARCH_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Exam.EXAM_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Lesson.LESSON_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Message.MESSAGE_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Notification.NOTIFICATION_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Notification.PUSH_PUBLIC_KEY;
import static org.example.learningprojectserver.constants.ControllerConstants.Parent.PARENT_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Question.QUESTION_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Schedule.SCHEDULE_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.SchoolManager.SCHOOL_MANAGER_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Session.SESSION_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Student.STUDENT_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.SystemAdmin.SYSTEM_ADMIN_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Teacher.TEACHER_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Test.TEST_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.UpcomingEvents.UPCOMING_EVENTS_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.User.USER_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.QuestionBank.QUESTION_BANK_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Gradebook.GRADEBOOK_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.LiveQuiz.LIVE_QUIZ_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Attendance.ATTENDANCE_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.UserStatistic.USER_STATISTIC_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Engagement.ENGAGEMENT_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.StudyPlan.STUDY_PLAN_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.LessonContent.LESSON_CONTENT_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Homework.HOMEWORK_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Accommodation.ACCOMMODATION_BASE_PATH;
// [s07] real-world cards and simulations
import static org.example.learningprojectserver.constants.ControllerConstants.RealWorld.REAL_WORLD_BASE_PATH;

public final class SecurityConstants {
    public static final class JwtAuthenticationFilterConstants {
        private JwtAuthenticationFilterConstants() {}

        public static final String ROLE_PREFIX  = "ROLE_";
        public static final String PRINCIPAL_USER_ID_KEY = "userId";
        public static final String PRINCIPAL_USERNAME_KEY = "username";
        public static final String PRINCIPAL_IMPERSONATOR_ID_KEY = "impersonatorId";
        public static final String PRINCIPAL_IMPERSONATOR_USERNAME_KEY = "impersonatorUsername";
    }
    public static final class LoggingFilterConstants {
        private LoggingFilterConstants() {}

        public static final String UNKNOWN_USER_ID = "UNKNOWN";
        public static final String UNKNOWN_USERNAME = "UNKNOWN";
        public static final String UNKNOWN_ROLE = "UNKNOWN";
        public static final String JWT_DECODE_ERROR_PREFIX = "שגיאה בפענוח JWT: ";
        public static final String LOG_FORMAT =
                "[%s] %s %s -> %d | %s: %s | %s: %s | role: %s | ip: %s";
    }
    public static final class MetricsFilterConstants {
        private MetricsFilterConstants() {}
        public static final String PRINT_FORMAT =
                "Path: %s | Time: %dms | Total Requests: %d | Avg Response Time: %dms%n";
    }
    public final class SecurityMatcherConstants {

        private SecurityMatcherConstants() { }
        public static final String CORS_MAPPING_PATH = "/**";

        public static final String V3_API_DOCS               = "/v3/api-docs"         + CORS_MAPPING_PATH;
        public static final String SWAGGER_UI_RESOURCES      = "/swagger-ui"          + CORS_MAPPING_PATH;
        public static final String SWAGGER_UI_HTML           = "/swagger-ui.html";

        public static final String USER_ALL                  = USER_BASE_PATH         + CORS_MAPPING_PATH;
        public static final String VALIDATE_TOKEN_ALL        = SESSION_BASE_PATH      + CORS_MAPPING_PATH;

        public static final String SYSTEM_ADMIN_ALL          = SYSTEM_ADMIN_BASE_PATH + CORS_MAPPING_PATH;
        public static final String SCHOOL_MANAGER_ALL        = SCHOOL_MANAGER_BASE_PATH + CORS_MAPPING_PATH;
        public static final String TEACHER_ALL               = TEACHER_BASE_PATH      + CORS_MAPPING_PATH;
        public static final String STUDENT_ALL               = STUDENT_BASE_PATH      + CORS_MAPPING_PATH;

        public static final String ACTIVE_USER_ALL           = CONNECTED_USERS_BASE_PATH + CORS_MAPPING_PATH;
        public static final String QUESTION_ALL              = QUESTION_BASE_PATH     + CORS_MAPPING_PATH;
        public static final String USER_STATISTIC_ALL        = USER_STATISTIC_BASE_PATH + CORS_MAPPING_PATH;
        public static final String TEST_ALL                  = TEST_BASE_PATH         + CORS_MAPPING_PATH;
        public static final String MESSAGE_ALL               = MESSAGE_BASE_PATH      + CORS_MAPPING_PATH;
        public static final String NOTIFICATION_ALL          = NOTIFICATION_BASE_PATH + CORS_MAPPING_PATH;
        // The VAPID public key is public; the SSE stream itself needs the access cookie.
        public static final String NOTIFICATION_PUSH_KEY     = NOTIFICATION_BASE_PATH + PUSH_PUBLIC_KEY;
        public static final String LESSON_ALL                = LESSON_BASE_PATH       + CORS_MAPPING_PATH;
        public static final String SCHEDULE_ALL              = SCHEDULE_BASE_PATH     + CORS_MAPPING_PATH;
        public static final String UPCOMING_EVENTS_ALL       = UPCOMING_EVENTS_BASE_PATH + CORS_MAPPING_PATH;
        public static final String AUDIT_LOG_ALL             = AUDIT_LOG_BASE_PATH    + CORS_MAPPING_PATH;
        public static final String IMPERSONATION_START       = IMPERSONATION_BASE_PATH + START_IMPERSONATION;
        public static final String IMPERSONATION_ALL         = IMPERSONATION_BASE_PATH + CORS_MAPPING_PATH;
        // Students submit their teacher tests through the teacher controller.
        public static final String TEACHER_TEST_SUBMIT       = TEACHER_BASE_PATH      + CHECK_TEACHER_TEST;
        public static final String EXAM_ALL                  = EXAM_BASE_PATH         + CORS_MAPPING_PATH;
        public static final String ENGAGEMENT_ALL            = ENGAGEMENT_BASE_PATH   + CORS_MAPPING_PATH;
        public static final String STUDY_PLAN_ALL            = STUDY_PLAN_BASE_PATH   + CORS_MAPPING_PATH;
        public static final String LESSON_CONTENT_ALL        = LESSON_CONTENT_BASE_PATH + CORS_MAPPING_PATH;
        public static final String HOMEWORK_ALL              = HOMEWORK_BASE_PATH     + CORS_MAPPING_PATH;
        public static final String ACCOMMODATION_ALL         = ACCOMMODATION_BASE_PATH + CORS_MAPPING_PATH;
        public static final String AI_STAFF_ALL              = AI_STAFF_BASE_PATH     + CORS_MAPPING_PATH;
        public static final String AI_ALL                    = AI_BASE_PATH           + CORS_MAPPING_PATH;
        public static final String QUESTION_BANK_ALL         = QUESTION_BANK_BASE_PATH + CORS_MAPPING_PATH;
        public static final String GRADEBOOK_ALL             = GRADEBOOK_BASE_PATH    + CORS_MAPPING_PATH;
        public static final String LIVE_QUIZ_ALL             = LIVE_QUIZ_BASE_PATH    + CORS_MAPPING_PATH;
        public static final String ATTENDANCE_ALL            = ATTENDANCE_BASE_PATH   + CORS_MAPPING_PATH;
        public static final String PARENT_ALL                = PARENT_BASE_PATH       + CORS_MAPPING_PATH;
        public static final String CALENDAR_ALL              = CALENDAR_BASE_PATH     + CORS_MAPPING_PATH;
        // Calendar apps open the .ics feed with only the secret token in the URL.
        public static final String CALENDAR_FEED             = CALENDAR_BASE_PATH     + FEED_PREFIX + "**";
        public static final String SEARCH_ALL                = SEARCH_BASE_PATH       + CORS_MAPPING_PATH;
        // [s07] real-world cards and simulations
        public static final String REAL_WORLD_ALL            = REAL_WORLD_BASE_PATH   + CORS_MAPPING_PATH;

        public static final String SYSTEM_ADMIN   = "SYSTEM_ADMIN";
        public static final String TEACHER        = "TEACHER";
        public static final String SCHOOL_MANAGER = "SCHOOLMANAGER";
        public static final String STUDENT        = "STUDENT";
        public static final String PARENT         = "PARENT";
        public static final List<String> ALLOWED_ORIGINS =
                List.of("http://localhost:5173","https://schoolmanagmentsystem-z0m5.onrender.com/");



        public static final List<String> ALLOWED_METHODS =
                List.of("GET", "POST", "PUT", "DELETE");

        public static final List<String> ALLOWED_HEADERS =
                List.of("*");
    }

    }


