

import React, { useContext, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import axios from 'axios';
import Cookies from 'universal-cookie';
import { UserContext } from '../context/UserContext.jsx';
import { FiBookOpen, FiSend } from 'react-icons/fi';
import { FaExclamationCircle } from 'react-icons/fa';
import {BEARER_PREFIX} from "../constants/shared.constant.js";
import ProgressRing from "./ui/ProgressRing.jsx";
import OrbitLoader from "./ui/OrbitLoader.jsx";
import ExamFeedback from "../features/adaptive/ExamFeedback.jsx";
import { fetchExamFeedback, sendExamQuestionTimes } from "../features/adaptive/adaptiveApi.js";
import {
    AND,
    AUTH_HEADER, CHECK_PRACTICE_TEST, CHECK_TEACHER_TEST, DIFFICULTY, GENERATE_PRACTICE_TEST,
    GENERATE_TEST_FOR_STUDENTS, GET_TEST,
    QUESTION, QUESTION_COUNT, STUDENT_BASE_PATH, SUBJECT,
    TEACHER_BASE_PATH,
    TEST_BASE_PATH, TEST_ID, TIME_LIMIT_MINUTES, TOPIC, USER_ID
} from "../constants/pages.constants.js";

const TestSession = ({ type }) => {
    const { user } = useContext(UserContext);
    const {
        selectedSubject,
        selectedTopic,
        selectedDifficulty,
        selectedQuestionCount,
        selectedTimeMinutes,
    } = useParams();
    const { TeacherTestId, timeLimitMinutes } = useParams();

    const [testData, setTestData] = useState({});
    const [error, setError] = useState(null);
    const [testId, setTestId] = useState('');
    const [userAnswers, setUserAnswers] = useState({});
    const [testResults, setTestResults] = useState(null);
    const [progress, setProgress] = useState(0);
    const [showConfirm, setShowConfirm] = useState(false);
    const [fetchTestLoader, setFetchTestLoader] = useState(true);
    const [testResultLoader, setTestResultLoader] = useState(false);
    const [feedback, setFeedback] = useState(null);
    const [feedbackError, setFeedbackError] = useState(null);
    // Time per question: the question whose answer field was focused last collects the time.
    const questionTimesRef = useRef({});
    const activeQuestionRef = useRef({ id: null, since: 0 });

    const flushQuestionTime = () => {
        const { id, since } = activeQuestionRef.current;
        if (id === null) return;
        const now = Date.now();
        questionTimesRef.current[id] = (questionTimesRef.current[id] || 0) + (now - since);
        activeQuestionRef.current = { id, since: now };
    };

    const focusQuestion = (qid) => {
        flushQuestionTime();
        activeQuestionRef.current = { id: qid, since: Date.now() };
    };

    const loadFeedback = async (checkedTestId) => {
        const seconds = Object.fromEntries(
            Object.entries(questionTimesRef.current).map(([qid, ms]) => [qid, Math.round(ms / 1000)])
        );
        await sendExamQuestionTimes(user.userId, checkedTestId, seconds);
        const res = await fetchExamFeedback(user.userId, checkedTestId);
        if (res.success) setFeedback(res.data);
        else setFeedbackError(res.errorCode);
    };

    const [timeLeft, setTimeLeft] = useState(() => {
        let minutes = 0;
        if (type === 'practiceTest') minutes = parseInt(selectedTimeMinutes);
        else if (type === 'teacherTest') minutes = parseInt(timeLimitMinutes);
        return isNaN(minutes) ? 0 : minutes * 60;
    });

    const cookies = new Cookies();
    const token = cookies.get('token');

    useEffect(() => {
        if (testResults || timeLeft <= 0) return;
        const timer = setInterval(() => {
            setTimeLeft((prev) => {
                if (prev <= 1) {
                    clearInterval(timer);
                    if (!testResults) submitTest();
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);
        return () => clearInterval(timer);
    }, [timeLeft, testResults]);

    useEffect(() => {
        if (user?.userId && type === 'practiceTest') {
            (async () => {
                try {
                    const res = await axios.post(
                        `${STUDENT_BASE_PATH}${GENERATE_PRACTICE_TEST}${QUESTION}${USER_ID}${user.userId}${AND}${SUBJECT}${selectedSubject}${AND}${TOPIC}${selectedTopic}${AND}${DIFFICULTY}${selectedDifficulty}${AND}${QUESTION_COUNT}${selectedQuestionCount}${AND}${TIME_LIMIT_MINUTES}${selectedTimeMinutes}`,
                        {}, {
                            headers: {
                                [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                        }
                        }
                    );
                    setTestData(res.data.data);
                    setTestId(res.data.data.id);
                    setFetchTestLoader(false);
                } catch (err) {
                    setError(err);
                }
            })();
        }
    }, [
        user?.userId,
        type,
        selectedSubject,
        selectedTopic,
        selectedDifficulty,
        selectedQuestionCount,
        selectedTimeMinutes,
    ]);

    useEffect(() => {
        if (user?.userId && type === 'teacherTest') {
            (async () => {
                try {
                    const res = await axios.get(
                            `${TEST_BASE_PATH}${GET_TEST}${QUESTION}${TEST_ID}${TeacherTestId}`,
                        { headers: {
                            [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                            }
                        }
                    );
                    setTestData(res.data);
                    setFetchTestLoader(false);
                } catch (err) {
                    console.warn(err);
                }
            })();
        }
    }, [user?.userId, type, TeacherTestId]);

    const submitTest = async () => {
        flushQuestionTime();
        setTestResultLoader(true)
        try {
            let url = '';
            if (type === 'practiceTest') {
                url =`${STUDENT_BASE_PATH}${CHECK_PRACTICE_TEST}${QUESTION}${USER_ID}${user.userId}${AND}${TEST_ID}${testId}`;
            } else if (type === 'teacherTest') {
                url =`${TEACHER_BASE_PATH}${CHECK_TEACHER_TEST}${QUESTION}${USER_ID}${user.userId}${AND}${TEST_ID}${TeacherTestId}`;
            }
            const res = await axios.post(url, userAnswers, {
                headers: {
                    [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    ,
                    'Content-Type': 'application/json',
                },
            });
            if (res){
                setTestResults(res.data.data);
                setTestResultLoader(false)
                if (res.data.success !== false) {
                    loadFeedback(type === 'practiceTest' ? testId : TeacherTestId);
                }
            }
        } catch (err) {
            console.error(err);
            setError(err);
        }
    };

    const handleAnswerChange = (qid, answer) => {
        const updated = { ...userAnswers, [qid]: answer };
        setUserAnswers(updated);
        const answered = Object.values(updated).filter(Boolean).length;
        setProgress((answered / testData.questions.length) * 100);
    };

    const formatTime = (s) =>
        `${Math.floor(s / 60)}:${(s % 60).toString().padStart(2, '0')}`;

    if(testResultLoader){
        return (
            <OrbitLoader
                title="מחשב את התשובות שלך..."
                subtitle="אנחנו מנתחים את התשובות שלך כדי להציג את התוצאה המדויקת ביותר"
            />
        );

    }
    if (fetchTestLoader) {
        return (
            <OrbitLoader
                title="טוען את המבחן..."
                subtitle="אנא המתן מספר שניות בזמן שהמערכת מכינה עבורך את המבחן"
            />
        );
    }


    const questions = testData.questions;

    return (
        <div
            className="page page--wide space-y-4 sm:space-y-5"
            dir="rtl"
        >
            <div className="grid grid-cols-1 md:grid-cols-12 gap-4 sm:gap-5">
                {type === 'teacherTest' && (
                    <div className="md:col-span-7 glass p-6 sm:p-7 reveal">
                        <div className="flex items-center gap-3 mb-5">
                            <span className="icon-badge icon-badge--mint"><FiBookOpen /></span>
                            <div>
                                <span className="eyebrow">Exam · מבחן</span>
                                <h2 className="title text-2xl mt-0.5">פרטי המבחן</h2>
                            </div>
                        </div>
                        <dl className="grid grid-cols-3 gap-3">
                            <div className="glass--sunk p-3">
                                <dt className="faint text-xs">מקצוע</dt>
                                <dd className="font-semibold text-ink mt-1">{questions[0]?.subject || 'לא צוין'}</dd>
                            </div>
                            <div className="glass--sunk p-3">
                                <dt className="faint text-xs">נושא</dt>
                                <dd className="font-semibold text-ink mt-1">{questions[0]?.topic || 'לא צוין'}</dd>
                            </div>
                            <div className="glass--sunk p-3">
                                <dt className="faint text-xs">מספר שאלות</dt>
                                <dd className="num font-semibold text-ink mt-1">{questions.length}</dd>
                            </div>
                        </dl>
                    </div>
                )}

                {type === 'practiceTest' && (
                    <div className="md:col-span-7 glass p-6 sm:p-7 reveal">
                        <div className="flex items-center gap-3 mb-5">
                            <span className="icon-badge"><FiBookOpen /></span>
                            <div>
                                <span className="eyebrow">Practice · תרגול</span>
                                <h2 className="title text-2xl mt-0.5">מבחן תרגול</h2>
                            </div>
                        </div>
                        <dl className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                            <div className="glass--sunk p-3">
                                <dt className="faint text-xs">מקצוע</dt>
                                <dd className="font-semibold text-ink mt-1">{selectedSubject || 'לא נבחר'}</dd>
                            </div>
                            <div className="glass--sunk p-3">
                                <dt className="faint text-xs">נושא</dt>
                                <dd className="font-semibold text-ink mt-1">{selectedTopic || 'לא נבחר'}</dd>
                            </div>
                            <div className="glass--sunk p-3">
                                <dt className="faint text-xs">רמה</dt>
                                <dd className="font-semibold text-ink mt-1">{selectedDifficulty || 'לא נבחר'}</dd>
                            </div>
                            <div className="glass--sunk p-3">
                                <dt className="faint text-xs">מספר שאלות</dt>
                                <dd className="num font-semibold text-ink mt-1">{selectedQuestionCount || 0}</dd>
                            </div>
                        </dl>
                    </div>
                )}

                {!testResults && (
                    <div className="md:col-span-5 glass tile p-6 sm:p-7 flex items-center justify-between gap-6 reveal" style={{ "--i": 1 }}>
                        <div className="space-y-2">
                            <span className="eyebrow">זמן נותר</span>
                            <div className={`num text-5xl sm:text-6xl font-semibold ${timeLeft <= 60 ? 'text-danger' : 'text-ink'}`} dir="ltr">
                                {formatTime(timeLeft)}
                            </div>
                            <span className="faint text-xs">התקדמות במבחן</span>
                        </div>
                        <ProgressRing value={progress} size={112} stroke={9}>
                            <span className="num text-xl font-semibold text-ink">{Math.round(progress)}%</span>
                        </ProgressRing>
                    </div>
                )}
            </div>

            {testResults ? (
                <div className="grid grid-cols-1 md:grid-cols-12 gap-4 sm:gap-5">
                    <div className="md:col-span-5 glass tile p-7 sm:p-9 flex flex-col items-center justify-center text-center gap-5 reveal">
                        <span className="eyebrow">תוצאות המבחן</span>
                        <ProgressRing value={testResults.score} size={190} stroke={14}>
                            <span className="grid">
                                <span className="num text-5xl font-semibold text-ink">{testResults.score}</span>
                                <span className="faint text-xs mt-1">ציון</span>
                            </span>
                        </ProgressRing>
                        <p className="muted">
                            תשובות נכונות:{' '}
                            <span className="num font-semibold text-mint">
                                {testResults.correctAnswers} מתוך {questions.length}
                            </span>
                        </p>
                    </div>

                    <div className="md:col-span-7 grid sm:grid-cols-2 gap-4 sm:gap-5" dir="ltr">
                        <div className="glass p-6 reveal" style={{ "--i": 1 }} dir="rtl">
                            <h4 className="title text-lg mb-4 flex items-center gap-2">
                                <span className="w-2.5 h-2.5 rotate-45 rounded-[3px] bg-mint shadow-[0_0_12px_var(--mint)]" />
                                שאלות נכונות:
                            </h4>
                            <ul className="space-y-2" dir="ltr">
                                {testResults.correctAnswerList.map((q, i) => (
                                    <li
                                        key={i}
                                        className="row-card !py-2.5 num text-sm text-ink flex items-center gap-2 !border-[color-mix(in_oklab,var(--mint)_35%,transparent)] reveal"
                                        style={{ "--i": Math.min(i, 8) }}
                                    >
                                        <span className="text-mint">✓</span>
                                        <span>{q}</span>
                                    </li>
                                ))}
                            </ul>
                        </div>
                        <div className="glass p-6 reveal" style={{ "--i": 2 }} dir="rtl">
                            <h4 className="title text-lg mb-4 flex items-center gap-2">
                                <span className="w-2.5 h-2.5 rotate-45 rounded-[3px] bg-danger shadow-[0_0_12px_var(--danger)]" />
                                שאלות לא נכונות:
                            </h4>
                            <ul className="space-y-2" dir="ltr">
                                {testResults.incorrectAnswerList.map((q, i) => (
                                    <li
                                        key={i}
                                        className="row-card !py-2.5 num text-sm text-ink flex items-center gap-2 !border-[color-mix(in_oklab,var(--danger)_30%,transparent)] reveal"
                                        style={{ "--i": Math.min(i, 8) }}
                                    >
                                        <span className="text-danger">✕</span>
                                        <span>{q}</span>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    </div>

                    <div className="md:col-span-12">
                        {feedback && <ExamFeedback feedback={feedback} />}
                        {feedbackError && <p className="notice notice--danger">{feedbackError}</p>}
                    </div>
                </div>
            ) : (
                <form
                    onSubmit={(e) => {
                        e.preventDefault();
                        if (type === 'teacherTest') setShowConfirm(true);
                        else submitTest();
                    }}
                    className="space-y-5"
                >
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                        {questions.map((q, i) => (
                            <div
                                key={q.id}
                                className={`glass tile p-5 reveal-scroll ${userAnswers[q.id] ? '!border-[color-mix(in_oklab,var(--mint)_45%,transparent)]' : ''}`}
                                dir="ltr"
                            >
                                <div className="flex items-center justify-between mb-4">
                                    <span className="num text-xs text-ink-3">Q{String(i + 1).padStart(2, '0')}</span>
                                    <span className={`w-2 h-2 rotate-45 rounded-[2px] transition-all duration-500 ${userAnswers[q.id] ? 'bg-mint shadow-[0_0_10px_var(--mint)]' : 'bg-line-strong'}`} />
                                </div>
                                <label className="block num text-lg font-medium text-ink mb-4 text-left leading-snug">
                                    {i + 1}. {q.questionText}
                                </label>
                                <input
                                    type="text"
                                    value={userAnswers[q.id] || ''}
                                    onChange={(e) => handleAnswerChange(q.id, e.target.value)}
                                    onFocus={() => focusQuestion(q.id)}
                                    className="field num"
                                />
                            </div>
                        ))}
                    </div>

                    <div className="sticky bottom-4 z-20">
                        <button
                            type="submit"
                            disabled={!questions.every((q) => userAnswers[q.id])}
                            className="btn btn-primary btn-block !min-h-[3.5rem] shadow-[var(--shadow-2)]"
                        >
                            <FiSend />
                            הגש מבחן
                        </button>
                    </div>
                </form>
            )}

            {showConfirm && (
                <div className="modal-backdrop">
                    <div className="modal space-y-5 text-right" dir="rtl">
                        <div className="flex items-center gap-3">
                            <span className="icon-badge icon-badge--amber"><FaExclamationCircle /></span>
                            <span className="title text-xl">אישור הגשה</span>
                        </div>
                        <p className="muted leading-relaxed">
                            האם אתה בטוח שברצונך להגיש את המבחן? לא תוכל לשנות תשובות
                            לאחר מכן.
                        </p>
                        <div className="flex justify-end gap-2 pt-2">
                            <button
                                onClick={() => {
                                    setShowConfirm(false);
                                    submitTest();
                                }}
                                className="btn btn-primary"
                            >
                                הגש
                            </button>
                            <button
                                onClick={() => setShowConfirm(false)}
                                className="btn btn-ghost"
                            >
                                חזור
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default TestSession;



