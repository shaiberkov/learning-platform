import React, {useContext, useEffect, useState} from 'react';
import axios from 'axios';
import Cookies from "universal-cookie";
import {UserContext} from "../../context/UserContext.jsx";
import FormShell from "../../components/ui/FormShell.jsx";
import {FiList} from "react-icons/fi";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";
import {
    ADD_SCHOOL_GRADES,
    AUTH_HEADER,
    QUESTION,
    SCHOOL_CODE,
    SCHOOL_MANAGER_BASE_PATH
} from "../../constants/pages.constants.js";

function AddSchoolGrades() {
    const [grades, setGrades] = useState('');
    const [message, setMessage] = useState('');
    const {user}=useContext(UserContext)
    const cookies = new Cookies();
    const token = cookies.get('token');



    const handleSubmit = async () => {
        if (!grades){
            setMessage("נא לירשום שכבות להוספה")
            return
        }

        const gradesList = grades.split(',').map(g => g.trim());

        try {
            const response = await axios.post(
                    `${SCHOOL_MANAGER_BASE_PATH}${ADD_SCHOOL_GRADES}${QUESTION}${SCHOOL_CODE}${user.schoolCode}`,
                gradesList,
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    }
                }
            );
            if (response.data.success){
                setMessage(response.data.errorCode)
                setGrades("")
            }else {
                setMessage(response.data.errorCode)
                setGrades("")
            }


        } catch (error) {
            setMessage("אירעה שגיאה")


        }
    };

    return (
        <FormShell eyebrow="Grades · שכבות" title="הוסף שכבות" icon={<FiList />}>

            <div>
                <label className="label">
                    <FiList/>
                    רשום רשימת שכבות מופרדות בפסיקים:
                </label>
                <input
                    type="text"
                    value={grades}
                    onChange={(e) => setGrades(e.target.value)}
                    placeholder="למשל: א, ב, ג"
                    required
                    className="field"
                />
            </div>

            <button
                onClick={handleSubmit}
                className="btn btn-primary btn-block"
            >
                <FiList/>
                הוסף שכבות
            </button>

            {message && (
                <div className="notice">
                    {message}
                </div>
            )}
        </FormShell>
    );
}

export default AddSchoolGrades;
