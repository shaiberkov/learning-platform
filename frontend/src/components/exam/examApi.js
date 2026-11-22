import axios from "axios";
import Cookies from "universal-cookie";
import { AUTH_HEADER, EXAM_BASE_PATH } from "../../constants/pages.constants.js";
import { BEARER_PREFIX } from "../../constants/shared.constant.js";

// Thin wrapper for the /exam endpoints: adds the bearer token and returns the BasicResponse body.
const headers = () => ({
    [AUTH_HEADER]: `${BEARER_PREFIX}${new Cookies().get("token")}`,
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
