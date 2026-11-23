
import { API_BASE_URL, BASE_API } from "./base.constants.js";

export const PagesConstants = {
    ActiveUser: {
        CONNECTED_USERS_BASE_PATH: `${API_BASE_URL}${BASE_API}/Active-User`,
        CONNECT_USER: '/connect-user',
        DISCONNECT_USER: '/disconnect-user',
    },


    Lesson: {
        LESSON_BASE_PATH: `${API_BASE_URL}${BASE_API}/lesson`,
        TEACHER_LESSONS: '/teacher/lessons',
    },

    Message: {
        MESSAGE_BASE_PATH: `${API_BASE_URL}${BASE_API}/message`,
        RECIPIENT_TYPES: '/recipient-types',
        SEND_MESSAGE: '/send-message',
        GET_ALL_RECEIVED_MESSAGES: '/get-all-recived-messages',
        RECEIVED_MESSAGES_PAGE: '/received-messages-page',
        SENT_MESSAGES: '/sent-messages',
        READ_RECEIPTS: '/read-receipts',
        MESSAGE_MARK_READ: '/mark-read',
        REPLY: '/reply',
        THREAD: '/thread',
        ATTACHMENT: '/attachment',
    },

    Notification: {
        NOTIFICATION_BASE_PATH: `${API_BASE_URL}${BASE_API}/notifications`,
        CONNECT: '/connect',
        NOTIFICATION_LIST: '/list',
        UNREAD_COUNT: '/unread-count',
        NOTIFICATION_MARK_READ: '/mark-read',
        MARK_ALL_READ: '/mark-all-read',
        PREFERENCES: '/preferences',
        PUSH_PUBLIC_KEY: '/push/public-key',
        PUSH_SUBSCRIBE: '/push/subscribe',
        PUSH_UNSUBSCRIBE: '/push/unsubscribe',
    },

    Calendar: {
        CALENDAR_BASE_PATH: `${API_BASE_URL}${BASE_API}/calendar`,
        CALENDAR_TOKEN: '/token',
        REGENERATE_CALENDAR_TOKEN: '/regenerate-token',
    },

    Search: {
        SEARCH_BASE_PATH: `${API_BASE_URL}${BASE_API}/search`,
        GLOBAL_SEARCH: '/global',
        STUDENTS_PAGE: '/students-page',
    },

    Question: {
        QUESTION_BASE_PATH: `${API_BASE_URL}${BASE_API}/question`,
        GENERATE_QUESTION: '/generate-question',
        SUBMIT_ANSWER: '/submit-answer',
    },

    Schedule: {
        SCHEDULE_BASE_PATH: `${API_BASE_URL}${BASE_API}/schedule`,
    },

    SchoolManager: {
        SCHOOL_MANAGER_BASE_PATH: `${API_BASE_URL}${BASE_API}/school-manager`,
        GET_TEACHER_DTO: '/get-teacher-dto',
        GET_ALL_CLASSES_BY_SCHOOL_CODE: '/get-all-classes-name-by-school-code',
        GET_SCHOOL_CODE: '/get-school-code',
        GET_SCHEDULE_FOR_CLASSROOM: '/get-schedule-for-classroom',
        ASSIGN_USER_AS_SCHOOL_TEACHER: '/assign-user-as-school-teacher',
        REMOVE_TEACHER_FROM_SCHOOL: '/remove-teacher-from-school',
        ADD_SCHOOL_GRADES: '/add-school-grades',
        REMOVE_SCHOOL_GRADES: '/remove-school-grades',
        ADD_CLASSES_TO_GRADE: '/add-classes-to-grade',
        ADD_ADDITIONAL_CLASS_TO_GRADE: '/add-additional-class-to-grade',
        ASSIGN_TEACHER_TO_CLASSES: '/assign-teacher-to-classes',
        REMOVE_TEACHER_FROM_CLASS: '/remove-teacher-from-class',
        GET_CLASS_NAMES_BY_SCHOOL_AND_GRADE: '/get-class-names-by-school-and-grade',
        GET_GRADES: '/grades',
        ADD_LESSON_TO_TEACHER: '/add-lesson-to-teacher',
        ADD_TEACHING_SUBJECT_TO_TEACHER: '/add-teaching-subject-to-teacher',
        REMOVE_TEACHING_SUBJECT_FROM_TEACHER: '/remove-teaching-subject-from-teacher',
        ASSIGN_STUDENT_TO_CLASS: '/assign-student-to-class',
        CHANGE_STUDENT_CLASS: '/change-student-class',
        GET_ALL_TEACHERS: '/get-all-teachers',
        GET_ALL_STUDENTS: '/get-all-students',
        GET_TEACHER_TEACHING_SUBJECTS: '/get-teacher-teaching-subjects',
        GET_SCHOOL: '/get-school',
    },

    Session: {
        SESSION_BASE_PATH: `${API_BASE_URL}${BASE_API}/validate-token`,
        VALIDATE_TOKEN: '/validate-token',
        AUTH_HEADER: 'Authorization',
    },

    UserStatistic: {
        USER_STATISTIC_BASE_PATH: `${API_BASE_URL}${BASE_API}/User-Statistic`,
        GET_USER_STATISTIC: '/get-user-statistic',
    },

    Student: {
        STUDENT_BASE_PATH: `${API_BASE_URL}${BASE_API}/student`,
        GET_STUDENT_SCHEDULE: '/get-student-schedule',
        GENERATE_QUESTION: '/generate-question',
        SUBMIT_ANSWER: '/submit-answer',
        GENERATE_PRACTICE_TEST: '/generate-practice-test',
        CHECK_PRACTICE_TEST: '/check-practice-test',
        START_TEST: '/start-test',
        GET_STUDENT_TESTS_STATUS: '/get-student-tests-status',
    },

    SystemAdmin: {
        SYSTEM_ADMIN_BASE_PATH: `${API_BASE_URL}${BASE_API}/system-admin`,
        ASSIGN_SCHOOL_MANAGER: '/assign-school-manager',
        ADD_NEW_SCHOOL_TO_SYSTEM: '/add-new-school-to-system',
        ASSIGN_SCHOOL_MANAGER_TO_SCHOOL: '/assign-school-manager-to-school',
        REMOVE_SCHOOL_MANAGER_FROM_SCHOOL: '/remove-school-manager-from-school',
        GET_ALL_SCHOOLS: '/get-all-schools',
    },


    Teacher: {
        TEACHER_BASE_PATH: `${API_BASE_URL}${BASE_API}/teacher`,
        SCHEDULE_FOR_TEACHER: '/schedule-for-teacher',
        GENERATE_TEST_FOR_STUDENTS: '/generate-test-for-students',
        CHECK_TEACHER_TEST: '/check-teacher-test',
    },

    Test: {
        TEST_BASE_PATH: `${API_BASE_URL}${BASE_API}/test`,
        GET_TEST: '/get-test',
    },

    Exam: {
        EXAM_BASE_PATH: `${API_BASE_URL}${BASE_API}/exam`,
        START_ATTEMPT: '/start-attempt',
        SAVE_ANSWERS: '/save-answers',
        SUBMIT_ATTEMPT: '/submit-attempt',
        INTEGRITY_EVENT: '/integrity-event',
        CREATE_EXAM: '/create-exam',
        CLONE_EXAM: '/clone-exam',
        TEACHER_EXAMS: '/teacher-exams',
        EXAM_ATTEMPTS: '/attempts',
        ITEM_ANALYSIS: '/item-analysis',
    },

    UpcomingEvents: {
        UPCOMING_EVENTS_BASE_PATH: `${API_BASE_URL}${BASE_API}/upcoming-events`,
        GET_UPCOMING_EVENTS: '/get-upcoming-events',
    },

    AuditLog: {
        AUDIT_LOG_BASE_PATH: `${API_BASE_URL}${BASE_API}/audit-log`,
        LIST_AUDIT_LOG: '/list',
        AUDIT_ACTIONS: '/actions',
    },

    Impersonation: {
        IMPERSONATION_BASE_PATH: `${API_BASE_URL}${BASE_API}/impersonation`,
        START_IMPERSONATION: '/start',
        STOP_IMPERSONATION: '/stop',
    },

    // Served under SystemAdmin.SYSTEM_ADMIN_BASE_PATH.
    AdminTools: {
        SYSTEM_HEALTH: '/health',
        FEATURE_FLAGS: '/feature-flags',
        SET_FEATURE_FLAG: '/feature-flags/set',
    },

    Adaptive: {
        STUDENT_ADAPTIVE_BASE_PATH: `${API_BASE_URL}${BASE_API}/student`,
        REVIEW_DUE: '/review/due',
        REVIEW_SUMMARY: '/review/summary',
        REVIEW_ANSWER: '/review/answer',
        SKILL_MAP: '/skill-map',
        PRACTICE_HINT: '/practice-hint',
        EXAM_FEEDBACK: '/exam-feedback',
        EXAM_QUESTION_TIMES: '/exam-question-times',
        TEACHER_ADAPTIVE_BASE_PATH: `${API_BASE_URL}${BASE_API}/teacher`,
        TEACHER_CLASSES: '/classes',
        CLASS_SKILL_MAP: '/class-skill-map',
        HINT_CATALOG: '/hint-catalog',
        HINT_TEMPLATE: '/hint-template',
    },

    Ai: {
        AI_BASE_PATH: `${API_BASE_URL}${BASE_API}/ai`,
        AI_STATUS: '/status',
        AI_TUTOR_STREAM: '/tutor/stream',
        AI_TUTOR_HISTORY: '/tutor/history',
        AI_TUTOR_TRANSCRIBE: '/tutor/transcribe',
        AI_OPEN_QUESTIONS: '/open-questions',
        AI_OPEN_QUESTION_ANSWERS: '/open-questions/answers',
    },

    AiStaff: {
        AI_STAFF_BASE_PATH: `${API_BASE_URL}${BASE_API}/ai/staff`,
        AI_QUESTIONS_GENERATE: '/questions/generate',
        AI_QUESTION_DRAFTS: '/questions/drafts',
        AI_QUESTION_BANK: '/questions/bank',
        AI_RUBRICS: '/grading/rubrics',
        AI_GRADING_ANSWERS: '/grading/answers',
        AI_INSIGHTS_LATEST: '/insights/latest',
        AI_INSIGHTS_GENERATE: '/insights/generate',
        AI_USAGE: '/usage',
        AI_SAFETY_FLAGGED: '/safety/flagged',
    },

    User: {
        USER_BASE_PATH: `${API_BASE_URL}${BASE_API}/user`,
        ADD_USER: '/add-user',
        LOGIN: '/login',
        SEND_OTP: '/send-otp',
        VERIFY_OTP: '/verify-otp',
        FORGOT_PASSWORD: '/forgot-password',
        RESET_PASSWORD: '/reset-password',
        GET_USER_PHONE: '/user/phone',
        REFRESH_TOKEN: '/refresh-token',
        LOGOUT: '/logout',
    },

    Engagement: {
        ENGAGEMENT_BASE_PATH: `${API_BASE_URL}${BASE_API}/engagement`,
        ENGAGEMENT_GET_SUMMARY: '/get-summary',
        ENGAGEMENT_CREATE_CLASS_CHALLENGE: '/create-class-challenge',
        ENGAGEMENT_TEACHER_CHALLENGES: '/teacher-challenges',
    },

    StudyPlan: {
        STUDY_PLAN_BASE_PATH: `${API_BASE_URL}${BASE_API}/study-plan`,
        STUDY_PLAN_GET_PLAN: '/get-plan',
        STUDY_PLAN_COMPLETE_TASK: '/complete-task',
    },

    LessonContent: {
        LESSON_CONTENT_BASE_PATH: `${API_BASE_URL}${BASE_API}/lesson-content`,
        LESSON_CONTENT_CREATE: '/create',
        LESSON_CONTENT_UPDATE: '/update',
        LESSON_CONTENT_DELETE: '/delete',
        LESSON_CONTENT_TEACHER_LESSONS: '/teacher-lessons',
        LESSON_CONTENT_STUDENT_LESSONS: '/student-lessons',
        LESSON_CONTENT_GET: '/get',
        LESSON_CONTENT_UPDATE_PROGRESS: '/update-progress',
    },

    Homework: {
        HOMEWORK_BASE_PATH: `${API_BASE_URL}${BASE_API}/homework`,
        HOMEWORK_CREATE: '/create',
        HOMEWORK_TEACHER_CLASSES: '/teacher-classes',
        HOMEWORK_TEACHER_BOARD: '/teacher-board',
        HOMEWORK_STUDENT_HOMEWORK: '/student-homework',
        HOMEWORK_SUBMIT: '/submit',
    },

    Accommodation: {
        ACCOMMODATION_BASE_PATH: `${API_BASE_URL}${BASE_API}/accommodation`,
        ACCOMMODATION_GET: '/get',
        ACCOMMODATION_TEACHER_STUDENTS: '/teacher-students',
        ACCOMMODATION_SET_EXTRA_TIME: '/set-extra-time',
        ACCOMMODATION_SET_PREFERENCES: '/set-preferences',
    },

    QuestionBank: {
        QUESTION_BANK_BASE_PATH: `${API_BASE_URL}${BASE_API}/question-bank`,
        ADD_BANK_QUESTION: '/add',
        UPDATE_BANK_QUESTION: '/update',
        DELETE_BANK_QUESTION: '/delete',
        SEARCH_BANK_QUESTIONS: '/search',
        SET_BANK_QUESTION_SHARED: '/set-shared',
        IMPORT_BANK_QUESTIONS: '/import',
        GET_BANK_TAGS: '/tags',
    },

    Gradebook: {
        GRADEBOOK_BASE_PATH: `${API_BASE_URL}${BASE_API}/gradebook`,
        GET_GRADEBOOK_CLASSES: '/classes',
        GET_GRADEBOOK: '/table',
        ADD_GRADEBOOK_ASSIGNMENT: '/add-assignment',
        DELETE_GRADEBOOK_ASSIGNMENT: '/delete-assignment',
        SET_GRADEBOOK_GRADE: '/set-grade',
        SET_GRADEBOOK_WEIGHTS: '/set-weights',
        EXPORT_GRADEBOOK: '/export',
    },

    LiveQuiz: {
        LIVE_QUIZ_BASE_PATH: `${API_BASE_URL}${BASE_API}/live-quiz`,
        START_LIVE_QUIZ: '/start',
        PUSH_LIVE_QUESTION: '/push-question',
        CLOSE_LIVE_QUESTION: '/close-question',
        END_LIVE_QUIZ: '/end',
        GET_LIVE_QUIZ_RESULTS: '/results',
        GET_CURRENT_LIVE_QUIZ: '/current',
        ANSWER_LIVE_QUESTION: '/answer',
    },

    Attendance: {
        ATTENDANCE_BASE_PATH: `${API_BASE_URL}${BASE_API}/attendance`,
        GET_ATTENDANCE_LESSONS: '/lessons-for-day',
        GET_ATTENDANCE_SHEET: '/sheet',
        MARK_ATTENDANCE: '/mark',
        GET_ATTENDANCE_REPORT: '/report',
    },
};

