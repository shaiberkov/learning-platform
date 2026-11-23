import axios from "axios";
import { EXAM_BASE_PATH } from "../../constants/pages.constants.js";

// Thin wrapper for the /exam endpoints: returns the BasicResponse body. Auth is the httpOnly
// cookie that axios sends (Utils/httpSetup.js).
const headers = () => ({
    "Content-Type": "application/json",
});

export const examGet = async (path, params) =>
    (await axios.get(`${EXAM_BASE_PATH}${path}`, { params, headers: headers() })).data;

export const examPost = async (path, params, body = {}) =>
    (await axios.post(`${EXAM_BASE_PATH}${path}`, body, { params, headers: headers() })).data;

export const KIND_LABELS = {
    TEXT: "תשובה פתוחה (מחולל)",
    MULTI_SELECT: "בחירה מרובה",
    NUMERIC: "מספרית עם סטייה",
    MATCHING: "התאמה",
    ORDERING: "סידור",
    SHORT_OPEN: "תשובה קצרה",
};
