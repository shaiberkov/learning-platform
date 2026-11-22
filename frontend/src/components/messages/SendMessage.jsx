import React, { useState, useEffect, useContext } from 'react';
import axios from 'axios';
import {UserContext} from "../../context/UserContext.jsx";
import Cookies from "universal-cookie";
import FormShell from "../ui/FormShell.jsx";
import {FaEnvelopeOpenText, FaPaperPlane} from "react-icons/fa";
import {
    AND,
    ASSIGN_SCHOOL_MANAGER_TO_SCHOOL,
    AUTH_HEADER, CONTENT, MESSAGE_BASE_PATH, QUESTION, RECIPIENT_TYPE, RECIPIENT_TYPES, RECIPIENT_VALUE,
    SEND_MESSAGE, SENDER_ID,
    SYSTEM_ADMIN_BASE_PATH, TITLE, USER_ID
} from "../../constants/pages.constants.js";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";

function SendMessage() {
    const { user } = useContext(UserContext);

    const [recipientType, setRecipientType] = useState('');
    const [recipientValue, setRecipientValue] = useState('');
    const [availableRecipientTypes, setAvailableRecipientTypes] = useState([]);
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [message, setMessage] = useState('');
    const cookies = new Cookies();
    const token = cookies.get('token');

    useEffect(() => {
        const fetchRecipientTypes = async () => {

            try {
                const res = await axios.get(
                    `${MESSAGE_BASE_PATH}${RECIPIENT_TYPES}${QUESTION}${USER_ID}${user.userId}`,
                    {
                        headers: {
                            [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                        }
                    });
                if(res.data.success){
                    setAvailableRecipientTypes(res.data.data);
                }else {
                    setMessage(res.data.errorCode)
                }

            } catch (err) {
                console.error('Failed to fetch recipient types', err);
            }
        };

        if (user) fetchRecipientTypes();
    }, [user]);

    const handleSubmit = async () => {

        try {
            const res = await axios.post(`${MESSAGE_BASE_PATH}${SEND_MESSAGE}${QUESTION}${SENDER_ID}${user.userId}${AND}${RECIPIENT_TYPE}${recipientType}${AND}${RECIPIENT_VALUE}${recipientValue}${AND}${TITLE}${title}${AND}${CONTENT}${content}`
                ,
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    }
                });

                    console.log(res)
            setMessage(res.data.errorCode);
        } catch (err) {
            setMessage('שגיאה בשליחת ההודעה');
            console.error(err);
        }
    };



    return (
        <FormShell eyebrow="Broadcast · הודעות" title="שליחת הודעה" icon={<FaEnvelopeOpenText />} width="medium">

            {/* סוג נמענים */}
            <div>
                <label className="label">סוג נמענים:</label>
                <select
                    value={recipientType}
                    onChange={(e) => setRecipientType(e.target.value)}
                    className="field"
                >
                    <option key="default" value="">בחר נמען</option>
                    {availableRecipientTypes.map((type) => (
                        <option key={type} value={type}>{type}</option>
                    ))}
                </select>
            </div>

            {/* כותרת */}
            <div>
                <label className="label">כותרת:</label>
                <input
                    type="text"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    className="field"
                    placeholder="כתוב כותרת"
                />
            </div>

            {/* תוכן ההודעה */}
            <div>
                <label className="label">תוכן ההודעה:</label>
                <textarea
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    className="field min-h-[9rem] resize-none"
                    placeholder="כתוב את ההודעה כאן..."
                />
            </div>

            {/* כפתור שליחה */}
            <button
                onClick={handleSubmit}
                className="btn btn-primary btn-block"
            >
                <FaPaperPlane />
                שלח הודעה
            </button>

            {/* הודעת הצלחה או שגיאה */}
            {message && (
                <p className="notice">{message}</p>
            )}
        </FormShell>
    );
}
export default SendMessage
