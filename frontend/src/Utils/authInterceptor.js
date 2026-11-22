import axios from "axios";
import Cookies from "universal-cookie";
import {API_BASE_URL} from "../constants/base.constants.js";
import {USER_BASE_PATH} from "../constants/pages.constants.js";
import {AUTH_HEADER, BEARER_PREFIX} from "../constants/shared.constant.js";

// The server enforces roles and ownership, so every API call needs the JWT. Screens that already
// set the header keep theirs; the public login/register endpoints never get a (possibly stale) token.
export function installAuthInterceptor() {
    axios.interceptors.request.use((config) => {
        const url = config.url || "";
        if (!url.startsWith(API_BASE_URL) || url.startsWith(USER_BASE_PATH)) {
            return config;
        }
        if (!config.headers?.[AUTH_HEADER]) {
            const token = new Cookies().get("token");
            if (token) {
                config.headers = config.headers || {};
                config.headers[AUTH_HEADER] = `${BEARER_PREFIX}${token}`;
            }
        }
        return config;
    });
}
