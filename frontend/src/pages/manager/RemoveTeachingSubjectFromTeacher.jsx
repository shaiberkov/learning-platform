import React, { useState } from 'react';
import axios from 'axios';
import Cookies from 'universal-cookie';
import FormShell from "../../components/ui/FormShell.jsx";
import {FiBookOpen, FiSearch, FiTrash2} from "react-icons/fi";
import {
    AND,
    AUTH_HEADER, GET_TEACHER_TEACHING_SUBJECTS,
    QUESTION,
    REMOVE_TEACHER_FROM_SCHOOL, REMOVE_TEACHING_SUBJECT_FROM_TEACHER,
    SCHOOL_MANAGER_BASE_PATH, SUBJECT_TO_REMOVE, TEACHER_ID, USER_ID
} from "../../constants/pages.constants.js";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";

function RemoveTeachingSubjectFromTeacher() {
    const [teacherId, setTeacherId] = useState('');
    const [subjects, setSubjects] = useState([]);
    const [selectedSubject, setSelectedSubject] = useState('');
    const [message, setMessage] = useState('');
    const [subjectsFetched, setSubjectsFetched] = useState(false);
    const cookies = new Cookies();
    const token = cookies.get('token');

    const fetchSubjects = async () => {
        if (!teacherId.trim()) {
            setMessage("יש להזין מזהה מורה");
            return;
        }

        try {
            const response = await axios.get(
                `${SCHOOL_MANAGER_BASE_PATH}${GET_TEACHER_TEACHING_SUBJECTS}${QUESTION}${TEACHER_ID}${teacherId}`,
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    }
                }
            );

            if (response.data.success){
                if (Array.isArray(response.data.data)) {
                    setSubjects(response.data.data);
                    setSelectedSubject('');
                    setMessage('');
                    setSubjectsFetched(true);
                } else {
                    setSubjects([]);
                    setSubjectsFetched(false);
                    setTeacherId('');
                }

            }else {
                setMessage(response.data.errorCode)
                setTeacherId('');
            }


        } catch (error) {
            console.error("Error fetching subjects:", error);
            setMessage("שגיאה בקבלת מקצועות ההוראה");
        }
    };

    const handleRemove = async () => {
        if (!selectedSubject) {
            setMessage("יש לבחור מקצוע להסרה");
            return;
        }

        try {
            const response = await axios.post(
                `${SCHOOL_MANAGER_BASE_PATH}${REMOVE_TEACHING_SUBJECT_FROM_TEACHER}${QUESTION}${TEACHER_ID}${teacherId}${AND}${SUBJECT_TO_REMOVE}${selectedSubject}`,
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    }
                }
            );


            if (response.data.success) {
                setSubjects(subjects.filter(subject => subject !== selectedSubject));
                setSelectedSubject('');
                setMessage(response.data.errorCode);
            } else {
                setMessage(response.data.errorCode || "הסרת המקצוע נכשלה");
            }

        } catch (error) {
            console.error("Error removing subject:", error);
            setMessage("שגיאה בהסרת המקצוע");
        }
    };

    return (

            <FormShell eyebrow="Teachers · מורים" title="הסר מקצוע הוראה ממורה" icon={<FiTrash2 />} tone="icon-badge--danger">

                <div>
                    <label className="label">
                        <FiSearch />
                        מזהה מורה:
                    </label>
                    <div className="flex flex-col sm:flex-row gap-3">
                        <input
                            type="text"
                            value={teacherId}
                            onChange={(e) => setTeacherId(e.target.value)}
                            placeholder="לדוג': abc12345"
                            className="field num"
                        />
                        <button
                            onClick={fetchSubjects}
                            className="btn btn-ghost shrink-0"
                        >
                            <FiSearch />
                            הבא מקצועות הוראה
                        </button>
                    </div>
                </div>

                {subjectsFetched && subjects.length > 0 && (
                    <div className="space-y-4 reveal">
                        <div>
                            <label className="label">
                                <FiBookOpen />
                                בחר מקצוע להסרה:
                            </label>
                            <select
                                value={selectedSubject}
                                onChange={(e) => setSelectedSubject(e.target.value)}
                                className="field"
                            >
                                <option value="">בחר מקצוע</option>
                                {subjects.map((subject, index) => (
                                    <option key={index} value={subject}>
                                        {subject}
                                    </option>
                                ))}
                            </select>
                        </div>
                        <button
                            onClick={handleRemove}
                            className="btn btn-danger btn-block"
                        >
                            <FiTrash2 />
                            הסר מקצוע
                        </button>
                    </div>
                )}

                {message && (
                    <p className="notice">
                        {message}
                    </p>
                )}
            </FormShell>
        );

}

export default RemoveTeachingSubjectFromTeacher;
