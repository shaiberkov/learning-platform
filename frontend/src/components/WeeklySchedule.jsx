
import React, { useContext, useEffect, useState, useMemo } from 'react';
import axios from 'axios';
import Cookies from 'universal-cookie';
import { UserContext } from "../context/UserContext.jsx";
import clsx from "clsx";
import {
    AND,
    AUTH_HEADER,
    CHECK_PRACTICE_TEST, CLASS_ROOM_NAME, GET_SCHEDULE_FOR_CLASSROOM, GET_STUDENT_SCHEDULE,
    QUESTION, SCHEDULE_FOR_TEACHER, SCHOOL_CODE, SCHOOL_MANAGER_BASE_PATH,
    STUDENT_BASE_PATH, STUDENT_ID, TEACHER_BASE_PATH, TEACHER_ID, TEST_ID,
    USER_ID
} from "../constants/pages.constants.js";
import {BEARER_PREFIX} from "../constants/shared.constant.js";
function WeeklySchedule({ type, classRoomName, singleDayMode = false }) {
    const [lessonsByDay, setLessonsByDay] = useState([]);
    const cookies = new Cookies();
    const token = cookies.get('token');
    const {user} = useContext(UserContext);

    useEffect(() => {
        const fetchSchedule = async () => {
            if (!user?.userId) return;

            try {
                let url = '';
                if (type === 'teacher') {
                    url = `${TEACHER_BASE_PATH}${SCHEDULE_FOR_TEACHER}${QUESTION}${TEACHER_ID}${user.userId}`;
                } else if (type === 'class') {
                    url =`${SCHOOL_MANAGER_BASE_PATH}${GET_SCHEDULE_FOR_CLASSROOM}${QUESTION}${SCHOOL_CODE}${user.schoolCode}${AND}${CLASS_ROOM_NAME}${classRoomName}`;
                } else if (type === 'student') {
                    url =`${STUDENT_BASE_PATH}${GET_STUDENT_SCHEDULE}${QUESTION}${SCHOOL_CODE}${user.schoolCode}${AND}${STUDENT_ID}${user.userId}`;
                }
                const response = await axios.get(url, {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`

                    },
                });

                if (response.data.success) {
                    setLessonsByDay(response.data.data);
                } else {
                    setLessonsByDay([]);
                    console.warn("בעיה בהבאת נתונים:", response.data);
                }
            } catch (error) {
                console.error("שגיאה בקריאת מערכת השעות:", error);
            }
        };

        fetchSchedule();
    }, [user?.userId, type, classRoomName, token]);

    const daysInHebrew = {
        SUNDAY: 'ראשון',
        MONDAY: 'שני',
        TUESDAY: 'שלישי',
        WEDNESDAY: 'רביעי',
        THURSDAY: 'חמישי',
        FRIDAY: 'שישי',
        SATURDAY: 'שבת'
    };

    const getTodayKey = () => {
        const dayNumber = new Date().getDay();
        const dayMap = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
        return dayMap[dayNumber];
    };

    const displayedDays = useMemo(() => {
        return singleDayMode ? [getTodayKey()] : Object.keys(daysInHebrew);
    }, [singleDayMode]);

    const timeSlots = [
        "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
        "12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00"
    ];

    const calculateRowSpan = (startTime, endTime) => {
        const toMinutes = (t) => {
            const [h, m] = t.split(":").map(Number);
            return h * 60 + m;
        };
        const diff = toMinutes(endTime) - toMinutes(startTime);
        return diff / 30;
    };

    const tableStyle =
        "w-full min-w-[260px] border-separate border-spacing-1 text-center text-sm sm:text-[0.92rem]";
    const thStyle =
        "px-2 py-3 rounded-xl bg-surface-sunk font-mono text-[0.68rem] font-medium uppercase tracking-[0.14em] text-ink-3";

    const tdStyle =
        "p-1 sm:p-1.5 rounded-xl bg-[color-mix(in_oklab,var(--surface-strong)_55%,transparent)] min-w-[4.5rem] h-9";
    const rowSpanCellStyle =
        "relative !p-2.5 !bg-[linear-gradient(150deg,var(--accent-soft),color-mix(in_oklab,var(--accent-2)_10%,transparent))] shadow-[inset_0_0_0_1px_color-mix(in_oklab,var(--accent)_35%,transparent)] text-ink transition-transform duration-500 ease-[var(--ease-spring)] hover:scale-[1.03]";

    const scheduleTable = useMemo(() => {
        const coveredSlots = {};

        return (
            <div
                className="relative overflow-x-auto rounded-2xl">

                <table className={tableStyle} dir="rtl">
                    <thead>
                    <tr>
                        <th
                            className={`${thStyle} sticky right-0 z-30 !bg-surface-strong`}
                        >
                            שעה
                        </th>
                        {displayedDays.map((dayKey) => (
                            <th key={dayKey} className={thStyle}>
                                {daysInHebrew[dayKey]}
                            </th>
                        ))}
                    </tr>
                    </thead>
                    <tbody>
                    {timeSlots.map((slot) => (
                        <tr
                            key={slot}
                            className="group"
                        >
                            <td
                                className="px-2 py-1 rounded-xl font-mono text-[0.75rem] text-ink-3 sticky right-0 z-20 bg-surface-strong group-hover:text-accent transition-colors"
                            >
                                {slot}
                            </td>
                            {displayedDays.map((dayKey) => {
                                if (coveredSlots[`${dayKey}_${slot}`]) return null;

                                const lesson = lessonsByDay[dayKey]?.find(
                                    (l) => l.startTime === slot
                                );

                                if (lesson) {
                                    const rowSpan = calculateRowSpan(
                                        lesson.startTime,
                                        lesson.endTime
                                    );
                                    const startIdx = timeSlots.indexOf(slot);

                                    for (let i = 1; i < rowSpan; i++) {
                                        coveredSlots[`${dayKey}_${timeSlots[startIdx + i]}`] = true;
                                    }

                                    return (
                                        <td
                                            key={dayKey + slot}
                                            rowSpan={rowSpan}
                                            className={`${tdStyle} ${rowSpanCellStyle} align-middle`}
                                        >
                                            <span className="absolute top-2 right-2 w-1.5 h-1.5 rotate-45 rounded-[2px] bg-accent shadow-[0_0_10px_var(--glow)]" />
                                            <div className="title text-[0.92rem]">
                                                {lesson.subject}
                                            </div>
                                            {type === "teacher" && (
                                                <div className="text-xs text-ink-2 mt-0.5">
                                                    כיתה {lesson.classRoomName}
                                                </div>
                                            )}
                                            {type === "class" && (
                                                <div className="text-xs text-ink-2 mt-0.5">
                                                    מורה {lesson.teacherName}
                                                </div>
                                            )}
                                            {type === "student" && (
                                                <div className="text-xs text-ink-2 mt-0.5">
                                                    {lesson.teacherName}
                                                </div>
                                            )}
                                            <div className="num text-[0.65rem] text-ink-3 mt-1" dir="ltr">
                                                {lesson.startTime}–{lesson.endTime}
                                            </div>
                                        </td>
                                    );
                                } else {
                                    return (
                                        <td key={dayKey + slot} className={tdStyle}></td>
                                    );
                                }
                            })}
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        );
    }, [lessonsByDay, displayedDays, classRoomName, type]);

    return (

        <div className={singleDayMode ? 'h-full' : `page ${type === 'class' ? '' : 'page--wide'}`} dir="rtl">

            <div className={type === 'class' && !singleDayMode ? '' : `glass ${singleDayMode ? 'tile h-full' : ''} p-4 sm:p-6`}>
                {singleDayMode ? (
                    <div className="panel-head">
                        <div className="flex items-center gap-3">
                            <span className="icon-badge">
                                <svg
                                    xmlns="http://www.w3.org/2000/svg"
                                    className="h-5 w-5"
                                    fill="none"
                                    viewBox="0 0 24 24"
                                    stroke="currentColor"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth="1.8"
                                        d="M8 7V3m8 4V3m-9 8h10m-12 4h14M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
                                    />
                                </svg>
                            </span>
                            <div>
                                <span className="eyebrow">Today</span>
                                <h2 className="title text-xl mt-0.5 text-balance">
                                    מערכת השעות ליום {daysInHebrew[displayedDays[0]]}
                                </h2>
                            </div>
                        </div>
                    </div>
                ) : type !== 'class' && (
                    <div className="panel-head">
                        <div>
                            <span className="eyebrow">Weekly · שבועי</span>
                            <h2 className="display text-3xl sm:text-4xl mt-2">מערכת שעות</h2>
                        </div>
                    </div>
                )}

                <div className={`w-full max-w-full overflow-x-auto ${singleDayMode ? 'max-h-[34rem] scroll-area' : ''}`}>
                    <div className="w-full">{scheduleTable}</div>
                </div>
            </div>
        </div>

    );

}

export default WeeklySchedule;