/* ActiveUser */
export const {
    CONNECTED_USERS_BASE_PATH,
    CONNECT_USER,
    DISCONNECT_USER,
} = PagesConstants.ActiveUser;



/* Lesson */
export const {
    LESSON_BASE_PATH,
    TEACHER_LESSONS,
} = PagesConstants.Lesson;

/* Message */
export const {
    MESSAGE_BASE_PATH,
    RECIPIENT_TYPES,
    SEND_MESSAGE,
    GET_ALL_RECEIVED_MESSAGES,
    RECEIVED_MESSAGES_PAGE,
    SENT_MESSAGES,
    READ_RECEIPTS,
    MESSAGE_MARK_READ,
    REPLY,
    THREAD,
    ATTACHMENT,
} = PagesConstants.Message;

/* Notification */
export const {
    NOTIFICATION_BASE_PATH,
    CONNECT,
    NOTIFICATION_LIST,
    UNREAD_COUNT,
    NOTIFICATION_MARK_READ,
    MARK_ALL_READ,
    PREFERENCES,
    PUSH_PUBLIC_KEY,
    PUSH_SUBSCRIBE,
    PUSH_UNSUBSCRIBE,
} = PagesConstants.Notification;

/* Calendar */
export const {
    CALENDAR_BASE_PATH,
    CALENDAR_TOKEN,
    REGENERATE_CALENDAR_TOKEN,
} = PagesConstants.Calendar;

