import axios from "axios";
import {
    CLASS_SKILL_MAP,
    EXAM_FEEDBACK,
    EXAM_QUESTION_TIMES,
    HINT_CATALOG,
    HINT_TEMPLATE,
    PRACTICE_HINT,
    REVIEW_ANSWER,
    REVIEW_DUE,
    REVIEW_SUMMARY,
    SKILL_MAP,
    STUDENT_ADAPTIVE_BASE_PATH,
    TEACHER_ADAPTIVE_BASE_PATH,
    TEACHER_CLASSES,
} from "../../constants/pages.constants.js";

// Calls for the adaptive-learning screens. Each returns the server's BasicResponse
// ({ success, errorCode, data }); a network failure becomes { success: false, errorCode }.

const NETWORK_ERROR = "לא ניתן להתחבר לשרת. נסה שוב מאוחר יותר.";

const call = async (request) => {
    try {
        const res = await request();
        return res.data;
    } catch (err) {
        return { success: false, errorCode: err?.response?.data?.errorCode || NETWORK_ERROR };
    }
};

// Auth is the httpOnly cookie that axios sends (Utils/httpSetup.js).
const get = (url, params) => call(() => axios.get(url, { params }));
const post = (url, params, body = {}) => call(() => axios.post(url, body, { params }));

/* Mistakes notebook */
export const fetchReviewSummary = (userId) => get(`${STUDENT_ADAPTIVE_BASE_PATH}${REVIEW_SUMMARY}`, { userId });
export const fetchDueReviewItems = (userId) => get(`${STUDENT_ADAPTIVE_BASE_PATH}${REVIEW_DUE}`, { userId });
export const answerReviewItem = (userId, itemId, answer) =>
    post(`${STUDENT_ADAPTIVE_BASE_PATH}${REVIEW_ANSWER}`, { userId, itemId, answer });

/* Mastery */
export const fetchSkillMap = (userId) => get(`${STUDENT_ADAPTIVE_BASE_PATH}${SKILL_MAP}`, { userId });
export const fetchTeacherClasses = (teacherId) => get(`${TEACHER_ADAPTIVE_BASE_PATH}${TEACHER_CLASSES}`, { teacherId });
export const fetchClassSkillMap = (teacherId, classRoomId) =>
    get(`${TEACHER_ADAPTIVE_BASE_PATH}${CLASS_SKILL_MAP}`, { teacherId, classRoomId });

/* Hints */
export const fetchPracticeHint = (userId, questionId, step) =>
    get(`${STUDENT_ADAPTIVE_BASE_PATH}${PRACTICE_HINT}`, { userId, questionId, step });
export const fetchHintCatalog = () => get(`${TEACHER_ADAPTIVE_BASE_PATH}${HINT_CATALOG}`);
export const fetchHintTemplate = (subject, subTopic) =>
    get(`${TEACHER_ADAPTIVE_BASE_PATH}${HINT_TEMPLATE}`, { subject, subTopic });
export const saveHintTemplate = (teacherId, template) =>
    post(`${TEACHER_ADAPTIVE_BASE_PATH}${HINT_TEMPLATE}`, { teacherId }, template);

/* Exam feedback */
export const sendExamQuestionTimes = (userId, testId, secondsByQuestion) =>
    post(`${STUDENT_ADAPTIVE_BASE_PATH}${EXAM_QUESTION_TIMES}`, { userId, testId }, secondsByQuestion);
export const fetchExamFeedback = (userId, testId) =>
    get(`${STUDENT_ADAPTIVE_BASE_PATH}${EXAM_FEEDBACK}`, { userId, testId });

/* Shared display helpers */
export const MATH_SUBJECT = "מתמטיקה";

export const SKILL_COLORS = {
    GREEN: { label: "שולט", cls: "bg-mint-soft text-mint border-[color-mix(in_oklab,var(--mint)_45%,transparent)]", dot: "bg-mint" },
    YELLOW: { label: "בדרך", cls: "bg-amber-soft text-amber border-[color-mix(in_oklab,var(--amber)_45%,transparent)]", dot: "bg-amber" },
    RED: { label: "צריך חיזוק", cls: "bg-danger-soft text-danger border-[color-mix(in_oklab,var(--danger)_45%,transparent)]", dot: "bg-danger" },
    NONE: { label: "טרם תורגל", cls: "bg-surface-sunk text-ink-3 border-line", dot: "bg-line-strong" },
};

export const skillColor = (color) => SKILL_COLORS[color] || SKILL_COLORS.NONE;

export const practicePath = (subject, topic, subTopic) =>
    `/exercises/${subject}/${topic}/${subTopic}`;

export const formatSeconds = (s) => {
    if (s === null || s === undefined) return "—";
    const m = Math.floor(s / 60);
    return m > 0 ? `${m}:${String(s % 60).padStart(2, "0")} דק'` : `${s} שנ'`;
};
