import axios from "axios";
import { apiGet, apiPost } from "../../lib/api.js";
import {
    EXAM_SIM_BASE_PATH,
    GRADE_TARGET,
    SIM_EXAMS,
    SIM_PREDICTION,
    SIM_RESULT,
    SIM_SAVE,
    SIM_START,
    SIM_SUBMIT,
} from "../../constants/pages.constants.js";

// The acting student comes from the session cookie on the server; no user id is sent.
const url = (path) => `${EXAM_SIM_BASE_PATH}${path}`;

export const fetchSimExams = () => apiGet(url(SIM_EXAMS));
export const fetchPrediction = (examId) => apiGet(url(SIM_PREDICTION), { examId });
/** Returns the whole BasicResponse (data + Hebrew message). */
export const startSimulation = (examId) => apiPost(url(SIM_START), { examId });
export const saveSimulation = (attemptId, answers) => apiPost(url(SIM_SAVE), { attemptId }, answers);
export const fetchSimResult = (attemptId) => apiGet(url(SIM_RESULT), { attemptId });
export const fetchGradeTarget = (target) => apiGet(url(GRADE_TARGET), { target });

/**
 * Submit. A late submission comes back with success=false but still carries the graded result,
 * so this reads the body itself instead of letting apiPost throw.
 */
export async function submitSimulation(attemptId, answers) {
    return (await axios.post(url(SIM_SUBMIT), answers ?? {}, { params: { attemptId } })).data;
}
