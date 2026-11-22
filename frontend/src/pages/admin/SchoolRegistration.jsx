import { useNavigate } from "react-router-dom";
import { useState } from "react";
import axios from "axios";
import { FaUser, FaSchool, FaKey } from "react-icons/fa";

import Cookies from "universal-cookie";
import FormShell from "../../components/ui/FormShell.jsx";
import {
    ADD_NEW_SCHOOL_TO_SYSTEM,
    ASSIGN_SCHOOL_MANAGER, ASSIGN_SCHOOL_MANAGER_TO_SCHOOL, AUTH_HEADER,
    SYSTEM_ADMIN_BASE_PATH
} from "../../constants/pages.constants.js";
import {BEARER_PREFIX} from "../../constants/shared.constant.js"; // Universal Cookies

function SchoolRegistration() {
    const [userId, setUserId] = useState("");
    const [schoolCode, setSchoolCode] = useState("");
    const [schoolName, setSchoolName] = useState("");
    const [userIdError, setUserIdError] = useState("");
    const [schoolCodeError, setSchoolCodeError] = useState("");
    const [message, setMessage] = useState("");

    const navigate = useNavigate();
    const cookies = new Cookies();

    const assignSchoolManager = async () => {
        setUserIdError("");
        setSchoolCodeError("");
        setMessage("");

        if (!userId || !schoolCode || !schoolName) {
            setMessage("אנא מלא את כל השדות");
            return;
        }

        try {
            const token = cookies.get("token");

            const assignResponse = await axios.post(
                `${SYSTEM_ADMIN_BASE_PATH}${ASSIGN_SCHOOL_MANAGER}`,
                new URLSearchParams({ userId }),
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    },
                }
            );

            if (!assignResponse.data.success) {
                setUserIdError(assignResponse.data.errorCode || "שגיאה במינוי המנהל");
                return;
            }

            const addSchoolResponse = await axios.post(

                `${SYSTEM_ADMIN_BASE_PATH}${ ADD_NEW_SCHOOL_TO_SYSTEM}`,
                new URLSearchParams({ schoolName, schoolCode }),
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`

                    },
                }
            );

            if (addSchoolResponse.data.success) {
                setMessage("בית הספר נוסף והמנהל מונה בהצלחה!");

                const connectResponse = await axios.post(

                    `${SYSTEM_ADMIN_BASE_PATH}${ASSIGN_SCHOOL_MANAGER_TO_SCHOOL}`,
                    new URLSearchParams({ userId, schoolCode }),
                    {
                        headers: {
                            [AUTH_HEADER]: `${BEARER_PREFIX}${token}`

                        },
                    }
                );

                if (connectResponse.data.success) {
                    setMessage(connectResponse.data.errorCode);

                } else {
                    setMessage(connectResponse.data.errorCode || "שגיאה בשיוך המנהל לבית הספר");
                }
            } else {
                setSchoolCodeError(addSchoolResponse.data.errorCode || "שגיאה בהוספת בית הספר");
            }

        } catch (err) {
        }
    };


    return (
        <FormShell eyebrow="Schools · בתי ספר" title="מינוי מנהל והוספת בית ספר" icon={<FaSchool />} tone="icon-badge--mint">

            <div>
                <div className="field-wrap">
                    <FaUser/>
                    <input
                        type="text"
                        placeholder="מזהה משתמש"
                        value={userId}
                        onChange={(e) => setUserId(e.target.value)}
                        className="field num"
                    />
                </div>
                {userIdError && (
                    <p className="field-error">{userIdError}</p>
                )}
            </div>

            <div>
                <div className="field-wrap">
                    <FaSchool/>
                    <input
                        type="text"
                        placeholder="שם בית הספר"
                        value={schoolName}
                        onChange={(e) => setSchoolName(e.target.value)}
                        className="field"
                    />
                </div>
            </div>

            <div>
                <div className="field-wrap">
                    <FaKey/>
                    <input
                        type="text"
                        placeholder="קוד בית הספר"
                        value={schoolCode}
                        onChange={(e) => setSchoolCode(e.target.value)}
                        className="field num"
                    />
                </div>
                {schoolCodeError && (
                    <p className="field-error">{schoolCodeError}</p>
                )}
            </div>

            <button
                onClick={assignSchoolManager}
                className="btn btn-primary btn-block"
            >
                הוסף
            </button>

            {message && (
                <p className="notice notice--mint">
                    {message}
                </p>
            )}
        </FormShell>
    );

}

export default SchoolRegistration;
