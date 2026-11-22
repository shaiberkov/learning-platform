import {useContext, useEffect, useState} from "react";
import axios from "axios";
import {UserContext} from "../../context/UserContext.jsx";
import FormShell from "../../components/ui/FormShell.jsx";
import {FaPaperPlane, FaUserFriends} from "react-icons/fa";
import {schoolGet} from "../../features/school/schoolApi.js";
import {
    AUTH_HEADER,
    GET_ALL_CLASSES_BY_SCHOOL_CODE,
    MESSAGE_BASE_PATH,
    SEND_MESSAGE,
} from "../../constants/pages.constants.js";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";
import Cookies from "universal-cookie";

// Message to the parents of one class (or of the whole school), for teachers and the manager.
// Uses the regular send-message endpoint with the parent recipient types.
const ALL_PARENTS = "__all__";

function MessageParents() {
    const {user} = useContext(UserContext);
    const [classes, setClasses] = useState([]);
    const [target, setTarget] = useState("");
    const [title, setTitle] = useState("");
    const [content, setContent] = useState("");
    const [message, setMessage] = useState("");

    useEffect(() => {
        if (!user?.schoolCode) return;
        schoolGet(GET_ALL_CLASSES_BY_SCHOOL_CODE, {schoolCode: user.schoolCode}).then((res) => {
            if (res.success && Array.isArray(res.data)) setClasses([...res.data].sort());
        });
    }, [user?.schoolCode]);

    const send = async () => {
        if (!target || !title.trim() || !content.trim()) {
            setMessage("יש לבחור נמענים ולמלא כותרת ותוכן");
            return;
        }
        try {
            const res = await axios.post(`${MESSAGE_BASE_PATH}${SEND_MESSAGE}`, null, {
                params: {
                    senderId: user.userId,
                    recipientType: target === ALL_PARENTS ? "כל-ההורים" : "הורי-כיתה",
                    recipientValue: target === ALL_PARENTS ? "" : target,
                    title,
                    content,
                },
                headers: {[AUTH_HEADER]: `${BEARER_PREFIX}${new Cookies().get("token")}`},
            });
            setMessage(res.data.errorCode);
            if (res.data.success) {
                setTitle("");
                setContent("");
            }
        } catch (err) {
            console.error(err);
            setMessage("שגיאה בשליחת ההודעה");
        }
    };

    return (
        <FormShell eyebrow="Parents · הודעה להורים" title="הודעה להורים" icon={<FaUserFriends/>} width="medium">
            <div>
                <label className="label">נמענים</label>
                <select className="field" value={target} onChange={(e) => setTarget(e.target.value)}>
                    <option value="">בחר נמענים</option>
                    {classes.map((c) => <option key={c} value={c}>הורי כיתה {c}</option>)}
                    <option value={ALL_PARENTS}>כל ההורים בבית הספר</option>
                </select>
            </div>
            <div>
                <label className="label">כותרת</label>
                <input className="field" value={title} onChange={(e) => setTitle(e.target.value)}/>
            </div>
            <div>
                <label className="label">תוכן ההודעה</label>
                <textarea className="field min-h-[9rem] resize-none" value={content} onChange={(e) => setContent(e.target.value)}/>
            </div>
            <button className="btn btn-primary btn-block" onClick={send}><FaPaperPlane/> שליחה</button>
            {message && <p className="notice">{message}</p>}
        </FormShell>
    );
}

export default MessageParents;