/* Search */
export const {
    SEARCH_BASE_PATH,
    GLOBAL_SEARCH,
    STUDENTS_PAGE,
} = PagesConstants.Search;

/* Question */
export const {
    QUESTION_BASE_PATH,
    GENERATE_QUESTION,
    SUBMIT_ANSWER,
} = PagesConstants.Question;

/* Schedule */
export const { SCHEDULE_BASE_PATH } = PagesConstants.Schedule;

/* SchoolManager */
export const {
    SCHOOL_MANAGER_BASE_PATH,
    GET_TEACHER_DTO,
    GET_ALL_CLASSES_BY_SCHOOL_CODE,
    GET_SCHOOL_CODE,
    GET_SCHEDULE_FOR_CLASSROOM,
    ASSIGN_USER_AS_SCHOOL_TEACHER,
    REMOVE_TEACHER_FROM_SCHOOL,
    ADD_SCHOOL_GRADES,
    REMOVE_SCHOOL_GRADES,
    ADD_CLASSES_TO_GRADE,
    ADD_ADDITIONAL_CLASS_TO_GRADE,
    ASSIGN_TEACHER_TO_CLASSES,
    REMOVE_TEACHER_FROM_CLASS,
    GET_CLASS_NAMES_BY_SCHOOL_AND_GRADE,
    GET_GRADES,
    ADD_LESSON_TO_TEACHER,
    ADD_TEACHING_SUBJECT_TO_TEACHER,
    REMOVE_TEACHING_SUBJECT_FROM_TEACHER,
    ASSIGN_STUDENT_TO_CLASS,
    CHANGE_STUDENT_CLASS,
    GET_ALL_TEACHERS,
    GET_ALL_STUDENTS,
    GET_TEACHER_TEACHING_SUBJECTS,
    GET_SCHOOL,
} = PagesConstants.SchoolManager;

