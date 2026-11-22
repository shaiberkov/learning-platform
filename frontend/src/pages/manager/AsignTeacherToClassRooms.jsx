

import { useContext, useEffect, useState } from "react";
import Cookies from "universal-cookie";
import { UserContext } from "../../context/UserContext.jsx";
import axios from "axios";
import Select from "react-select";
import FormShell from "../../components/ui/FormShell.jsx";
import {FiUserCheck, FiUsers} from "react-icons/fi";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";
import {
    AND,
    ASSIGN_TEACHER_TO_CLASSES,
    AUTH_HEADER,
    GET_ALL_CLASSES_BY_SCHOOL_CODE, QUESTION, SCHOOL_CODE,
    SCHOOL_MANAGER_BASE_PATH, TEACHER_ID
} from "../../constants/pages.constants.js";

function AssignTeacherToClassRooms() {
    const [classesName, setClassesName] = useState([]);
    const [teacherId, setTeacherId] = useState('');
    const [selectedClasses, setSelectedClasses] = useState([]);
    const [message, setMessage] = useState('');
    const { user } = useContext(UserContext);
    const cookies = new Cookies();
    const token = cookies.get('token');

    useEffect(() => {
        const fetchClasses = async () => {
            if (user?.userId) {
                try {

                    const response = await axios.get(
                         `${SCHOOL_MANAGER_BASE_PATH}${GET_ALL_CLASSES_BY_SCHOOL_CODE}${QUESTION}${SCHOOL_CODE}${user.schoolCode}`,
                        {
                            headers: {
                                [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                            }
                        }
                    );

                    if (response.data.success) {
                        console.log(response.data)
                        setClassesName(response.data.data);
                    } else {
                        setMessage(response.data.errorCode);
                    }
                } catch (error) {
                    console.error("Error fetching data:", error);
                    setMessage("שגיאה בעת טעינת נתונים.");
                }
            }
        };

        fetchClasses();
    }, [user?.userId]);

    const handleAssign = async () => {

        if (!teacherId || selectedClasses.length === 0) {
            setMessage("נא למלא את כל השדות");
            return;
        }

        try {
            const response = await axios.post(
                `${SCHOOL_MANAGER_BASE_PATH}${ASSIGN_TEACHER_TO_CLASSES}${QUESTION}${SCHOOL_CODE}${user.schoolCode}${AND}${TEACHER_ID}${teacherId}`,
                selectedClasses,
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`,
                    }
                }
            );

            if (response.data.success) {
                setMessage(response.data.errorCode);
                setTeacherId("");
                setSelectedClasses([]);
            } else {
                setMessage(response.data.errorCode);
            }
        } catch (error) {
            console.error("Assign error:", error);
            setMessage("שגיאה בעת ניסיון השיבוץ.");
        }
    };

    const classOptions = classesName.map(name => ({ label: name, value: name }));

    return (
        <FormShell eyebrow="Teachers · מורים" title="שיבוץ מורה לכיתה" icon={<FiUsers />}>

            {/* מזהה מורה */}
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
                    required
                />
            </div>

            {/* בחירת כיתות */}
            <div className="z-[999]"
            >
                <label className="label">
                    <FiUsers/>
                    בחר כיתות:
                </label>
                <Select
                    isMulti
                    options={classOptions}
                    value={classOptions.filter(option => selectedClasses.includes(option.value))}
                    onChange={(selectedOptions) =>
                        setSelectedClasses(selectedOptions.map(option => option.value))
                    }
                    placeholder="בחר כיתות..."
                    noOptionsMessage={() => "לא נמצאו כיתות מתאימות"}
                    className="react-select-container"
                    classNamePrefix="lp-select"
                    menuPortalTarget={document.body}
                    styles={{
                        control: (base) => ({
                            ...base,
                            direction: "rtl", // תיבת הבחירה הראשית
                        }),
                        menu: (base) => ({
                            ...base,
                            direction: "rtl", // תפריט האופציות
                            textAlign: "right",
                        }),
                        option: (base) => ({
                            ...base,
                            direction: "rtl", // כל אופציה בנפרד
                            textAlign: "right",
                        }),
                        multiValueLabel: (base) => ({
                            ...base,
                            direction: "rtl",
                        }),
                        menuPortal: (base) => ({ ...base, zIndex: 9999 })
                    }}
                />

            </div>

            {/* כפתור */}
            <button
                onClick={handleAssign}
                className="btn btn-primary btn-block"
            >
                <FiUsers/>
                שבץ מורה
            </button>

            {/* הודעה */}
            {message && (
                <div className="notice">
                    {message}
                </div>
            )}
        </FormShell>

    );
}

export default AssignTeacherToClassRooms;
