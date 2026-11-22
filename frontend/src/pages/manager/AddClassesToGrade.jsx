import React, { useEffect, useState, useContext } from 'react';
import axios from 'axios';
import Cookies from 'universal-cookie';
import { UserContext } from '../../context/UserContext.jsx';
import FormShell from "../../components/ui/FormShell.jsx";
import {FiLayers, FiPlusSquare} from "react-icons/fi";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";
import {
    ADD_CLASSES_TO_GRADE, AND,
    AUTH_HEADER, CLASSES_COUNT,
    GET_GRADES, GRADE_NAME,
    QUESTION,
    SCHOOL_CODE,
    SCHOOL_MANAGER_BASE_PATH
} from "../../constants/pages.constants.js";

function AddClassesToGrade() {
    const [gradesList, setGradesList] = useState([]);
    const [selectedGrade, setSelectedGrade] = useState('');
    const [classesCount, setClassesCount] = useState('');
    const [message, setMessage] = useState('');

    const {user} = useContext(UserContext);
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

    const handleSubmit = async () => {
        if (!selectedGrade) {
            setMessage("נא לבחור שכבה");
            return;
        }
        if (!classesCount) {
            setMessage("נא להזין מספר כיתות");
            return;
        }

        try {
            const response = await axios.post(
                `${SCHOOL_MANAGER_BASE_PATH}${ADD_CLASSES_TO_GRADE}${QUESTION}${SCHOOL_CODE}${user.schoolCode}${AND}${GRADE_NAME}${selectedGrade}${AND}${CLASSES_COUNT}${classesCount}`,
                {},
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    }
                }
            );

            if (response.data.success) {
                setMessage(response.data.errorCode);
                setClassesCount('');
            } else {
                setMessage(response.data.errorCode || "לא ניתן להוסיף כיתות");
            }
        } catch (err) {
            console.error("Error adding classes", err);
            setMessage("אירעה שגיאה");
        }
    };

    return (
        <FormShell eyebrow="Grades · שכבות" title="הוסף כיתות לשכבה" icon={<FiPlusSquare />}>

            <div>
                <label className="label">
                    <FiLayers/>
                    בחר שכבה:
                </label>
                <select
                    value={selectedGrade}
                    onChange={(e) => setSelectedGrade(e.target.value)}
                    className="field"
                >
                    <option value="">בחר שכבה</option>
                    {gradesList.map((grade, index) => (
                        <option key={index} value={grade}>
                            {grade}
                        </option>
                    ))}
                </select>
            </div>

            <div>
                <label className="label">
                    <FiPlusSquare/>
                    מספר כיתות להוספה:
                </label>
                <input
                    type="number"
                    value={classesCount}
                    onChange={(e) => setClassesCount(e.target.value)}
                    min="1"
                    className="field num"
                />
            </div>

            <button
                onClick={handleSubmit}
                className="btn btn-primary btn-block"
            >
                <FiPlusSquare/>
                הוסף כיתות
            </button>

            {message && (
                <div className="notice">
                    {message}
                </div>
            )}
        </FormShell>
    )
}

export default AddClassesToGrade;