/* Session */
export const {
    SESSION_BASE_PATH,
    VALIDATE_TOKEN,
    AUTH_HEADER,
} = PagesConstants.Session;

/* UserStatistic */
export const {
    USER_STATISTIC_BASE_PATH,
    GET_USER_STATISTIC,
} = PagesConstants.UserStatistic;

/* Student */
export const {
    STUDENT_BASE_PATH,
    GET_STUDENT_SCHEDULE,

    GENERATE_PRACTICE_TEST,
    CHECK_PRACTICE_TEST,
    START_TEST,
    GET_STUDENT_TESTS_STATUS,
} = PagesConstants.Student;

/* SystemAdmin */
export const {
    SYSTEM_ADMIN_BASE_PATH,
    ASSIGN_SCHOOL_MANAGER,
    ADD_NEW_SCHOOL_TO_SYSTEM,
    ASSIGN_SCHOOL_MANAGER_TO_SCHOOL,
    REMOVE_SCHOOL_MANAGER_FROM_SCHOOL,
    GET_ALL_SCHOOLS,
} = PagesConstants.SystemAdmin;

/* Teacher */
export const {
    TEACHER_BASE_PATH,
    SCHEDULE_FOR_TEACHER,
    GENERATE_TEST_FOR_STUDENTS,
    CHECK_TEACHER_TEST,
} = PagesConstants.Teacher;

