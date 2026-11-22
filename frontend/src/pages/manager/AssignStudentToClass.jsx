import { useContext, useEffect, useState } from "react";
import axios from "axios";
import { UserContext } from "../../context/UserContext.jsx";
import Cookies from "universal-cookie";
import Select from "react-select";
import FormShell from "../../components/ui/FormShell.jsx";
import {FiBook, FiCheckCircle, FiUser} from "react-icons/fi";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";
import {
    AND, ASSIGN_STUDENT_TO_CLASS,
    ASSIGN_TEACHER_TO_CLASSES,
    AUTH_HEADER, CLASS_NAME, GET_ALL_CLASSES_BY_SCHOOL_CODE,
    QUESTION, SCHOOL_CODE,
    SCHOOL_MANAGER_BASE_PATH, STUDENT_ID, TEACHER_ID
} from "../../constants/pages.constants.js";

function AssignStudentToClass() {
    const cookies = new Cookies();
    const token = cookies.get('token');
    const [classesNames, setClassesNames] = useState([]);
    const [message, setMessage] = useState("");
    const [className, setClassName] = useState("");
    const [studentId, setStudentId] = useState("");

    const {user} = useContext(UserContext);

    useEffect(() => {
        const fetch = async () => {
            if (user?.userId) {
                try {
                    const classRes = await axios.get(
                        `${SCHOOL_MANAGER_BASE_PATH}${GET_ALL_CLASSES_BY_SCHOOL_CODE}${QUESTION}${SCHOOL_CODE}${user.schoolCode}`,
                        {
                            headers: {
                                [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                        }
                        }
                    );

                    if (classRes.data.success) {
                        setClassesNames(classRes.data.data);
                    }

                } catch (err) {
                    console.error(err);
                    setMessage("שגיאה בטעינת הנתונים");
                }
            }
        };

        fetch();
    }, [user?.userId]);

    const assignStudent = async () => {
        if (!studentId) {
            setMessage("אנא תרשום את התעודת זהות של התלמיד")
            return;
        }
        if (!className) {
            setMessage("אנא בחר כיתה לשיבוץ התלמיד")
            return;
        }
        try {
            const response = await axios.post(
                `${SCHOOL_MANAGER_BASE_PATH}${ASSIGN_STUDENT_TO_CLASS}${QUESTION}${SCHOOL_CODE}${user.schoolCode}${AND}${STUDENT_ID}${studentId}${AND}${CLASS_NAME}${className}`,
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    },
                }
            );

            if (response.data.success) {
                setMessage(response.data.errorCode);
            } else {
                setMessage(response.data.errorCode);
            }
        } catch (err) {
            console.error(err);
            setMessage("שגיאה בשיבוץ התלמיד");
        }
    }


    const classOptions = classesNames.map((cls) => ({
        label: cls,
        value: cls,
    }));


    return (

        <FormShell eyebrow="Students · תלמידים" title="שיבוץ תלמיד לכיתה" icon={<FiUser />} tone="icon-badge--mint">

            <div>
                <label className="label">
                    <FiUser/>
                    ת"ז תלמיד:
                </label>
                <input
                    type="text"
                    value={studentId}
                    onChange={(e) => setStudentId(e.target.value)}
                    placeholder="הכנס תז"
                    className="field num"
                />
            </div>

            <div>
                <label className="label">
                    <FiBook/>
                    בחר כיתה:
                </label>
                <Select
                    options={classOptions}
                    onChange={(selected) => setClassName(selected?.value || "")}
                    value={classOptions.find((opt) => opt.value === className)}
                    placeholder="בחר כיתה..."
                    isClearable
                    isSearchable
                    className="text-right z-50"
                    classNamePrefix="lp-select"
                    menuPortalTarget={typeof window !== "undefined" ? document.body : null}
                    menuPosition="fixed"
                    styles={{
                        control: (base) => ({
                            ...base,
                            direction: "rtl",
                            zIndex: 50,
                        }),
                        menu: (base) => ({
                            ...base,
                            zIndex: 9999,
                            direction: "rtl",
                        }),
                        menuPortal: (base) => ({
                            ...base,
                            zIndex: 9999,
                        }),
                        input: (base) => ({
                            ...base,
                            direction: "rtl",
                        }),
                        placeholder: (base) => ({
                            ...base,
                            textAlign: "right",
                        }),
                    }}
                />
            </div>

            <button
                onClick={assignStudent}
                className="btn btn-primary btn-block"
            >
                <FiCheckCircle/>
                שיבוץ תלמיד
            </button>

            {message && (
                <div className="notice">
                    {message}
                </div>
            )}
        </FormShell>
        )

}

export default AssignStudentToClass;
