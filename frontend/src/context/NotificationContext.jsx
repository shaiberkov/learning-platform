import {createContext, useContext, useEffect, useRef, useState} from "react";
import { UserContext } from "./UserContext";
import {API_BASE_URL, BASE_API} from "../constants/base.constants.js";
import {CONNECT, NOTIFICATION_BASE_PATH, QUESTION, USER_ID} from "../constants/pages.constants.js";

export const NotificationContext = createContext();

export function NotificationProvider({ children }) {
    const [messages, setMessages] = useState([]);
    const [tests, setTests] = useState([]);
    // Live in-class quiz: the last state pushed to a student, the last results pushed to the teacher.
    const [liveQuiz, setLiveQuiz] = useState(null);
    const [liveQuizResults, setLiveQuizResults] = useState(null);
    const { user } = useContext(UserContext);


    useEffect(() => {
        if (!user?.userId) return;

        const eventSource = new EventSource(
            `${NOTIFICATION_BASE_PATH}${CONNECT}${QUESTION}${USER_ID}${user.userId}`
        );

        eventSource.addEventListener("message", (event) => {
            try {
                const data = JSON.parse(event.data);
                console.log(data)
                if (data.type === "SYSTEM_MESSAGE") {
                    setMessages(prevMessages => [data.payload.message, ...prevMessages]);
                } else if (data.type === "NEW_TEST") {
                    if (user.role === "STUDENT") {
                        setTests(prevTests => [data.payload.userTestStatusDTO, ...prevTests]);
                    } else {
                        console.warn("User not authorized to receive test notifications");
                    }
                } else if (data.type === "LIVE_QUIZ_STATE") {
                    setLiveQuiz({ ...data.payload, receivedAt: Date.now() });
                } else if (data.type === "LIVE_QUIZ_RESULTS") {
                    setLiveQuizResults({ ...data.payload, receivedAt: Date.now() });
                }
            } catch (err) {
                console.error("Failed to parse SSE notification:", err);
            }
        });

        eventSource.onerror = (err) => {
            console.error("SSE error:", err);
            eventSource.close();
        };

        return () => {
            eventSource.close();
        };
    }, [user?.userId]);

    return (
        <NotificationContext.Provider value={{ messages, tests, liveQuiz, liveQuizResults }}>
            {children}
        </NotificationContext.Provider>
    );
}

export function useNotificationContext() {
    return useContext(NotificationContext);
}