/* Test */
export const {
    TEST_BASE_PATH,
    GET_TEST,
} = PagesConstants.Test;

/* Exam */
export const {
    EXAM_BASE_PATH,
    START_ATTEMPT,
    SAVE_ANSWERS,
    SUBMIT_ATTEMPT,
    INTEGRITY_EVENT,
    CREATE_EXAM,
    CLONE_EXAM,
    TEACHER_EXAMS,
    EXAM_ATTEMPTS,
    ITEM_ANALYSIS,
} = PagesConstants.Exam;

/* UpcomingEvents */
export const {
    UPCOMING_EVENTS_BASE_PATH,
    GET_UPCOMING_EVENTS,
} = PagesConstants.UpcomingEvents;

/* AuditLog */
export const {
    AUDIT_LOG_BASE_PATH,
    LIST_AUDIT_LOG,
    AUDIT_ACTIONS,
} = PagesConstants.AuditLog;

/* Impersonation */
export const {
    IMPERSONATION_BASE_PATH,
    START_IMPERSONATION,
    STOP_IMPERSONATION,
} = PagesConstants.Impersonation;

/* AdminTools (under SYSTEM_ADMIN_BASE_PATH) */
export const {
    SYSTEM_HEALTH,
    FEATURE_FLAGS,
    SET_FEATURE_FLAG,
} = PagesConstants.AdminTools;

