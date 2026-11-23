import React, {createContext, useState, useEffect} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import axios from 'axios';
import {
    SESSION_BASE_PATH,
    VALIDATE_TOKEN
} from "../constants/pages.constants.js";
import {refreshSession} from "../Utils/httpSetup.js";

const PUBLIC_PATHS = [
    '/login',
    '/register',
    '/reset-password',
    '/two-factor-auth',
    '/about',
    '/faq',
    '/privacy-policy',
    '/terms-of-service',
    '/accessibility'
];

export const UserContext = createContext();

// The last validated user, kept so the app (offline practice) still works when the
// session check cannot reach the server. The session itself is in httpOnly cookies;
// this holds only the display data the session check returned. Cleared on logout.
const OFFLINE_USER_KEY = 'lp-offline-user';

function rememberUser(user) {
    try {
        localStorage.setItem(OFFLINE_USER_KEY, JSON.stringify({ user }));
    } catch {
        /* storage unavailable: offline mode then needs a connection to start */
    }
}

function rememberedUser() {
    try {
        const saved = JSON.parse(localStorage.getItem(OFFLINE_USER_KEY) || 'null');
        return saved?.user ?? null;
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
    const location = useLocation();

    const navigate=useNavigate()


    useEffect(() => {
        const isPublicPage = PUBLIC_PATHS.includes(location.pathname);

        // The session lives in httpOnly cookies the page cannot read, so ask the server.
        const fetchSession = async () => {
            const response = await axios.get(`${SESSION_BASE_PATH}${VALIDATE_TOKEN}`);
            return response.data?.valid ? response.data : null;
        };

        const validateAndSetUser = async () => {
            let session = null;
            try {
                session = await fetchSession();
                if (!session && await refreshSession()) {
                    session = await fetchSession();
                }
            } catch (error) {
                console.error('❌ שגיאה בבדיקת התחברות:', error.response?.status ?? error.message);
                // No answer from the server (offline / server asleep): keep the last validated
                // user instead of logging out.
                const offlineUser = !error.response && axios.isAxiosError(error) ? rememberedUser() : null;
                if (offlineUser) {
                    setUser(offlineUser);
                    return;
                }
            }

            if (session) {
                const validated = {
                    userId: session.userId,
                    role: session.role,
                    username: session.username,
                    schoolCode: session.schoolCode,
                    impersonatorId: session.impersonatorId,
                    impersonatorUsername: session.impersonatorUsername
                };
                setUser(validated);
                rememberUser(validated);
                return;
            }
            forgetUser();
            setUser(null);
            if (!isPublicPage) {
                navigate('/login');
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
