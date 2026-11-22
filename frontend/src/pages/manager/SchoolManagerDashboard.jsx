
import {useContext, useEffect, useState} from "react";
import { UserContext } from "../../context/UserContext.jsx";
import MessageList from "../../components/messages/MessageList.jsx";
import Cookies from "universal-cookie";
import axios from "axios";
import {getGreeting} from "../../Utils/Greeting.jsx";
import EventForm from "../../components/EventForm.jsx";
import { FaSchool } from "react-icons/fa";
import DashboardHero from "../../components/ui/DashboardHero.jsx";
import {
    AUTH_HEADER, GET_SCHOOL,
    QUESTION,
    SCHOOL_MANAGER_BASE_PATH, SCHOOL_MANAGER_ID, TEACHER_ID
} from "../../constants/pages.constants.js";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";

function SchoolManagerDashboard() {
    const [schoolData,setSchoolData]=useState({})
    const [messageFromServer, setMessageFromServer] = useState("");
    const { user } = useContext(UserContext);
    const cookies = new Cookies();
    const token = cookies.get('token');



    useEffect(() => {
        const fetchSchoolData = async () => {
            try {
                const response = await axios.get(
                    `${SCHOOL_MANAGER_BASE_PATH}${GET_SCHOOL}${QUESTION}${SCHOOL_MANAGER_ID}${user.userId}`,
                    {
                        headers: {
                            [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                        },
                    }
                );

                if (response.data.success) {
                    if (response.data.data) {
                        setSchoolData(response.data.data);
                    }
                } else {
                    setMessageFromServer(response.data.errorCode);
                }
            } catch (error) {
                console.error("שגיאה:", error);
                setMessageFromServer("שגיאה בחיבור לשרת");
            }
        };

       if(user) fetchSchoolData();
    }, [user?.userId]);


    return (
        <div className="page" dir="rtl">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 sm:gap-5 items-stretch">
                <DashboardHero
                    className="lg:col-span-8"
                    eyebrow="School manager · לוח ניהול"
                    greeting={getGreeting()}
                    name={user?.username}
                    subtitle="לוח ניהול בית הספר"
                />

                {/* פרטי בית הספר */}
                <div className="lg:col-span-4 reveal" style={{ "--i": 1 }}>
                    {schoolData ? (
                        <div className="glass tile p-6 h-full flex flex-col gap-5">
                            <div className="panel-head !mb-0">
                                <div className="flex items-center gap-3">
                                    <span className="icon-badge"><FaSchool /></span>
                                    <div>
                                        <span className="eyebrow">בית הספר</span>
                                        <p className="title text-xl mt-0.5">{schoolData.schoolName}</p>
                                    </div>
                                </div>
                                <span className="chip num">קוד: {schoolData.schoolCode}</span>
                            </div>

                            <div className="grid grid-cols-2 gap-3 flex-1">
                                <div className="glass--sunk p-4 flex flex-col justify-between">
                                    <span className="faint text-xs">מספר מורים</span>
                                    {schoolData.teacherCount && schoolData.teacherCount > 0
                                        ? <span className="num text-4xl font-semibold text-ink mt-3">{schoolData.teacherCount}</span>
                                        : <span className="faint text-sm mt-3">אין כרגע מורים רשומים</span>}
                                </div>
                                <div className="glass--sunk p-4 flex flex-col justify-between">
                                    <span className="faint text-xs">מספר תלמידים</span>
                                    {schoolData.studentCount && schoolData.studentCount > 0
                                        ? <span className="num text-4xl font-semibold text-mint mt-3">{schoolData.studentCount}</span>
                                        : <span className="faint text-sm mt-3">אין כרגע תלמידים רשומים</span>}
                                </div>
                            </div>
                        </div>
                    ) : (
                        <p className="notice">{messageFromServer}</p>
                    )}
                </div>

                {/* אירועים קרובים */}
                {user && (
                    <div className="lg:col-span-5 reveal" style={{ "--i": 2 }}>
                        <EventForm/>
                    </div>
                )}

                {/* הודעות */}
                {user && (
                    <div className="lg:col-span-7 reveal" style={{ "--i": 3 }}>
                        <MessageList userId={user.userId}/>
                    </div>
                )}
            </div>
        </div>

    );


}

export default SchoolManagerDashboard;
