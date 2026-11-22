import {useContext, useState} from "react";
import {UserContext} from "../../context/UserContext.jsx";
import FormShell from "../../components/ui/FormShell.jsx";
import {FaUserFriends} from "react-icons/fa";
import {schoolPost} from "../../features/school/schoolApi.js";
import {LINK_PARENT, UNLINK_PARENT} from "../../constants/pages.constants.js";

// The manager links a registered user (or an existing parent) to a student of the school.
function ParentLinks() {
    const {user} = useContext(UserContext);
    const [parentId, setParentId] = useState("");
    const [studentId, setStudentId] = useState("");
    const [message, setMessage] = useState("");

    const submit = async (path) => {
        if (!parentId || !studentId) {
            setMessage("יש להזין תעודת זהות של ההורה ושל התלמיד");
            return;
        }
        const res = await schoolPost(path, {schoolCode: user.schoolCode, parentId, studentId});
        setMessage(res.errorCode);
    };

    return (
        <FormShell eyebrow="Parents · הורים" title="קישור הורה לתלמיד" icon={<FaUserFriends/>}>
            <p className="muted text-sm">
                ההורה צריך להיות רשום במערכת. משתמש רשום שאינו משויך לבית ספר יהפוך לחשבון הורה.
                אפשר לקשר הורה לכמה ילדים. הורים חדשים אפשר גם לייבא מקובץ CSV.
            </p>
            <div>
                <label className="label">תעודת זהות של ההורה</label>
                <input className="field" value={parentId} onChange={(e) => setParentId(e.target.value.trim())}/>
            </div>
            <div>
                <label className="label">תעודת זהות של התלמיד</label>
                <input className="field" value={studentId} onChange={(e) => setStudentId(e.target.value.trim())}/>
            </div>
            <div className="flex gap-2">
                <button className="btn btn-primary" onClick={() => submit(LINK_PARENT)}>קישור</button>
                <button className="btn btn-ghost" onClick={() => submit(UNLINK_PARENT)}>הסרת קישור</button>
            </div>
            {message && <p className="notice">{message}</p>}
        </FormShell>
    );
}

export default ParentLinks;