/* Adaptive learning */
export const {
    STUDENT_ADAPTIVE_BASE_PATH,
    REVIEW_DUE,
    REVIEW_SUMMARY,
    REVIEW_ANSWER,
    SKILL_MAP,
    PRACTICE_HINT,
    EXAM_FEEDBACK,
    EXAM_QUESTION_TIMES,
    TEACHER_ADAPTIVE_BASE_PATH,
    TEACHER_CLASSES,
    CLASS_SKILL_MAP,
    HINT_CATALOG,
    HINT_TEMPLATE,
} = PagesConstants.Adaptive;

/* Ai (paths with an id: `${AI_QUESTION_DRAFTS}/${id}/approve`, `${AI_RUBRICS}/${id}/answers`,
   `${AI_GRADING_ANSWERS}/${id}/suggest|approve`, matching ControllerConstants.AiStaff) */
export const {
    AI_BASE_PATH,
    AI_STATUS,
    AI_TUTOR_STREAM,
    AI_TUTOR_HISTORY,
    AI_TUTOR_TRANSCRIBE,
    AI_OPEN_QUESTIONS,
    AI_OPEN_QUESTION_ANSWERS,
} = PagesConstants.Ai;

export const {
    AI_STAFF_BASE_PATH,
    AI_QUESTIONS_GENERATE,
    AI_QUESTION_DRAFTS,
    AI_QUESTION_BANK,
    AI_RUBRICS,
    AI_GRADING_ANSWERS,
    AI_INSIGHTS_LATEST,
    AI_INSIGHTS_GENERATE,
    AI_USAGE,
    AI_SAFETY_FLAGGED,
} = PagesConstants.AiStaff;

export const {
    USER_BASE_PATH,
    ADD_USER,
    LOGIN,
    SEND_OTP,
    VERIFY_OTP,
    FORGOT_PASSWORD,
    RESET_PASSWORD,
    GET_USER_PHONE,
    REFRESH_TOKEN,
    LOGOUT
} = PagesConstants.User;

/* Engagement (gamification) */
export const {
    ENGAGEMENT_BASE_PATH,
    ENGAGEMENT_GET_SUMMARY,
    ENGAGEMENT_CREATE_CLASS_CHALLENGE,
    ENGAGEMENT_TEACHER_CHALLENGES,
} = PagesConstants.Engagement;

/* StudyPlan */
export const {
    STUDY_PLAN_BASE_PATH,
    STUDY_PLAN_GET_PLAN,
    STUDY_PLAN_COMPLETE_TASK,
} = PagesConstants.StudyPlan;

/* LessonContent (lesson pages, not schedule slots) */
export const {
    LESSON_CONTENT_BASE_PATH,
    LESSON_CONTENT_CREATE,
    LESSON_CONTENT_UPDATE,
    LESSON_CONTENT_DELETE,
    LESSON_CONTENT_TEACHER_LESSONS,
    LESSON_CONTENT_STUDENT_LESSONS,
    LESSON_CONTENT_GET,
    LESSON_CONTENT_UPDATE_PROGRESS,
} = PagesConstants.LessonContent;

/* Homework */
export const {
    HOMEWORK_BASE_PATH,
    HOMEWORK_CREATE,
    HOMEWORK_TEACHER_CLASSES,
    HOMEWORK_TEACHER_BOARD,
    HOMEWORK_STUDENT_HOMEWORK,
    HOMEWORK_SUBMIT,
} = PagesConstants.Homework;

/* Accommodation */
export const {
    ACCOMMODATION_BASE_PATH,
    ACCOMMODATION_GET,
    ACCOMMODATION_TEACHER_STUDENTS,
    ACCOMMODATION_SET_EXTRA_TIME,
    ACCOMMODATION_SET_PREFERENCES,
} = PagesConstants.Accommodation;

