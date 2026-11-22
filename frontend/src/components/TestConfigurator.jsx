import React, {useContext, useState} from 'react';
import { useNavigate } from 'react-router-dom';
import DateTime from 'react-datetime';
import dayjs from 'dayjs';
import axios from "axios";
import {UserContext} from "../context/UserContext.jsx";
import Cookies from "universal-cookie";
import {
    FaBook,
    FaClipboardList,
    FaLayerGroup,
    FaSortAmountDown,
    FaClock,
    FaCalendarAlt,
    FaUserGraduate,
    FaPlay
} from "react-icons/fa";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { he } from "date-fns/locale";
import "../Styles/datePicker.css";
import {
    AND,
    AUTH_HEADER, DIFFICULTY,
    GENERATE_TEST_FOR_STUDENTS,
    QUESTION, QUESTION_COUNT,
    SUBJECT, TEACHER_BASE_PATH, TEACHER_ID, TEST_START_TIME, TIME_LIMIT_MINUTES, TOPIC
} from "../constants/pages.constants.js";
import {BEARER_PREFIX} from "../constants/shared.constant.js";


const TestConfigurator = ({type}) => {
    const navigate = useNavigate();
    const { user } = useContext(UserContext);
    const cookies = new Cookies();
    const token = cookies.get('token');

    const subjects = ['מתמטיקה'];
    const topics = {
        'מתמטיקה': ['מספרים שלמים'],
    };
    const difficulties = ['קל', 'בינוני', 'קשה'];
    const questionCounts = Array.from({ length: 100 }, (_, i) => i + 1);

    const [selectedSubject, setSelectedSubject] = useState('');
    const [selectedTopic, setSelectedTopic] = useState('');
    const [selectedDifficulty, setSelectedDifficulty] = useState('');
    const [selectedQuestionCount, setSelectedQuestionCount] = useState('');
    const [selectedTimeMinutes, setSelectedTimeMinutes] = useState(30);
    const [startTime, setStartTime] = useState('');
    const [studentsIdsInput, setStudentsIdsInput] = useState('');
    const [message, setMessage] = useState('');


    const navigateToTest = () => {
        navigate(`/test/${selectedSubject}/${selectedTopic}/${selectedDifficulty}/${selectedQuestionCount}/${selectedTimeMinutes}`);
    };


    const handleSubmit = async () => {
        const formattedStartTime = dayjs(startTime).format('DD/MM/YYYY HH:mm');

        const usersIds = studentsIdsInput
            .split(',')
            .map(id => id.trim())
            .filter(id => id.length > 0);

        const url =`${TEACHER_BASE_PATH}${GENERATE_TEST_FOR_STUDENTS}${QUESTION}${TEACHER_ID}${user.userId}${AND}${TEST_START_TIME}${formattedStartTime}${AND}${SUBJECT}${selectedSubject}${AND}${TOPIC}${selectedTopic}${AND}${DIFFICULTY}${selectedDifficulty}${AND}${QUESTION_COUNT}${selectedQuestionCount}${AND}${TIME_LIMIT_MINUTES}${selectedTimeMinutes}`;

        try {
            const response = await axios.post(url, usersIds, {
                headers: {
                    'Content-Type': 'application/json',
                    [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                }
            });
            if(response.data.success){
                setMessage(response.data.errorCode)
            }else {
                setMessage(response.data.errorCode)
            }

        } catch (error) {
            console.error('שגיאה ביצירת מבחן:', error);
        }
    };


    return (
        <div className="page page--medium" dir="rtl">
            <div className="glass p-6 sm:p-9 space-y-6 reveal">
            <div className="space-y-3 pb-2">
                <span className="eyebrow">{type === "teacher" ? "Test builder · יצירת מבחן" : "Mock exam · תרגול מבחנים"}</span>
                <h2 className="display text-4xl sm:text-5xl flex items-center gap-3">
                    <span className="icon-badge icon-badge--mint"><FaClipboardList /></span>
                    יצירת מבחן
                </h2>
            </div>

            <div className="reveal">
                <label className="label">
                    <FaBook />
                    בחר מקצוע:
                </label>
                <select className="field" value={selectedSubject}
                        onChange={(e) => setSelectedSubject(e.target.value)}>
                    <option value="">-- בחר --</option>
                    {subjects.map((subject) => (
                        <option key={subject} value={subject}>{subject}</option>
                    ))}
                </select>
            </div>

            {selectedSubject && (
                <div className="reveal">
                    <label className="label">
                        <FaLayerGroup />
                        בחר נושא:
                    </label>
                    <select className="field" value={selectedTopic}
                            onChange={(e) => setSelectedTopic(e.target.value)}>
                        <option value="">-- בחר --</option>
                        {topics[selectedSubject].map((topic) => (
                            <option key={topic} value={topic}>{topic}</option>
                        ))}
                    </select>
                </div>
            )}

            {selectedTopic && (
                <div className="reveal">
                    <label className="label">
                        <FaSortAmountDown />
                        בחר רמת קושי:
                    </label>
                    <select className="field" value={selectedDifficulty}
                            onChange={(e) => setSelectedDifficulty(e.target.value)}>
                        <option value="">-- בחר --</option>
                        {difficulties.map((level) => (
                            <option key={level} value={level}>{level}</option>
                        ))}
                    </select>
                </div>
            )}

            {selectedDifficulty && (
                <div className="grid sm:grid-cols-2 gap-4 reveal">
                    <div>
                        <label className="label">
                            <FaClipboardList />
                            בחר מספר שאלות:
                        </label>
                        <select className="field num" value={selectedQuestionCount}
                                onChange={(e) => setSelectedQuestionCount(parseInt(e.target.value))}>
                            {questionCounts.map((count) => (
                                <option key={count} value={count}>{count}</option>
                            ))}
                        </select>
                    </div>

                    {selectedQuestionCount && (
                        <div className="reveal">
                            <label className="label">
                                <FaClock />
                                זמן בדקות:
                            </label>
                            <input
                                type="number"
                                min="1"
                                className="field num"
                                value={selectedTimeMinutes || ""}
                                onChange={(e) => {
                                    const value = e.target.value;
                                    const parsedValue = value ? parseInt(value, 10) : "";
                                    setSelectedTimeMinutes(parsedValue);
                                }}
                            />
                        </div>
                    )}
                </div>
            )}

            {type === "teacher" && selectedSubject && selectedTopic && selectedDifficulty && selectedQuestionCount && selectedTimeMinutes > 0 && (
                <div className="reveal">
                    <label className="label">
                        <FaCalendarAlt />
                        בחר תאריך ושעה למבחן:
                    </label>
                    <DatePicker
                        selected={startTime}
                        onChange={(date) => setStartTime(date)}
                        showTimeSelect
                        timeFormat="HH:mm"
                        timeIntervals={15}
                        dateFormat="Pp"
                        locale={he}
                        placeholderText="בחר תאריך ושעה"
                        className="field num"
                        wrapperClassName="w-full"

                    />
                </div>
            )}

            {selectedSubject && selectedTopic && selectedDifficulty && selectedQuestionCount && selectedTimeMinutes > 0 && startTime && type === "teacher" && (
                <div className="reveal">
                    <label className="label">
                        <FaUserGraduate />
                        הכנס ת"ז של תלמידים (מופרדים בפסיקים):
                    </label>
                    <textarea
                        rows={4}
                        className="field num resize-none"
                        value={studentsIdsInput}
                        onChange={(e) => setStudentsIdsInput(e.target.value)}
                        placeholder="123456789,987654321,..."

                    />
                </div>
            )}

            {(type === "teacher" && selectedSubject && selectedTopic && selectedDifficulty && selectedQuestionCount && selectedTimeMinutes > 0 && startTime) ||
            (type === "student" && selectedSubject && selectedTopic && selectedDifficulty && selectedQuestionCount && selectedTimeMinutes > 0) ? (
                <div className="pt-2 reveal">
                    <button
                        onClick={type === "teacher" ? handleSubmit : navigateToTest}
                        className="btn btn-primary btn-block"

                    >
                        <FaPlay />
                        צור מבחן
                    </button>
                </div>
            ) : null}


            {message && (
                <div className="notice">
                    {message}
                </div>
            )}
            </div>
        </div>

    );
};

export default TestConfigurator;
