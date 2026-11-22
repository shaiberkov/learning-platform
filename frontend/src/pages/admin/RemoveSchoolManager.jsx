import { useState } from "react";
import axios from "axios";
import Cookies from "universal-cookie";
import FormShell from "../../components/ui/FormShell.jsx";
import {FaCheckCircle, FaIdCard} from "react-icons/fa";
import {
    AUTH_HEADER,
    QUESTION,
    REMOVE_SCHOOL_MANAGER_FROM_SCHOOL,
    SYSTEM_ADMIN_BASE_PATH, USER_ID
} from "../../constants/pages.constants.js";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";


function RemoveSchoolManager() {
    const [schoolManagerId, setSchoolManagerId] = useState("");
    const [errorCode, setErrorCode] = useState("");
    const cookies = new Cookies();
    const token = cookies.get('token');

    const removeSchoolManagerFromSchool = async () => {
        setErrorCode("");
        if (!schoolManagerId) {
            setErrorCode("נא לרשום תז של מנהל בית ספר");
            return;
        }

        try {

            const response = await axios.post(
                `${SYSTEM_ADMIN_BASE_PATH}${REMOVE_SCHOOL_MANAGER_FROM_SCHOOL}${QUESTION}${USER_ID}${schoolManagerId}`,
                {},
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    }
                }
            );

            if (response.data && response.data.errorCode) {
                setErrorCode(response.data.errorCode);
                setSchoolManagerId("")
            } else {
                setErrorCode("אין תגובה מהשרת");
            }
        } catch (error) {
            setErrorCode("שגיאה בעת שליחת הבקשה");
            console.error(error);
        }
    };

    return (
        <FormShell eyebrow="Schools · בתי ספר" title="הסרת מנהל מבית ספר" icon={<FaIdCard />} tone="icon-badge--danger">

            <div>
                <div className="field-wrap">
                    <FaIdCard/>
                    <input
                        type="text"
                        value={schoolManagerId}
                        onChange={(e) => setSchoolManagerId(e.target.value)}
                        placeholder="תז של מנהל"
                        className="field num"
                    />
                </div>
            </div>

            <button
                onClick={removeSchoolManagerFromSchool}
                disabled={schoolManagerId.length === 0}
                className="btn btn-danger btn-block"
            >
                הסר מנהל
            </button>


            {errorCode && (
                <p className="notice">
                    {errorCode}
                </p>
            )}
        </FormShell>
    );
}

export default RemoveSchoolManager;
