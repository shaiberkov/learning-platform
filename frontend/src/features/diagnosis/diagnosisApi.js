import axios from "axios";
import {
    CLASS_FEYNMAN,
    CLASS_MISTAKE_TYPES,
    FEYNMAN_EXPLANATIONS,
    FEYNMAN_PROMPTS,
    MISTAKE_DIAGNOSIS,
    STUDENT_DIAGNOSIS_BASE_PATH,
    TEACHER_DIAGNOSIS_BASE_PATH,
} from "../../constants/pages.constants.js";

// [s06] Calls for "explain it to me" (Feynman) and the mistake-type diagnosis. The student or teacher
// is taken from the session cookie on the server. Each call resolves to the server's BasicResponse
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

const student = (path) => `${STUDENT_DIAGNOSIS_BASE_PATH}${path}`;
const teacher = (path) => `${TEACHER_DIAGNOSIS_BASE_PATH}${path}`;

/* Explain it to me (Feynman) */
export const fetchFeynmanPrompts = () => call(() => axios.get(student(FEYNMAN_PROMPTS)));
export const fetchFeynmanHistory = () => call(() => axios.get(student(FEYNMAN_EXPLANATIONS)));
export const submitFeynmanExplanation = (promptKey, text, inputMode) =>
    call(() => axios.post(student(FEYNMAN_EXPLANATIONS), { promptKey, text, inputMode }));

/* Mistake-type diagnosis */
export const fetchMistakeDiagnosis = () => call(() => axios.get(student(MISTAKE_DIAGNOSIS)));
export const selfCheckMistake = (itemId, answer) =>
    call(() => axios.post(student(`${MISTAKE_DIAGNOSIS}/${itemId}/self-check`), null, { params: { answer } }));
export const analyzeMistakeWithAi = (itemId) =>
    call(() => axios.post(student(`${MISTAKE_DIAGNOSIS}/${itemId}/ai`)));

/* Teacher */
export const fetchClassMistakeTypes = (classRoomId) =>
    call(() => axios.get(teacher(CLASS_MISTAKE_TYPES), { params: { classRoomId } }));
export const fetchClassFeynman = (classRoomId) =>
    call(() => axios.get(teacher(CLASS_FEYNMAN), { params: { classRoomId } }));
