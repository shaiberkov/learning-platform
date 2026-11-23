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
        public static final String RECEIVED_MESSAGES_PAGE = "/received-messages-page";
        public static final String SENT_MESSAGES = "/sent-messages";
        public static final String READ_RECEIPTS = "/read-receipts";
        public static final String MARK_READ = "/mark-read";
        public static final String REPLY = "/reply";
        public static final String THREAD = "/thread";
        public static final String ATTACHMENT = "/attachment";
    }
    public static final class Notification {
        private Notification() {}

        public static final String NOTIFICATION_BASE_PATH = BASE_API + "/notifications";
        public static final String CONNECT = "/connect";
        public static final String LIST = "/list";
        public static final String UNREAD_COUNT = "/unread-count";
        public static final String MARK_READ = "/mark-read";
        public static final String MARK_ALL_READ = "/mark-all-read";
        public static final String PREFERENCES = "/preferences";
        public static final String PUSH_PUBLIC_KEY = "/push/public-key";
        public static final String PUSH_SUBSCRIBE = "/push/subscribe";
        public static final String PUSH_UNSUBSCRIBE = "/push/unsubscribe";
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

    public static final class Ai {
        private Ai() {}

        public static final String AI_BASE_PATH          = BASE_API + "/ai";
        public static final String STATUS                = "/status";
        public static final String TUTOR_STREAM          = "/tutor/stream";
        public static final String TUTOR_HISTORY         = "/tutor/history";
        public static final String TUTOR_TRANSCRIBE      = "/tutor/transcribe";
        public static final String OPEN_QUESTIONS        = "/open-questions";
        public static final String OPEN_QUESTION_ANSWERS = "/open-questions/answers";
    }
    public static final class AiStaff {
        private AiStaff() {}

        public static final String AI_STAFF_BASE_PATH     = BASE_API + "/ai/staff";
        public static final String QUESTIONS_GENERATE     = "/questions/generate";
        public static final String QUESTION_DRAFTS        = "/questions/drafts";
        public static final String QUESTION_DRAFT         = "/questions/drafts/{draftId}";
        public static final String QUESTION_DRAFT_APPROVE = "/questions/drafts/{draftId}/approve";
        public static final String QUESTION_DRAFT_REJECT  = "/questions/drafts/{draftId}/reject";
        public static final String QUESTION_BANK          = "/questions/bank";
        public static final String RUBRICS                = "/grading/rubrics";
        public static final String RUBRIC_ANSWERS         = "/grading/rubrics/{rubricId}/answers";
        public static final String ANSWER_SUGGEST         = "/grading/answers/{answerId}/suggest";
        public static final String ANSWER_APPROVE         = "/grading/answers/{answerId}/approve";
        public static final String INSIGHTS_LATEST        = "/insights/latest";
        public static final String INSIGHTS_GENERATE      = "/insights/generate";
        public static final String USAGE                  = "/usage";
        public static final String SAFETY_FLAGGED         = "/safety/flagged";
    }

    /** School administration tools of the manager: bulk import, dashboard, timetable builder, school years, parent links. */
    public static final class SchoolAdministration {
        private SchoolAdministration() {}

        public static final String SCHOOL_ADMINISTRATION_BASE_PATH = BASE_API + "/school-manager";

        public static final String BULK_IMPORT_PREVIEW      = "/bulk-import/preview";
        public static final String BULK_IMPORT_COMMIT       = "/bulk-import/commit";

        public static final String SCHOOL_ANALYTICS         = "/analytics";

        public static final String TIMETABLE_TEACHERS       = "/timetable/teachers";
        public static final String TIMETABLE_LESSONS        = "/timetable/lessons";
        public static final String TIMETABLE_CHECK          = "/timetable/check";
        public static final String TIMETABLE_SAVE           = "/timetable/save";
        public static final String TIMETABLE_DELETE         = "/timetable/delete";

        public static final String SCHOOL_YEARS             = "/school-years";
        public static final String CREATE_SCHOOL_YEAR       = "/school-years/create";
        public static final String ADD_SEMESTER             = "/school-years/add-semester";
        public static final String REMOVE_SEMESTER          = "/school-years/remove-semester";
        public static final String PROMOTION_PLAN           = "/school-years/promotion-plan";
        public static final String CLOSE_SCHOOL_YEAR        = "/school-years/close";
        public static final String SCHOOL_YEAR_ARCHIVE      = "/school-years/archive";

        public static final String LINK_PARENT              = "/parents/link";
        public static final String UNLINK_PARENT            = "/parents/unlink";
    }
    public static final class Parent {
        private Parent() {}

        public static final String PARENT_BASE_PATH = BASE_API + "/parent";
        public static final String GET_CHILDREN     = "/children";
        public static final String CHILD_OVERVIEW   = "/child-overview";
    }
    public static final class Calendar {
        private Calendar() {}

        public static final String CALENDAR_BASE_PATH = BASE_API + "/calendar";
        public static final String TOKEN = "/token";
        public static final String REGENERATE_TOKEN = "/regenerate-token";
        public static final String FEED_PREFIX = "/feed/";
        public static final String FEED = FEED_PREFIX + "{token}.ics";
    }
    public static final class Search {
        private Search() {}

        public static final String SEARCH_BASE_PATH = BASE_API + "/search";
        public static final String GLOBAL = "/global";
        public static final String STUDENTS_PAGE = "/students-page";
    }
    // [s01] Study rooms and focus mode, under /student/** (STUDENT_ALL in SecurityConfig).
    public static final class StudyRooms {
        private StudyRooms() {}

        public static final String STUDY_ROOMS_BASE_PATH = BASE_API + "/student/study-rooms";
        public static final String LIST_ROOMS            = "/list";
        public static final String CREATE_ROOM           = "/create";
        public static final String ROOM_HEARTBEAT        = "/heartbeat";
        public static final String LEAVE_ROOM            = "/leave";
        public static final String ROOM_CHAT             = "/chat";
    }
    // [s01] end
    // [s01] Focus mode and study-habit insights, under /student/**.
    public static final class Focus {
        private Focus() {}

        public static final String FOCUS_BASE_PATH = BASE_API + "/student/focus";
        public static final String RECORD_SESSION  = "/sessions/record";
        public static final String INSIGHTS        = "/insights";
    }
    // [s01] end
    // [s02] Class forum (questions with teacher-approved answers) and student-authored practice questions.
    public static final class ClassCommunity {
        private ClassCommunity() {}

        public static final String CLASS_COMMUNITY_BASE_PATH = BASE_API + "/class-community";
        public static final String FORUM_CLASSES             = "/forum/classes";
        public static final String FORUM_THREADS             = "/forum/threads";
        public static final String FORUM_SIMILAR             = "/forum/similar";
        public static final String FORUM_THREAD              = "/forum/thread";
        public static final String FORUM_CREATE_THREAD       = "/forum/threads/create";
        public static final String FORUM_REPLY               = "/forum/reply";
        public static final String FORUM_APPROVE_POST        = "/forum/approve";
        public static final String FORUM_DELETE_POST         = "/forum/post";
        public static final String FORUM_DELETE_THREAD       = "/forum/thread/delete";
        public static final String PEER_SUBMIT               = "/peer-questions/submit";
        public static final String PEER_MINE                 = "/peer-questions/mine";
        public static final String PEER_PRACTICE             = "/peer-questions/practice";
        public static final String PEER_ANSWER               = "/peer-questions/practice/answer";
        public static final String PEER_REVIEW_QUEUE         = "/peer-questions/review";
        public static final String PEER_APPROVE              = "/peer-questions/approve";
        public static final String PEER_REJECT               = "/peer-questions/reject";
    // [s03] 1-on-1 duels and the daily challenge (student side, under the STUDENT route group).
    public static final class Duel {
        private Duel() {}

        public static final String DUEL_BASE_PATH = BASE_API + "/student/duels";
        public static final String DUEL_TOPICS     = "/topics";
        public static final String DUEL_OPPONENTS  = "/opponents";
        public static final String DUEL_LIST       = "/mine";
        public static final String DUEL_INVITE     = "/invite";
        public static final String DUEL_STATE      = "/{duelId}";
        public static final String DUEL_ACCEPT     = "/{duelId}/accept";
        public static final String DUEL_DECLINE    = "/{duelId}/decline";
        public static final String DUEL_ANSWER     = "/{duelId}/answer";
        public static final String DUEL_FORFEIT    = "/{duelId}/forfeit";
    }
    public static final class DailyChallenge {
        private DailyChallenge() {}

        public static final String DAILY_CHALLENGE_BASE_PATH = BASE_API + "/student/daily-challenge";
        public static final String DAILY_CHALLENGE_TODAY     = "/today";
        public static final String DAILY_CHALLENGE_START     = "/start";
        public static final String DAILY_CHALLENGE_ANSWER    = "/answer";
    // [s04] Flashcards (AI from a photo or summary, shared with the class) and the personal study notebook.
    // Under /student/** (STUDENT, SYSTEM_ADMIN); the student always comes from the access token.
    public static final class StudyTools {
        private StudyTools() {}

        public static final String STUDY_TOOLS_BASE_PATH  = BASE_API + "/student";
        public static final String FLASHCARDS             = "/flashcards";
        public static final String FLASHCARDS_DUE         = "/flashcards/due";
        public static final String FLASHCARDS_SUMMARY     = "/flashcards/summary";
        public static final String FLASHCARDS_CLASS       = "/flashcards/class";
        public static final String FLASHCARDS_GENERATE    = "/flashcards/generate";
        public static final String FLASHCARD_BY_ID        = "/flashcards/{cardId}";
        public static final String FLASHCARD_SHARE        = "/flashcards/{cardId}/share";
        public static final String FLASHCARD_ADD_TO_DECK  = "/flashcards/{cardId}/add-to-deck";
        public static final String FLASHCARD_REVIEW       = "/flashcards/{cardId}/review";
        public static final String NOTES                  = "/notes";
        public static final String NOTES_TOPICS           = "/notes/topics";
        public static final String NOTE_BY_ID             = "/notes/{noteId}";
    }
}
