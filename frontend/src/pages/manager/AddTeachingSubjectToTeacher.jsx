import React, { useState, useContext } from 'react';
import axios from 'axios';
import Cookies from 'universal-cookie';
import FormShell from "../../components/ui/FormShell.jsx";
import {FiBookOpen, FiUserCheck} from "react-icons/fi";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";
import {
    ADD_TEACHING_SUBJECT_TO_TEACHER,
    AUTH_HEADER, QUESTION,
    SCHOOL_MANAGER_BASE_PATH, TEACHER_ID
} from "../../constants/pages.constants.js";

function AddTeachingSubjectToTeacher() {
    const [teacherId, setTeacherId] = useState('');
    const [message, setMessage] = useState('');
    const [subject, setSubject] = useState('מתמטיקה');
    const cookies = new Cookies();
    const token = cookies.get('token');

    //TODO לקבל את זה מהשרת
    const subjects = [
        "מתמטיקה"
    ];
    const handleSubmit = async () => {
        if (!teacherId.trim()) {
            setMessage("יש להזין מזהה מורה");
            return;
        }
        if (!subject.trim()) {
            setMessage("יש לבחור מקצוע");
            return;
        }

        try {
            const response = await axios.post(
            `${SCHOOL_MANAGER_BASE_PATH}${ADD_TEACHING_SUBJECT_TO_TEACHER}${QUESTION}${TEACHER_ID}${teacherId}`,
                [subject],
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    }
                }
            );

            if (response.data.success) {
                setMessage(response.data.errorCode);
            } else {
                setMessage(response.data.errorCode || "פעולה נכשלה");
            }

        } catch (error) {
            console.error("Error adding subject", error);
            setMessage("אירעה שגיאה בעת ביצוע הפעולה");
        }
    };

    return (
        <FormShell eyebrow="Teachers · מורים" title="הוסף מקצוע הוראה למורה" icon={<FiBookOpen />}>

            <div>
                <label className="label">
                    <FiUserCheck/>
                    מזהה מורה:
                </label>
                <input
                    type="text"
                    value={teacherId}
                    onChange={(e) => setTeacherId(e.target.value)}
                    placeholder="תז..."
                    className="field num"
                />
            </div>

            <div>
                <label className="label">
                    <FiBookOpen/>
                    בחר מקצוע:
                </label>
                <select
                    value={subject}
                    onChange={(e) => setSubject(e.target.value)}
                    className="field"
                >
                    <option value="">בחר מקצוע</option>
                    {subjects.map((s, index) => (
                        <option key={index} value={s}>
                            {s}
                        </option>
                    ))}
                </select>
            </div>

            <button
                onClick={handleSubmit}
                className="btn btn-primary btn-block"
            >
                <FiBookOpen/>
                הוסף מקצוע
            </button>

            {message && (
                <div className="notice">
                    {message}
                </div>
            )}
        </FormShell>
    );
}

export default AddTeachingSubjectToTeacher;
