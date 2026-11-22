import React, {useContext, useEffect, useState} from 'react';
import axios from 'axios';
import Cookies from "universal-cookie";
import {UserContext} from "../../context/UserContext.jsx";
import FormShell from "../../components/ui/FormShell.jsx";
import {FaIdCard} from "react-icons/fa";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";
import {
    AND,
    ASSIGN_STUDENT_TO_CLASS, ASSIGN_USER_AS_SCHOOL_TEACHER,
    AUTH_HEADER, CLASS_NAME,
    QUESTION, SCHOOL_CODE,
    SCHOOL_MANAGER_BASE_PATH, STUDENT_ID, USER_ID
} from "../../constants/pages.constants.js";

function AssignTeacher() {
    const [userId, setUserId] = useState('');
    const [message, setMessage] = useState('');
    const {user} = useContext(UserContext)

    const cookies = new Cookies();
    const token = cookies.get('token');


    const assignUserAsTeacher = async () => {

        try {
            const response = await axios.post(
                `${SCHOOL_MANAGER_BASE_PATH}${ASSIGN_USER_AS_SCHOOL_TEACHER}${QUESTION}${USER_ID}${userId}${AND}${SCHOOL_CODE}${user.schoolCode}`,
                {}, {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    }
                }
            );

            if (!response.data.success) {
                setMessage(response.data.errorCode);
            } else {
                setMessage(response.data.errorCode);
                setUserId("")
            }

        } catch (error) {
            console.error('Request failed:', error);
            setMessage('שגיאה בבקשת ההפיכה למורה');
        }
    };


    return (

        <FormShell eyebrow="Teachers · מורים" title="שיוך מורה לבית ספר" icon={<FaIdCard />} tone="icon-badge--mint">
                <div>
                    <label className="label">
                        <FaIdCard/>
                        הכנס מזהה מורה:
                    </label>
                    <input
                        type="text"
                        placeholder="תעודת זהות"
                        value={userId}
                        onChange={(e) => setUserId(e.target.value)}
                        required
                        className="field num"
                    />
                </div>

                <button
                    onClick={assignUserAsTeacher}
                    className="btn btn-primary btn-block"
                >
                    שייך מורה
                </button>

                {message && (
                    <p
                        className={`notice ${
                            message.includes('שגיאה') ? 'notice--danger' : 'notice--mint'
                        }`}
                    >
                        {message}
                    </p>
                )}
        </FormShell>
    )
}

export default AssignTeacher;
