import { useContext, useEffect, useState } from "react";
import { UserContext } from "../context/UserContext.jsx";
import Cookies from "universal-cookie";
import axios from "axios";
import {FaCalendarAlt} from "react-icons/fa";
import {
    AND,
    AUTH_HEADER,
    GET_UPCOMING_EVENTS,
    QUESTION, ROLE, UPCOMING_EVENTS_BASE_PATH,
    USER_ID
} from "../constants/pages.constants.js";
import {BEARER_PREFIX} from "../constants/shared.constant.js";
function EventForm() {
    const { user } = useContext(UserContext);
    const cookies = new Cookies();
    const token = cookies.get('token');

    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!user ) return;

        const fetchEvents = async () => {

            const url =`${UPCOMING_EVENTS_BASE_PATH}${GET_UPCOMING_EVENTS}${QUESTION}${ROLE}${user.role}${AND}${USER_ID}${user.userId}`
            try {
                const response = await axios.get(url, {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`,
                        Accept: "application/json",
                    },
                });
                setEvents(response.data);
            } catch (err) {
                setError(err.response?.data?.message || "שגיאה בבקשת האירועים");
            } finally {
                setLoading(false);
            }
        };

        fetchEvents();
    }, [user?.userId]);



    return (
    <div
        className="glass tile w-full h-full min-h-[17rem] p-5 sm:p-6 flex flex-col"
        dir="rtl"
    >
        <div className="panel-head">
            <div className="flex items-center gap-3">
                <span className="icon-badge icon-badge--amber"><FaCalendarAlt /></span>
                <div>
                    <span className="eyebrow">Upcoming</span>
                    <h2 className="title text-xl mt-0.5">האירועים הקרובים שלך</h2>
                </div>
            </div>
        </div>

        <div className="flex-1 max-h-72 scroll-area -mx-1 px-1 py-1">
            {events.length === 0 ? (
                <p className="faint text-center py-8">אין אירועים קרובים</p>
            ) : (
                <ul className="space-y-2.5">
                    {events.map((event, index) => (
                        <li
                            key={index}
                            className="row-card flex items-center justify-between gap-3 reveal"
                            style={{ "--i": Math.min(index, 6) }}
                        >
                            <div className="flex items-center gap-3 min-w-0">
                                <span className="w-2 h-2 rotate-45 rounded-[2px] bg-amber shadow-[0_0_10px_var(--amber)] shrink-0" />
                                <span className="font-semibold text-ink truncate">
                                    {event.eventName}
                                </span>
                            </div>
                            <span className="chip chip--amber num">בעוד {event.eventInDays} ימים </span>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    </div>

);
}

export default EventForm;