/* QuestionBank */
export const {
    QUESTION_BANK_BASE_PATH,
    ADD_BANK_QUESTION,
    UPDATE_BANK_QUESTION,
    DELETE_BANK_QUESTION,
    SEARCH_BANK_QUESTIONS,
    SET_BANK_QUESTION_SHARED,
    IMPORT_BANK_QUESTIONS,
    GET_BANK_TAGS,
} = PagesConstants.QuestionBank;

/* Gradebook */
export const {
    GRADEBOOK_BASE_PATH,
    GET_GRADEBOOK_CLASSES,
    GET_GRADEBOOK,
    ADD_GRADEBOOK_ASSIGNMENT,
    DELETE_GRADEBOOK_ASSIGNMENT,
    SET_GRADEBOOK_GRADE,
    SET_GRADEBOOK_WEIGHTS,
    EXPORT_GRADEBOOK,
} = PagesConstants.Gradebook;

/* LiveQuiz */
export const {
    LIVE_QUIZ_BASE_PATH,
    START_LIVE_QUIZ,
    PUSH_LIVE_QUESTION,
    CLOSE_LIVE_QUESTION,
    END_LIVE_QUIZ,
    GET_LIVE_QUIZ_RESULTS,
    GET_CURRENT_LIVE_QUIZ,
    ANSWER_LIVE_QUESTION,
} = PagesConstants.LiveQuiz;

/* Attendance */
export const {
    ATTENDANCE_BASE_PATH,
    GET_ATTENDANCE_LESSONS,
    GET_ATTENDANCE_SHEET,
    MARK_ATTENDANCE,
    GET_ATTENDANCE_REPORT,
} = PagesConstants.Attendance;


const QueryParams = {
    USER_ID: 'userId=',
    SCHOOL_CODE: 'schoolCode=',
     GRADE_NAME: 'gradeName=',
    CLASS_NAME:  'className=',
    OTP: 'otp=',
    NEW_PASSWORD: 'newPassword=',
    ROLE: 'role=',
    SENDER_ID: 'senderId=',
    RECIPIENT_TYPE: 'recipientType=',
    RECIPIENT_VALUE: 'recipientValue=',
    TITLE: 'title=',
    CONTENT: 'content=',
    CLASSES_COUNT:'classesCount=',
    TEACHER_ID: 'teacherId=',
    STUDENT_ID: 'studentId=',
    SUBJECT_TO_REMOVE: 'subjectToRemove=',
    SCHOOL_MANAGER_ID: 'schoolManagerId=',
    SUBJECT: 'subject=',
    TOPIC: 'topic=',
    SUB_TOPIC: 'subTopic=',
    MESSAGE: 'message=',
    TEST_ID: 'testId=',
    TEST_START_TIME: 'testStartTime=',
    DIFFICULTY: 'difficulty=',
    QUESTION_COUNT: 'questionCount=',
    TIME_LIMIT_MINUTES: 'timeLimitMinutes=',
    CLASS_ROOM_NAME: 'classRoomName=',
    ATTEMPT_ID: 'attemptId=',
    EVENT_TYPE: 'type=',


};

export const {
    USER_ID,
    SCHOOL_CODE,
    GRADE_NAME,
    CLASS_NAME,
    OTP,
    NEW_PASSWORD,
    ROLE,
    SENDER_ID,
    RECIPIENT_TYPE,
    RECIPIENT_VALUE,
    TITLE,
    CONTENT,
    CLASSES_COUNT,
    TEACHER_ID,
    STUDENT_ID,
    SUBJECT_TO_REMOVE,
    SCHOOL_MANAGER_ID,
    SUBJECT,
    TOPIC,
    SUB_TOPIC,
    MESSAGE,
    TEST_ID,
    TEST_START_TIME,
    DIFFICULTY,
    QUESTION_COUNT,
    TIME_LIMIT_MINUTES,
    CLASS_ROOM_NAME,
    ATTEMPT_ID,
    EVENT_TYPE


}=QueryParams;


const QueryDelimiters = {
    QUESTION: '?',
    AND: '&',

};
export const {
    QUESTION,
    AND,
}=QueryDelimiters;


/* SchoolAdministration: the school manager's tools (bulk import, dashboard, timetable builder,
   school years, parent links). Mirrors ControllerConstants.SchoolAdministration. */
