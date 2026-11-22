// Small axios wrappers for the newer screens: Bearer token from the cookie, query params
// encoded by axios (Hebrew-safe), and the server's BasicResponse returned as is.
import axios from "axios";
import Cookies from "universal-cookie";
import { AUTH_HEADER, BEARER_PREFIX } from "../constants/shared.constant.js";

const authHeaders = () => ({ [AUTH_HEADER]: `${BEARER_PREFIX}${new Cookies().get("token")}` });

export const apiGet = (url, params) =>
    axios.get(url, { params, headers: authHeaders() }).then((r) => r.data);

export const apiPost = (url, params, body = {}) =>
    axios.post(url, body, { params, headers: authHeaders() }).then((r) => r.data);

export const apiPut = (url, params, body = {}) =>
    axios.put(url, body, { params, headers: authHeaders() }).then((r) => r.data);

export const apiDelete = (url, params) =>
    axios.delete(url, { params, headers: authHeaders() }).then((r) => r.data);

/** Hebrew text to show for a failed request. */
export const errorText = (err) => err?.response?.data?.errorCode || "אירעה שגיאה בחיבור לשרת";

/** dd/MM/yyyy HH:mm for ISO date-times coming from the server. */
export const formatDateTime = (iso) => {
    if (!iso) return "";
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return iso;
    const pad = (n) => String(n).padStart(2, "0");
    return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

/** dd/MM for ISO dates (yyyy-MM-dd). */
export const formatShortDate = (isoDate) => {
    if (!isoDate) return "";
    const [, m, d] = isoDate.split("-");
    return `${d}/${m}`;
};
