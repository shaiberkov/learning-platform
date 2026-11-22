// components/MessageList.jsx
import {useEffect, useState} from "react";
import axios from "axios";
import Cookies from "universal-cookie";
import {useNotificationContext} from "../../context/NotificationContext.jsx";
import {
    AUTH_HEADER,
    GET_ALL_RECEIVED_MESSAGES, MESSAGE_BASE_PATH, QUESTION,
    SESSION_BASE_PATH, USER_ID,
    VALIDATE_TOKEN
} from "../../constants/pages.constants.js";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";

function MessageList({ userId }) {
    const [messagesList, setMessagesList] = useState([]);
    const {messages} = useNotificationContext();
    const cookies = new Cookies();
    const token = cookies.get("token");

    useEffect(() => {
        const getReceivedMessages = async () => {
            try {
                const response = await axios.get(
                    `${MESSAGE_BASE_PATH}${GET_ALL_RECEIVED_MESSAGES}${QUESTION}${USER_ID}${userId}`,

                {
                        headers: {
                            [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                        },
                    }
                );
                const sortedMessages = response.data.data.sort(
                    (a, b) => new Date(b.sentAt) - new Date(a.sentAt)
                );
                setMessagesList(sortedMessages);
            } catch (error) {
                console.error("Failed to fetch received messages:", error);
            }
        };

        if (userId) {
            getReceivedMessages();
        }
    }, [userId]);
    const combinedMessages = [...messages, ...messagesList];

    return (
        <div className="h-full">
            <div
                className="glass tile w-full h-full min-h-[24rem] overflow-hidden flex flex-col p-5 sm:p-6"
                dir="rtl"
            >
                <div className="panel-head">
                    <div className="flex items-center gap-3">
                        <span className="icon-badge">
                            <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16v10H8l-4 4V6z"/></svg>
                        </span>
                        <div>
                            <span className="eyebrow">Inbox</span>
                            <h2 className="title text-xl mt-0.5">הודעות</h2>
                        </div>
                    </div>
                    <span className="chip num">{combinedMessages.length}</span>
                </div>

                {combinedMessages.length > 0 ? (
                    <div className="flex-1 max-h-[22rem] scroll-area -mx-1 px-1 py-2">
                        <ul className="space-y-3">
                            {combinedMessages.map((message, index) => (
                                <li
                                    key={index}
                                    className="row-card reveal"
                                    style={{ "--i": Math.min(index, 6) }}
                                >
                                    <div className="flex justify-between items-center gap-3 mb-2">
                                        <span className="chip">{message.senderName}</span>
                                        <span className="num faint text-[0.7rem]" dir="ltr">{new Date(message.sentAt).toLocaleString()}</span>
                                    </div>
                                    <p className="title text-base">{message.title}</p>
                                    <p className="muted text-sm mt-1 leading-relaxed break-words">{message.content}</p>
                                </li>
                            ))}
                        </ul>
                    </div>
                ) : (
                    <div className="flex-1 flex flex-col items-center justify-center gap-3 p-4 text-center">
                        <span className="w-14 h-14 rounded-2xl border border-dashed border-line-strong grid place-items-center text-ink-3 rotate-45">
                            <span className="-rotate-45 num">0</span>
                        </span>
                        <p className="faint">אין הודעות להציג.</p>
                    </div>
                )}
            </div>
        </div>
    );

}

export default MessageList;
