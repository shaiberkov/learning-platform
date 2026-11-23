import React, { useState, useEffect, useContext } from 'react';
import axios from 'axios';
import {UserContext} from "../../context/UserContext.jsx";
import Cookies from "universal-cookie";
import FormShell from "../ui/FormShell.jsx";
import {FaEnvelopeOpenText, FaPaperPlane} from "react-icons/fa";
import {
    AUTH_HEADER, MESSAGE_BASE_PATH, QUESTION, RECIPIENT_TYPES,
    SEND_MESSAGE, USER_ID
} from "../../constants/pages.constants.js";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";
import {apiPostForm, errorMessage} from "../../lib/api.js";

const MAX_FILE = 5 * 1024 * 1024;
// Recipient types whose strategy needs a value (a class or grade name).
const NEEDS_VALUE = { "כיתה": "שם הכיתה (למשל ז1)", "שכבה": "שם השכבה", "מורי-שכבה": "שם השכבה" };

function SendMessage() {
    const { user } = useContext(UserContext);

    const [recipientType, setRecipientType] = useState('');
    const [recipientValue, setRecipientValue] = useState('');
    const [availableRecipientTypes, setAvailableRecipientTypes] = useState([]);
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [scheduledFor, setScheduledFor] = useState('');
    const [file, setFile] = useState(null);
    const [message, setMessage] = useState('');
    const [sending, setSending] = useState(false);
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

    // Sent as multipart/form-data so the text is never cut by the URL and a file can ride along.
    const handleSubmit = async (e) => {
        e.preventDefault();
        if (file && file.size > MAX_FILE) {
            setMessage('הקובץ גדול מדי (עד 5MB)');
            return;
        }
        setSending(true);
        try {
            const res = await apiPostForm(`${MESSAGE_BASE_PATH}${SEND_MESSAGE}`, {
                senderId: user.userId,
                recipientType,
                recipientValue,
                title,
                content,
                scheduledFor,
                file,
            });
            setMessage(res.errorCode);
            setTitle('');
            setContent('');
            setFile(null);
            setScheduledFor('');
        } catch (err) {
            setMessage(errorMessage(err, 'שגיאה בשליחת ההודעה'));
            console.error(err);
        } finally {
            setSending(false);
        }
    };

    const valueLabel = NEEDS_VALUE[recipientType];

    return (
        <FormShell eyebrow="Broadcast · הודעות" title="שליחת הודעה" icon={<FaEnvelopeOpenText />} width="medium">
            <form onSubmit={handleSubmit} className="space-y-5">

            {/* סוג נמענים */}
            <div>
                <label htmlFor="msg-recipient-type" className="label">סוג נמענים:</label>
                <select
                    id="msg-recipient-type"
                    value={recipientType}
                    onChange={(e) => setRecipientType(e.target.value)}
                    className="field"
                    required
                >
                    <option key="default" value="">בחר נמען</option>
                    {availableRecipientTypes.map((type) => (
                        <option key={type} value={type}>{type}</option>
                    ))}
                </select>
            </div>

            {valueLabel && (
                <div>
                    <label htmlFor="msg-recipient-value" className="label">{valueLabel}:</label>
                    <input
                        id="msg-recipient-value"
                        type="text"
                        value={recipientValue}
                        onChange={(e) => setRecipientValue(e.target.value)}
                        className="field"
                        required
                    />
                </div>
            )}

            {/* כותרת */}
            <div>
                <label htmlFor="msg-title" className="label">כותרת:</label>
                <input
                    id="msg-title"
                    type="text"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    className="field"
                    placeholder="כתוב כותרת"
                    required
                />
            </div>

            {/* תוכן ההודעה */}
            <div>
                <label htmlFor="msg-content" className="label">תוכן ההודעה:</label>
                <textarea
                    id="msg-content"
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    className="field min-h-[9rem] resize-none"
                    placeholder="כתוב את ההודעה כאן..."
                    required
                />
            </div>

            <div className="grid sm:grid-cols-2 gap-4">
                <div>
                    <label htmlFor="msg-schedule" className="label">תזמון שליחה (לא חובה):</label>
                    <input
                        id="msg-schedule"
                        type="datetime-local"
                        value={scheduledFor}
                        min={new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16)}
                        onChange={(e) => setScheduledFor(e.target.value)}
                        className="field num"
                        aria-describedby="msg-schedule-hint"
                    />
                    <p id="msg-schedule-hint" className="faint text-xs mt-1">השאירו ריק כדי לשלוח מיד.</p>
                </div>
                <div>
                    <label htmlFor="msg-file" className="label">קובץ מצורף (עד 5MB):</label>
                    <input
                        id="msg-file"
                        type="file"
                        onChange={(e) => setFile(e.target.files?.[0] || null)}
                        className="field !py-2"
                    />
                </div>
            </div>

            {/* כפתור שליחה */}
            <button
                type="submit"
                disabled={sending}
                className="btn btn-primary btn-block"
            >
                <FaPaperPlane aria-hidden="true" />
                {scheduledFor ? 'תזמון ההודעה' : 'שלח הודעה'}
            </button>

            {/* הודעת הצלחה או שגיאה */}
            {message && (
                <p className="notice" role="status">{message}</p>
            )}
            </form>
        </FormShell>
    );
}
export default SendMessage
