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
    '/terms-of-service'
];

export const UserContext = createContext();

export const UserProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const location = useLocation();
    // const userIdRef = useRef(null);

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
            }

            if (session) {
                setUser({
                    userId: session.userId,
                    role: session.role,
                    username: session.username,
                    schoolCode: session.schoolCode,
                    impersonatorId: session.impersonatorId,
                    impersonatorUsername: session.impersonatorUsername
                });
                return;
            }
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