export const SchoolAdministrationPaths = {
    SCHOOL_ADMINISTRATION_BASE_PATH: `${API_BASE_URL}${BASE_API}/school-manager`,
    BULK_IMPORT_PREVIEW: '/bulk-import/preview',
    BULK_IMPORT_COMMIT: '/bulk-import/commit',
    SCHOOL_ANALYTICS: '/analytics',
    TIMETABLE_TEACHERS: '/timetable/teachers',
    TIMETABLE_LESSONS: '/timetable/lessons',
    TIMETABLE_CHECK: '/timetable/check',
    TIMETABLE_SAVE: '/timetable/save',
    TIMETABLE_DELETE: '/timetable/delete',
    SCHOOL_YEARS: '/school-years',
    CREATE_SCHOOL_YEAR: '/school-years/create',
    ADD_SEMESTER: '/school-years/add-semester',
    REMOVE_SEMESTER: '/school-years/remove-semester',
    PROMOTION_PLAN: '/school-years/promotion-plan',
    CLOSE_SCHOOL_YEAR: '/school-years/close',
    SCHOOL_YEAR_ARCHIVE: '/school-years/archive',
    LINK_PARENT: '/parents/link',
    UNLINK_PARENT: '/parents/unlink',
};
export const {
    SCHOOL_ADMINISTRATION_BASE_PATH,
    BULK_IMPORT_PREVIEW,
    BULK_IMPORT_COMMIT,
    SCHOOL_ANALYTICS,
    TIMETABLE_TEACHERS,
    TIMETABLE_LESSONS,
    TIMETABLE_CHECK,
    TIMETABLE_SAVE,
    TIMETABLE_DELETE,
    SCHOOL_YEARS,
    CREATE_SCHOOL_YEAR,
    ADD_SEMESTER,
    REMOVE_SEMESTER,
    PROMOTION_PLAN,
    CLOSE_SCHOOL_YEAR,
    SCHOOL_YEAR_ARCHIVE,
    LINK_PARENT,
    UNLINK_PARENT,
} = SchoolAdministrationPaths;

/* Parent. Mirrors ControllerConstants.Parent. */
export const ParentPaths = {
    PARENT_BASE_PATH: `${API_BASE_URL}${BASE_API}/parent`,
    GET_CHILDREN: '/children',
    CHILD_OVERVIEW: '/child-overview',
};
export const {
    PARENT_BASE_PATH,
    GET_CHILDREN,
    CHILD_OVERVIEW,
} = ParentPaths;

/* [s09] Story quests and portfolio. Mirrors ControllerConstants.Quest and ControllerConstants.Portfolio. */
export const QuestPortfolioPaths = {
    QUEST_BASE_PATH: `${API_BASE_URL}${BASE_API}/student/quests`,
    LIST_QUESTS: '/list',
    QUEST_STATE: '/state',
    START_QUEST: '/start',
    ANSWER_QUEST: '/answer',
    QUEST_COSMETICS: '/cosmetics',
    EQUIP_COSMETIC: '/cosmetics/equip',
    PORTFOLIO_BASE_PATH: `${API_BASE_URL}${BASE_API}/student/portfolio`,
    MY_PORTFOLIO: '/me',
    ADD_PORTFOLIO_PROJECT: '/projects/add',
    DELETE_PORTFOLIO_PROJECT: '/projects/delete',
    CREATE_PORTFOLIO_SHARE: '/share/create',
    REVOKE_PORTFOLIO_SHARE: '/share/revoke',
    PORTFOLIO_PUBLIC_BASE_PATH: `${API_BASE_URL}${BASE_API}/portfolio-public`,
};
export const {
    QUEST_BASE_PATH,
    LIST_QUESTS,
    QUEST_STATE,
    START_QUEST,
    ANSWER_QUEST,
    QUEST_COSMETICS,
    EQUIP_COSMETIC,
    PORTFOLIO_BASE_PATH,
    MY_PORTFOLIO,
    ADD_PORTFOLIO_PROJECT,
    DELETE_PORTFOLIO_PROJECT,
    CREATE_PORTFOLIO_SHARE,
    REVOKE_PORTFOLIO_SHARE,
    PORTFOLIO_PUBLIC_BASE_PATH,
} = QuestPortfolioPaths;
