import axios from "axios";
import Cookies from "universal-cookie";
import { AUTH_HEADER, BEARER_PREFIX } from "../constants/shared.constant.js";

// axios config with the JWT from the `token` cookie, plus optional query params.
export const authConfig = (params, extra = {}) => ({
    ...extra,
    params,
    headers: { [AUTH_HEADER]: `${BEARER_PREFIX}${new Cookies().get("token")}`, ...(extra.headers || {}) },
});

// GET/POST/PUT/DELETE returning the server's BasicResponse ({ success, errorCode, data }).
// A network or server failure comes back as { success: false, errorCode: <Hebrew message> }.
const call = async (request) => {
    try {
        const response = await request();
        return response.data;
    } catch (error) {
        console.error(error);
        return { success: false, errorCode: "שגיאה בתקשורת עם השרת", data: null };
    }
};

export const apiGet = (url, params) => call(() => axios.get(url, authConfig(params)));
export const apiPost = (url, body, params) => call(() => axios.post(url, body ?? null, authConfig(params)));
export const apiPut = (url, body, params) => call(() => axios.put(url, body ?? null, authConfig(params)));
export const apiDelete = (url, params) => call(() => axios.delete(url, authConfig(params)));
