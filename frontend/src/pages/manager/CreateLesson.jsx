import {useContext, useEffect, useState} from "react";
import axios from "axios";
import Cookies from "universal-cookie";
import {UserContext} from "../../context/UserContext.jsx";
import FormShell from "../../components/ui/FormShell.jsx";
import {FiBookOpen, FiCalendar, FiClock, FiPlusCircle, FiSearch, FiUserCheck} from "react-icons/fi";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";
import {
    ADD_LESSON_TO_TEACHER,
    AND,
    ASSIGN_USER_AS_SCHOOL_TEACHER,
    AUTH_HEADER, GET_TEACHER_DTO,
    QUESTION, SCHOOL_CODE,
    SCHOOL_MANAGER_BASE_PATH, TEACHER_ID, USER_ID
} from "../../constants/pages.constants.js";

function CreateLesson() {
    const [formData, setFormData] = useState({
        subject: "",
        dayOfWeek: "",
        startTime: "",
        endTime: "",
        classRoomName: ""
    });
    const [teacherId,setTeacherId]=useState("")
    const [teacher, setTeacher] = useState(null);
    const [submitMessage, setSubmitMessage] = useState("");
    const [errorMessage, setErrorMessage] = useState("");
    const [loadingTeacher, setLoadingTeacher] = useState(false);
    const { user } = useContext(UserContext);

    const cookies = new Cookies();
    const token = cookies.get('token');

    const daysOfWeek = [
        { value: "SUNDAY", label: "ראשון" },
        { value: "MONDAY", label: "שני" },
        { value: "TUESDAY", label: "שלישי" },
        { value: "WEDNESDAY", label: "רביעי" },
        { value: "THURSDAY", label: "חמישי" },
        { value: "FRIDAY", label: "שישי" }
    ];

    const generateTimeOptions = () => {
        const times = [];
        for (let hour = 8; hour <= 15; hour++) {
            times.push(`${String(hour).padStart(2, '0')}:00`);
            if (hour < 15) {
                times.push(`${String(hour).padStart(2, '0')}:30`);
            }
        }
        return times;
    };

    const timeOptions = generateTimeOptions();

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: value
        }));

    };


    const fetchTeacher = async () => {
        if (!teacherId) {
            setErrorMessage("יש להזין תעודת זהות של מורה");
            return;
        }

        setLoadingTeacher(true);
        setErrorMessage("");
        try {
            const response = await axios.get(
                `${SCHOOL_MANAGER_BASE_PATH}${GET_TEACHER_DTO}${QUESTION}${TEACHER_ID}${teacherId}${AND}${SCHOOL_CODE}${user.schoolCode}`,
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    }
                }
            );
            if(response.data.success){
               setTeacher(response.data.data);
                setErrorMessage(response.data.errorCode)
            }else{

            }

        } catch (error) {
            console.error("Error fetching teacher:", error);
            setTeacher(null);
            setErrorMessage("שגיאה בטעינת פרטי המורה");
        } finally {
            setLoadingTeacher(false);
        }
    };

    const handleSubmit = async () => {

        const { subject, dayOfWeek, startTime, endTime, classRoomName} = formData;
        if (!subject || !dayOfWeek || !startTime || !endTime || !classRoomName ) {
            setSubmitMessage("יש למלא את כל השדות");
             return;
        }

        if (endTime <= startTime) {
            setSubmitMessage("שעת סיום חייבת להיות לאחר שעת ההתחלה");
            return;
        }

        try {
            const response = await axios.post(
                `${SCHOOL_MANAGER_BASE_PATH}${ADD_LESSON_TO_TEACHER}${QUESTION}${TEACHER_ID}${teacherId}`,
                formData,
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    },
                }
            );

            if(response.data.success){
                setSubmitMessage(response.data.errorCode);


            }
            else {
                setSubmitMessage(response.data.errorCode);
            }
        } catch (error) {
            console.error("Failed to add lesson:", error);
            setSubmitMessage("אירעה שגיאה בעת ניסיון הוספת השיעור");
        }
    };

    return (

            <FormShell eyebrow="Timetable · שיבוץ" title="יצירת שיעור חדש" icon={<FiPlusCircle />} width="medium">

                <div>
                    <label className="label">
                        <FiUserCheck />
                        תעודת זהות מורה:
                    </label>
                    <div className="flex flex-col sm:flex-row gap-3">
                        <input
                            type="text"
                            name="teacherId"
                            value={teacherId}
                            onChange={(e) => setTeacherId(e.target.value)}
                            required
                            className="field num"
                        />
                        <button
                            onClick={fetchTeacher}
                            className="btn btn-ghost shrink-0"
                        >
                            <FiSearch />
                            בדוק מורה
                        </button>
                    </div>
                </div>

                {loadingTeacher && <p className="faint text-sm text-center flex items-center justify-center gap-3"><span className="dot-pulse"><span /><span /><span /></span>טוען נתוני מורה...</p>}
                {errorMessage && <p className="notice">{errorMessage}</p>}

                {teacher && (
                    <div className="space-y-5 reveal">
                        <p className="glass--sunk px-4 py-3 flex items-center justify-between gap-3">
                            <span className="faint text-sm">שם המורה:</span>
                            <span className="title text-base">{teacher.teacherName}</span>
                        </p>

                        <div className="grid sm:grid-cols-2 gap-4">
                            {/* מקצוע */}
                            <div>
                                <label className="label">
                                    <FiBookOpen />
                                    מקצוע:
                                </label>
                                <select
                                    name="subject"
                                    value={formData.subject}
                                    onChange={handleChange}
                                    required
                                    className="field"
                                >
                                    <option value="">בחר מקצוע</option>
                                    {teacher.subjects.map((subj, idx) => (
                                        <option key={idx} value={subj}>{subj}</option>
                                    ))}
                                </select>
                            </div>

                            <div>
                                <label className="label">
                                    <FiUserCheck />
                                    כיתה:
                                </label>
                                <select
                                    name="classRoomName"
                                    value={formData.classRoomName}
                                    onChange={handleChange}
                                    required
                                    className="field"
                                >
                                    <option value="">בחר כיתה</option>
                                    {teacher.classes.map((cls, idx) => (
                                        <option key={idx} value={cls}>{cls}</option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <div className="grid sm:grid-cols-3 gap-4">
                            <div>
                                <label className="label">
                                    <FiCalendar />
                                    יום בשבוע:
                                </label>
                                <select
                                    name="dayOfWeek"
                                    value={formData.dayOfWeek}
                                    onChange={handleChange}
                                    required
                                    className="field"
                                >
                                    <option value="">בחר יום</option>
                                    {daysOfWeek.map((day) => (
                                        <option key={day.value} value={day.value}>
                                            {day.label}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div>
                                <label className="label">
                                    <FiClock />
                                    שעת התחלה:
                                </label>
                                <select
                                    name="startTime"
                                    value={formData.startTime}
                                    onChange={handleChange}
                                    required
                                    className="field num"
                                >
                                    <option value="">בחר</option>
                                    {timeOptions.map((time, i) => (
                                        <option key={`start-${i}`} value={time}>{time}</option>
                                    ))}
                                </select>
                            </div>

                            <div>
                                <label className="label">
                                    <FiClock />
                                    שעת סיום:
                                </label>
                                <select
                                    name="endTime"
                                    value={formData.endTime}
                                    onChange={handleChange}
                                    required
                                    className="field num"
                                >
                                    <option value="">בחר</option>
                                    {timeOptions.map((time, i) => (
                                        <option key={`end-${i}`} value={time}>{time}</option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <button
                            onClick={handleSubmit}
                            className="btn btn-primary btn-block"
                        >
                            <FiPlusCircle />
                            צור שיעור
                        </button>
                    </div>
                )}

                {submitMessage && (
                    <p className="notice">{submitMessage}</p>
                )}
            </FormShell>
    );
}

export default CreateLesson;
