import React, { useState, useContext } from 'react';
import axios from 'axios';
import Cookies from 'universal-cookie';
import FormShell from "../../components/ui/FormShell.jsx";
import {FiTrash2} from "react-icons/fi";
import {
    AND,
    ASSIGN_USER_AS_SCHOOL_TEACHER,
    AUTH_HEADER,
    QUESTION, REMOVE_TEACHER_FROM_SCHOOL, SCHOOL_CODE,
    SCHOOL_MANAGER_BASE_PATH, USER_ID
} from "../../constants/pages.constants.js";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";

function RemoveTeacherFromSchool() {
    const [teacherId, setTeacherId] = useState('');
    const [message, setMessage] = useState('');
    const cookies = new Cookies();
    const token = cookies.get('token');

    const handleRemove = async () => {
        if (!teacherId) {
            setMessage('אנא הזן מזהה מורה');
            return;
        }

        try {
            const response = await axios.post(
                `${SCHOOL_MANAGER_BASE_PATH}${REMOVE_TEACHER_FROM_SCHOOL}${QUESTION}${USER_ID}${teacherId}`,
                {},
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    }
                }
            );

            if (response.data.success) {
                setMessage(response.data.errorCode);
                setTeacherId('');
            } else {
                setMessage(response.data.errorCode || 'לא ניתן להסיר את המורה');
            }
        } catch (error) {
            console.error('שגיאה בהסרת מורה', error);
            setMessage('אירעה שגיאה');
        }
    };



    return (
        <FormShell eyebrow="Teachers · מורים" title="הסרת מורה" icon={<FiTrash2 />} tone="icon-badge--danger">

            <div>
                <label className="label">
                    <FiTrash2/>
                    הכנס מזהה מורה להסרה:
                </label>

                <input
                    type="text"
                    value={teacherId}
                    onChange={(e) => setTeacherId(e.target.value)}
                    placeholder="Teacher ID"
                    required
                    className="field num"
                />
            </div>

            <button
                onClick={handleRemove}
                className="btn btn-danger btn-block"
            >
                <FiTrash2/>
                הסר מורה
            </button>

            {message && (
                <div className="notice">
                    {message}
                </div>
            )}
        </FormShell>
    );

}

export default RemoveTeacherFromSchool;
