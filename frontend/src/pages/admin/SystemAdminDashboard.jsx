
import {useContext, useEffect, useState} from "react";
import { useNavigate } from "react-router-dom";
import { UserContext } from "../../context/UserContext.jsx";
import axios from "axios";
import Cookies from "universal-cookie";
import MessageList from "../../components/messages/MessageList.jsx";
import {getGreeting} from "../../Utils/Greeting.jsx";
import {FaClipboardList, FaPlusCircle, FaSchool} from "react-icons/fa";
import EventForm from "../../components/EventForm.jsx";
import DashboardHero from "../../components/ui/DashboardHero.jsx";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";
import {
    AUTH_HEADER,
    GET_ALL_SCHOOLS,
    SYSTEM_ADMIN_BASE_PATH
} from "../../constants/pages.constants.js";



export default function SystemAdminDashboard() {
    const navigate = useNavigate();
    const [schoolData,setSchoolData]=useState([])
    const [messageFromServer, setMessageFromServer] = useState("");

    const { user } = useContext(UserContext);
    const cookies = new Cookies();
    const token = cookies.get('token');




    const getSchoolData = async () => {
        try {
            const response = await axios.get(
                `${SYSTEM_ADMIN_BASE_PATH}${GET_ALL_SCHOOLS}`,
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
            console.error("שגיאה בשליפת בתי ספר:", error);
            setMessageFromServer("אירעה שגיאה בחיבור לשרת");
        }
    };

    useEffect(() => {
       if (user) getSchoolData();
    }, [user?.userId]);



    const handleRegisterSchool = () => {
        navigate("/school-registration");
    };

    return (
        <div className="page" dir="rtl">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 sm:gap-5">
                <DashboardHero
                    className="lg:col-span-8"
                    eyebrow="System admin · לוח ניהול מערכת"
                    greeting={getGreeting()}
                    name={user.username}
                    subtitle="לוח ניהול מערכת"
                />

                <div className="lg:col-span-4 grid grid-cols-2 lg:grid-cols-1 gap-4 sm:gap-5">
                    <div className="glass tile p-6 flex flex-col justify-between gap-4 reveal" style={{ "--i": 1 }}>
                        <span className="icon-badge icon-badge--mint"><FaSchool /></span>
                        <div>
                            <p className="faint text-xs">מספר בתי ספר רשומים</p>
                            {schoolData.length>0&& (
                                <p className="num text-5xl font-semibold text-ink mt-1">{schoolData.length}</p>
                            )}
                        </div>
                    </div>

                    <div className="glass tile p-6 flex flex-col justify-between gap-4 reveal overflow-hidden" style={{ "--i": 2 }}>
                        <div className="absolute inset-0 -z-10 bg-[radial-gradient(circle_at_20%_10%,var(--accent-soft),transparent_70%)]" />
                        <div className="flex items-center gap-3">
                            <span className="icon-badge"><FaPlusCircle /></span>
                            <p className="title text-base">רישום בית ספר חדש</p>
                        </div>
                        <button
                            onClick={handleRegisterSchool}
                            className="btn btn-primary btn-sm btn-block"
                        >
                            הוסף בית ספר
                        </button>
                    </div>
                </div>

                <section className="lg:col-span-7 glass p-6 sm:p-7 reveal" style={{ "--i": 3 }}>
                    <div className="panel-head">
                        <div className="flex items-center gap-3">
                            <span className="icon-badge"><FaClipboardList /></span>
                            <h3 className="title text-xl">רשימת בתי ספר</h3>
                        </div>
                        <span className="chip num">{schoolData.length}</span>
                    </div>

                    {schoolData.length > 0 ? (
                        <ul className="grid sm:grid-cols-2 gap-3 max-h-[26rem] scroll-area p-1">
                            {schoolData.map((school, idx) => (
                                <li
                                    key={idx}
                                    className="row-card flex justify-between items-center gap-3 reveal"
                                    style={{ "--i": Math.min(idx, 8) }}
                                >
                                    <span className="font-semibold text-ink truncate">{school.schoolName}</span>
                                    <span className="chip num">{school.schoolCode}</span>
                                </li>
                            ))}
                        </ul>
                    ) : (
                        <p className="faint text-center py-6">{messageFromServer}</p>
                    )}
                </section>

                {user && (
                    <div className="lg:col-span-5 flex flex-col gap-4 sm:gap-5 reveal" style={{ "--i": 4 }}>
                        <MessageList userId={user.userId} />
                        <EventForm />
                    </div>
                )}

                {/*{user && (*/}
                {/*    <div className="lg:col-span-1 ">*/}
                {/*        <MessageList userId={user.userId} />*/}
                {/*    </div>*/}
                {/*)}*/}
                {/*{user && <EventForm />}*/}
            </div>
        </div>
    );

}
