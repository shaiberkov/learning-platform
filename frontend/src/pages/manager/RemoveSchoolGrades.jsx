import React, { useContext, useEffect, useState } from 'react';
import axios from 'axios';
import Cookies from "universal-cookie";
import { UserContext } from "../../context/UserContext.jsx";
import FormShell from "../../components/ui/FormShell.jsx";
import {FiLayers} from "react-icons/fi";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";
import {
    AND,
    AUTH_HEADER, GET_GRADES,
    GET_TEACHER_DTO,
    QUESTION, REMOVE_SCHOOL_GRADES, SCHOOL_CODE,
    SCHOOL_MANAGER_BASE_PATH,
    TEACHER_ID
} from "../../constants/pages.constants.js";

function RemoveSchoolGrades() {
    const [gradesList, setGradesList] = useState([]);
    const [selectedGrade, setSelectedGrade] = useState('');
    const [message, setMessage] = useState('');
    const { user } = useContext(UserContext);
    const cookies = new Cookies();
    const token = cookies.get('token');

    useEffect(() => {
        const fetchGrades = async () => {
            if (user?.userId) {
                try {

                    const gradesResponse = await axios.get(
                        `${SCHOOL_MANAGER_BASE_PATH}${GET_GRADES}${QUESTION}${SCHOOL_CODE}${user.schoolCode}`,
                        {
                            headers: {
                                [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                            }
                        }
                    );

                    if (gradesResponse.data.success && Array.isArray(gradesResponse.data.data)) {
                        setGradesList(gradesResponse.data.data);
                    }
                } catch (err) {
                    console.error("Error fetching grades or school code", err);
                    setMessage("שגיאה בטעינת השכבות");
                }
            }
        };

        fetchGrades();
    }, [user?.userId]);

    const handleRemove = async () => {
        if (!selectedGrade) {
            setMessage("אנא בחר שכבה להסרה");
            return;
        }

        try {
            const response = await axios.post(
                `${SCHOOL_MANAGER_BASE_PATH}${REMOVE_SCHOOL_GRADES}${QUESTION}${SCHOOL_CODE}${user.schoolCode}`,
                [selectedGrade],
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    }
                }
            );

            if (response.data.success) {
                setMessage("השכבה הוסרה בהצלחה");
                setGradesList(gradesList.filter(g => g !== selectedGrade));
                setSelectedGrade('');
            } else {
                setMessage(response.data.errorCode || "שגיאה בהסרה");
            }
        } catch (error) {
            console.error("Error removing grade", error);
            setMessage("אירעה שגיאה בהסרת שכבה");
        }
    };


    return (
        <FormShell eyebrow="Grades · שכבות" title="הסר שכבה" icon={<FiLayers />} tone="icon-badge--danger">

            <div>
                <label className="label">
                    <FiLayers />
                    בחר שכבה להסרה:
                </label>

                <select
                    value={selectedGrade}
                    onChange={(e) => setSelectedGrade(e.target.value)}
                    className="field"
                >
                    <option value="">-- בחר שכבה --</option>
                    {gradesList.map((grade, index) => (
                        <option key={index} value={grade}>
                            {grade}
                        </option>
                    ))}
                </select>
            </div>

            <button
                onClick={handleRemove}
                className="btn btn-danger btn-block"
            >
                <FiLayers />
                הסר שכבה
            </button>

            {message && (
                <div className="notice">
                    {message}
                </div>
            )}
        </FormShell>
    );


}

export default RemoveSchoolGrades;
