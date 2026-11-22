

import { useContext, useEffect, useState } from "react";
import { UserContext } from "../../context/UserContext.jsx";
import Cookies from "universal-cookie";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { useNotificationContext } from "../../context/NotificationContext.jsx";
import {
    FaPlay,
    FaCheckCircle,
    FaClock,
    FaLayerGroup,
    FaBookOpen,
    FaClipboardList, FaExclamationCircle, FaTimesCircle, FaRegCalendarAlt, FaChartBar
} from "react-icons/fa";
import {
    AND,
    AUTH_HEADER,
    GENERATE_QUESTION, GET_STUDENT_TESTS_STATUS,
    QUESTION, START_TEST,
    STUDENT_BASE_PATH, STUDENT_ID, SUB_TOPIC, SUBJECT, TEST_ID, TOPIC,
    USER_ID
} from "../../constants/pages.constants.js";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";

function TestsTable() {
    const [testsData, setTestsData] = useState([]);
    const { user } = useContext(UserContext);
    const cookies = new Cookies();
    const token = cookies.get("token");
    const navigate = useNavigate();
    const { tests } = useNotificationContext();

    const [showModal, setShowModal] = useState(false);
    const [modalMessage, setModalMessage] = useState("");
    const [pendingNavigation, setPendingNavigation] = useState(null);

    const parseDateTime = (str) => {
        const [datePart, timePart] = str.split(" ");
        const [day, month, year] = datePart.split("/");
        const dateTimeString = `${year}-${month}-${day}T${timePart || "00:00"}`;
        return new Date(dateTimeString);
    };

    const startTest = async (teacherTestId, timeLimitMinutes, startTimeStr) => {
        try {
            const response = await axios.post(
                `${STUDENT_BASE_PATH}${START_TEST}${QUESTION}${USER_ID}${user.userId}${AND}${TEST_ID}${teacherTestId}`,
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    },
                }
            );

            if (response.data.success) {
                setModalMessage(response.data.errorCode);
                setPendingNavigation({
                    testId: teacherTestId,
                    timeLimit: timeLimitMinutes,
                    startTime: startTimeStr,
                });
                setShowModal(true);
            } else {
                setModalMessage(response.data.errorCode);
                setPendingNavigation(null);
                setShowModal(true);
            }
        } catch (error) {
            console.error("Failed to start test", error);
            setModalMessage("אירעה שגיאה בעת התחלת המבחן");
            setPendingNavigation(null);
            setShowModal(true);
        }
    };

    useEffect(() => {
        const fetchTests = async () => {
            if (user?.userId) {
                try {
                    const response = await axios.get(
                        `${STUDENT_BASE_PATH}${GET_STUDENT_TESTS_STATUS}${QUESTION}${STUDENT_ID}${user.userId}`,
                        {
                            headers: {
                                [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                            },
                        }
                    );
                    const sorted = response.data.sort((a, b) => {
                        return parseDateTime(b.startTime) - parseDateTime(a.startTime);
                    });
                    setTestsData(sorted);
                } catch (err) {
                    console.error("Error fetching tests data", err);
                }
            }
        };

        fetchTests();
    }, [user?.userId]);

    const combinedTests = [...tests, ...testsData];

    return (


        <div className="page page--wide" dir="rtl">
            <div className="glass p-5 sm:p-8 space-y-6 reveal">
            <div className="flex flex-wrap items-end justify-between gap-4">
                <div className="space-y-2">
                    <span className="eyebrow">Assessments · מבחנים</span>
                    <h2 className="display text-4xl sm:text-5xl flex items-center gap-3">
                        רשימת המבחנים שלך
                    </h2>
                </div>
                <span className="chip num"><FaClipboardList /> {combinedTests.length}</span>
            </div>

            {combinedTests.length > 0 ? (
                <div className="overflow-x-auto -mx-2 px-2">
                    <table className="data-table min-w-[640px] text-right">
                        <thead>
                        <tr>
                            <th>
                                <FaChartBar
                                    className="inline ml-2"
                                />
                                ציון / פעולה
                            </th>
                            <th>
                                <FaClock
                                    className="inline ml-2"/>
                                משך המבחן
                            </th>
                            <th>
                                <FaRegCalendarAlt
                                    className="inline ml-2"
                                />
                                תאריך
                            </th>
                            <th>
                                <FaLayerGroup
                                    className="inline ml-2"/>
                                תת נושא
                            </th>
                            <th>
                                <FaBookOpen
                                    className="inline ml-2"/>
                                נושא
                            </th>
                        </tr>
                        </thead>


                        <tbody>
                        {combinedTests.map((test) => (
                            <tr key={test.testId}>
                                <td>
                                    {test.score !== -1 ? (
                                        <span
                                            className={`chip num !text-sm !px-3 !py-1 ${test.score >= 60 ? 'chip--mint' : 'chip--danger'}`}>
            {test.score >= 60 ? (
                <FaCheckCircle/>
            ) : (
                <FaTimesCircle/>
            )}
                                            {test.score}
          </span>
                                    ) : (
                                        <button
                                            onClick={() =>
                                                startTest(
                                                    test.testId,
                                                    test.timeLimitMinutes,
                                                    test.startTime
                                                )
                                            }
                                            className="btn btn-primary btn-sm mx-auto"
                                        >
                                            <FaPlay/>
                                            התחל
                                        </button>
                                    )}
                                </td>
                                <td className="num">{test.timeLimitMinutes}</td>
                                <td className="num" dir="ltr">{test.startTime}</td>
                                <td>{test.topic}</td>
                                <td className="font-semibold !text-ink">{test.subject}</td>
                            </tr>
                        ))}
                        </tbody>

                    </table>
                </div>
            ) : (
                <p className="faint text-center py-10">
                    אין מבחנים זמינים כעת
                </p>
            )}
            </div>

            {/* Modal */}
            {showModal && (


                <div
                    className="modal-backdrop">
                    <div
                        className="modal space-y-5 text-right"
                        dir="rtl"
                    >
                        <div className="flex items-center gap-3">
                            <span className="icon-badge"><FaExclamationCircle/></span>
                            <span className="title text-xl">הודעה</span>
                        </div>

                        <p className="muted leading-relaxed">{modalMessage}</p>

                        <div className="flex justify-end gap-2 pt-2">
                            {pendingNavigation ? (
                                <>
                                    <button
                                        onClick={() => {
                                            const now = new Date();
                                            const start = parseDateTime(pendingNavigation.startTime);
                                            const diff = Math.max(0, Math.floor((now - start) / 60000));
                                            const updatedLimit = pendingNavigation.timeLimit - diff;
                                            navigate(`/test/${pendingNavigation.testId}/${updatedLimit}`);
                                        }}
                                        className="btn btn-primary"
                                    >
                                        המשך
                                    </button>
                                    <button
                                        onClick={() => {
                                            setShowModal(false);
                                            setPendingNavigation(null);
                                        }}
                                        className="btn btn-ghost"
                                    >
                                        חזור
                                    </button>
                                </>
                            ) : (
                                <button
                                    onClick={() => setShowModal(false)}
                                    className="btn btn-ghost"
                                >
                                    סגור
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>

    );
}

export default TestsTable;
