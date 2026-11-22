
import React, {createContext, useState, useEffect, useRef} from 'react';
import Cookies from 'universal-cookie';
import {useLocation, useNavigate} from 'react-router-dom';
import axios from 'axios';
import {
    AUTH_HEADER,
    QUESTION,
    SEND_OTP,
    SESSION_BASE_PATH,
    USER_BASE_PATH,
    VALIDATE_TOKEN
} from "../constants/pages.constants.js";
import {BEARER_PREFIX} from "../constants/shared.constant.js";
import {API_BASE_URL, BASE_API} from "../constants/base.constants.js";

export const UserContext = createContext();

// The last validated user, kept so the app (offline practice) still works when the
// session check cannot reach the server. Tied to the token it was validated with.
const OFFLINE_USER_KEY = 'lp-offline-user';

function rememberUser(token, user) {
    try {
        localStorage.setItem(OFFLINE_USER_KEY, JSON.stringify({ tokenTail: String(token).slice(-24), user }));
    } catch {
        /* storage unavailable: offline mode then needs a connection to start */
    }
}

function rememberedUser(token) {
    try {
        const saved = JSON.parse(localStorage.getItem(OFFLINE_USER_KEY) || 'null');
        return saved && saved.tokenTail === String(token).slice(-24) ? saved.user : null;
    } catch {
        return null;
    }
}

export function forgetUser() {
    try {
        localStorage.removeItem(OFFLINE_USER_KEY);
    } catch {
        /* nothing to clear */
    }
}

export const UserProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const cookies = new Cookies();
    const token = cookies.get('token');
    const location = useLocation();
    // const userIdRef = useRef(null);

    const navigate=useNavigate()


    useEffect(() => {
        const validateAndSetUser = async () => {


            if (!token) {
                setUser(null);
                if (
                    location.pathname !== '/login' &&
                    location.pathname !== '/register' &&
                    location.pathname !== '/reset-password' &&
                    location.pathname !== '/two-factor-auth' &&
                    location.pathname !== '/about' &&
                    location.pathname !== '/faq' &&
                    location.pathname !== '/privacy-policy' &&
                    location.pathname !== '/terms-of-service' &&
                    location.pathname !== '/accessibility'
                ) {
                    navigate('/login');
                }
                return;
            }
            console.log("🔍 בודק טוקן");
            try {
                const response = await axios.get(
                    `${SESSION_BASE_PATH}${VALIDATE_TOKEN}`
                    ,
                    {
                        headers: {
                            [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                            ,
                        }
                    }
                );

                if (response.data.valid) {
                    // const newUserId = response.data.userId;
                    // console.log("👁‍🗨 userIdRef.current:", userIdRef.current);
                    // console.log("👁‍🗨 newUserId:", newUserId);
                    // if (userIdRef.current === newUserId) {
                    //     console.log("ℹ️ userId לא השתנה - לא מבצע setUser");
                    //     return;
                    // }
                    const validated = {
                        userId:response.data.userId,
                        role:response.data.role,
                        username:response.data.username,
                        schoolCode:response.data.schoolCode
                    };
                    setUser(validated);
                    rememberUser(token, validated);
                    // userIdRef.current = newUserId;
                    console.log(`✅ טוקן תקף - userId: ${response.data.userId}, role: ${response.data.role}, username: ${response.data.username},schoolCode:${response.data.schoolCode}`);
                } else {
                    throw new Error('✖️ נתוני טוקן לא שלמים');
                }

            } catch (error) {
                if (error.response) {
                    if (error.response.status === 401) {
                        console.error('⚠️ לא מורשה - טוקן לא תקף (401)');
                    } else if (error.response.status === 403) {
                        console.error('🚫 אין הרשאה (403)');
                    } else {
                        console.error(`❌ שגיאה לא צפויה: ${error.response.status}`);
                    }
                } else {
                    console.error('❌ שגיאה בבדיקת טוקן:', error.message);
                    // No answer from the server (offline / server asleep): keep the last
                    // validated user for this token instead of logging out.
                    const offlineUser = axios.isAxiosError(error) ? rememberedUser(token) : null;
                    if (offlineUser) {
                        setUser(offlineUser);
                        return;
                    }
                }
                forgetUser();
                navigate('/login');
                cookies.remove('token');
                setUser(null);
            }
        };

        validateAndSetUser();
    }, [location]);

    return (
        <UserContext.Provider value={{ user, setUser }}>
            {children}
        </UserContext.Provider>
    );
};

