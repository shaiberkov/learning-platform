import {useContext, useEffect, useRef, useState} from "react";
import {UserContext} from "../../context/UserContext.jsx";
import {useParams} from "react-router-dom";
import axios from "axios";
import ChatComponent from "../../components/ChatComponent.jsx";
import Cookies from "universal-cookie";
import {
    AND,
    AUTH_HEADER, CHAT_BASE_PATH, GENERATE_QUESTION, MESSAGE, QUESTION,
    STUDENT_BASE_PATH, SUB_TOPIC, SUBJECT, SUBMIT_ANSWER, TEACHER_ID, TOPIC, USER_ID, WITH_MEMORY
} from "../../constants/pages.constants.js";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";
import {useTranslation} from "react-i18next";
import useOnlineStatus from "../../offline/useOnlineStatus.js";
import {countQuestions, exerciseKey, queueAnswer, saveQuestions, takeQuestion} from "../../offline/practiceStore.js";

const OFFLINE_BATCH = 10;

function QuestionPractice(){


    const { subjectName, topicName, exerciseName } = useParams();
    const { user } = useContext(UserContext);
    const { t } = useTranslation();
    const online = useOnlineStatus();
    const [cachedCount, setCachedCount] = useState(0);
    const [offlineNotice, setOfflineNotice] = useState("");
    const [savingOffline, setSavingOffline] = useState(false);
    const offlineKey = exerciseKey(user?.userId, subjectName, topicName, exerciseName);
    const [exercise, setExercise] = useState({});
    const [answer, setAnswer] = useState("");
    const [responseMessage, setResponseMessage] = useState(null);
    const [isCorrect, setIsCorrect] = useState(null);
    const [isChatOpen, setIsChatOpen] = useState(false);
    const [newMessages, setNewMessages] = useState(0);
    const [isDrawing, setIsDrawing] = useState(false);
    const [messages, setMessages] = useState([]);
    const [userMessage, setUserMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const cookies = new Cookies();
    const token=cookies.get('token')
    const canvasRef = useRef(null);
    useEffect(() => {
        const canvas = canvasRef.current;
        const ctx = canvas.getContext("2d");

        ctx.fillStyle = "#fff";
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.lineWidth = 2;
        ctx.strokeStyle = "#000";
        ctx.lineJoin = "round";
    }, []);

    const handleMouseDown = (e) => {
        const canvas = canvasRef.current;
        const ctx = canvas.getContext("2d");
        const rect = canvas.getBoundingClientRect();
        ctx.moveTo(e.clientX - rect.left, e.clientY - rect.top);
        setIsDrawing(true);
    };

    const handleMouseMove = (e) => {
        if (!isDrawing) return;
        const canvas = canvasRef.current;
        const ctx = canvas.getContext("2d");
        const rect = canvas.getBoundingClientRect();
        ctx.lineTo(e.clientX - rect.left, e.clientY - rect.top);
        ctx.stroke();
    };

    const handleMouseUp = () => {
        setIsDrawing(false);
    };

    const handleClearCanvas = () => {
        const canvas = canvasRef.current;
        const ctx = canvas.getContext("2d");

        ctx.clearRect(0, 0, canvas.width, canvas.height);

        ctx.fillStyle = "#fff";
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.lineWidth = 2;
        ctx.strokeStyle = "#000";
        ctx.lineJoin = "round";
        ctx.beginPath();
    };

    const handleSaveDrawing = () => {
        const canvas = canvasRef.current;
        const image = canvas.toDataURL("image/png");
        console.log("תמונה שנשמרה:", image);
    };


    const requestQuestion = () => axios.get(
            `${STUDENT_BASE_PATH}${GENERATE_QUESTION}${QUESTION}${USER_ID}${user.userId}${AND}${SUBJECT}${subjectName}${AND}${TOPIC}${topicName}${AND}${SUB_TOPIC}${exerciseName}`,
            {
                headers: {
                    [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                }
            });

    // Offline: the next question saved in advance for this exercise (IndexedDB).
    const nextOfflineQuestion = async () => {
        try {
            const saved = await takeQuestion(offlineKey);
            setCachedCount(await countQuestions(offlineKey));
            if (saved) {
                setExercise(saved);
                setOfflineNotice("");
            } else {
                setExercise({});
                setOfflineNotice(t("offline.noCached"));
            }
        } catch {
            setOfflineNotice(t("offline.noCached"));
        }
    };

    const fetchData = async () => {
        console.log(user.name,"ביקשתי שאלה")
         setMessages([]);

        if (!navigator.onLine) {
            await nextOfflineQuestion();
            return;
        }
        try {
            const response = await requestQuestion();
            if (response.data) {
                setExercise(response.data.data);
                setOfflineNotice("");
            }
        } catch (error) {
            if (!error.response) {
                await nextOfflineQuestion();
            } else {
                console.error("שגיאה בטעינת שאלה:", error);
            }
        }
    };

    useEffect(() => {
        if(user?.userId){
            fetchData();
            countQuestions(offlineKey).then(setCachedCount);
        }
    }, [subjectName, topicName,user.userId]);

    // Downloads a batch of questions now so practice can continue without a connection.
    const saveForOffline = async () => {
        setSavingOffline(true);
        try {
            const results = await Promise.allSettled(Array.from({ length: OFFLINE_BATCH }, () => requestQuestion()));
            const questions = results
                .filter((r) => r.status === "fulfilled" && r.value.data?.data?.id)
                .map((r) => r.value.data.data);
            await saveQuestions(offlineKey, questions);
            setCachedCount(await countQuestions(offlineKey));
            setOfflineNotice(t("offline.saved", { count: questions.length }));
        } catch (error) {
            console.error("שמירת שאלות לתרגול לא מקוון נכשלה:", error);
        } finally {
            setSavingOffline(false);
        }
    };

    const queueOffline = async (params) => {
        await queueAnswer(params);
        setResponseMessage(t("offline.queued"));
        setIsCorrect(null);
        setAnswer("");
        await nextOfflineQuestion();
    };

    const submitAnswer = async () => {
        const { id, subTopic} = exercise;
        const answerParams = {
            userId: user.userId,
            id: id,
            subTopic: subTopic,
            answer: answer
        };
        if (!navigator.onLine) {
            await queueOffline(answerParams);
            return;
        }
        try {
            const params = new URLSearchParams(answerParams);

            const response = await axios.post(
                `${STUDENT_BASE_PATH}${SUBMIT_ANSWER}${QUESTION}${params.toString()}`,
                null,
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    }
                }
            );
            setResponseMessage(response.data.data.message);
            setIsCorrect(response.data.data.correct);
            fetchData();
            setAnswer("");
            return response.data.data;
        } catch (error) {
            if (!error.response) {
                await queueOffline(answerParams); // the connection dropped mid-request
                return;
            }
            console.error("שגיאה בשליחת התשובה:", error);
        }
    };


    const toggleChat = () => {
        setIsChatOpen(!isChatOpen);
        setNewMessages(0);
    };

    const sendMessage = async () => {
        if (!userMessage.trim()) return;

        setMessages((prevMessages) => [
            ...prevMessages,
            { sender: "user", content: userMessage },
        ]);
        setUserMessage("");
        setIsLoading(true);

        try {
            const response = await axios.post(
                `${CHAT_BASE_PATH}${WITH_MEMORY}${QUESTION}${USER_ID}${user.userId}${AND}${MESSAGE}${userMessage}`,
                {
                    headers: {
                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                    }
                }
            );

            setMessages((prevMessages) => [
                ...prevMessages,
                { sender: "chatgpt", content: response.data },
            ]);
        } catch (error) {
            console.error("Error sending message:", error);
            const errorMessage = {
                sender: "chatgpt",
                content: "Failed to get response from ChatGPT.",
            };
            setMessages((prevMessages) => [...prevMessages, errorMessage]);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="page page--wide">
            <section className="exercise-page grid grid-cols-1 lg:grid-cols-12 gap-4 sm:gap-5" dir="rtl">
                <div className="lg:col-span-5 glass p-6 sm:p-8 space-y-6 reveal">
                    <div className="space-y-2">
                        <span className="eyebrow">Exercise · תרגיל</span>
                        <h2 className="display text-3xl sm:text-4xl">
                            תרגיל: {exerciseName}
                        </h2>
                    </div>

                    {exercise && (
                        <>
                            <div className="glass--sunk p-5 sm:p-6" dir="ltr">
                                <span className="num text-[0.65rem] text-ink-3 tracking-[0.18em] uppercase">Question</span>
                                <h4 className="num text-xl sm:text-2xl font-medium text-ink mt-2 leading-relaxed break-words">
                                    {exercise.questionText}
                                </h4>
                            </div>

                            <label htmlFor="practice-answer" className="sr-only">תשובה</label>
                            <input
                                id="practice-answer"
                                type="text"
                                placeholder="תשובה"
                                value={answer}
                                onChange={(e) => setAnswer(e.target.value)}
                                onKeyDown={(e) => { if (e.key === "Enter" && answer && exercise.id) submitAnswer(); }}
                                className="field num !text-lg !min-h-[3.5rem]"
                                dir="ltr"
                            />

                            <button
                                onClick={submitAnswer}
                                disabled={!answer || !exercise.id}
                                className="btn btn-primary btn-block"
                            >
                                שלח תשובה
                            </button>
                        </>
                    )}

                    {responseMessage && (
                        <p
                            role="status"
                            className={`notice ${
                                isCorrect === null ? "" : isCorrect ? "notice--mint" : "notice--danger"
                            }`}
                        >
                            {responseMessage}
                        </p>
                    )}

                    {offlineNotice && <p className="notice" role="status">{offlineNotice}</p>}

                    <div className="pt-4 border-t border-dashed border-line flex flex-wrap items-center justify-between gap-2">
                        <span className="faint text-xs">{t("offline.cachedLeft", { count: cachedCount })}</span>
                        <button
                            type="button"
                            className="btn btn-ghost btn-sm"
                            onClick={saveForOffline}
                            disabled={!online || savingOffline}
                        >
                            {savingOffline ? t("common.loading") : t("offline.saveForOffline")}
                        </button>
                    </div>
                </div>

                <div className="lg:col-span-7 glass p-4 sm:p-6 flex flex-col gap-4 reveal" style={{ "--i": 1 }}>
                    <div className="flex items-center justify-between">
                        <div>
                            <span className="eyebrow">Scratchpad · דף טיוטה</span>
                            <p className="faint text-xs mt-1">שרטט, חשב ונסה — הדף נשאר רק אצלך.</p>
                        </div>
                        <button
                            onClick={handleClearCanvas}
                            className="btn btn-ghost btn-sm"
                        >
                            נקה
                        </button>
                    </div>
                    <canvas
                        ref={canvasRef}
                        width={500}
                        height={300}
                        onMouseDown={handleMouseDown}
                        onMouseMove={handleMouseMove}
                        onMouseUp={handleMouseUp}
                        onMouseLeave={() => setIsDrawing(false)}
                        className="w-full h-60 sm:h-[22rem] rounded-[20px] border border-line-strong cursor-crosshair
                     shadow-[inset_0_2px_6px_rgba(0,0,0,0.06),var(--shadow-1)] bg-white"
                    />
                </div>
            </section>

            <button
                onClick={toggleChat}
                className="fixed bottom-5 left-5 z-40 btn btn-primary !rounded-full !px-6 shadow-[var(--shadow-2)]"
            >
                <span className="relative flex w-2.5 h-2.5">
                    <span className="absolute inline-flex h-full w-full rounded-full bg-white/70 animate-ping" />
                    <span className="relative inline-flex w-2.5 h-2.5 rounded-full bg-white" />
                </span>
                צ'אט
        {/*        {newMessages > 0 && (*/}
        {/*            <span className="flex h-5 w-5 items-center justify-center rounded-full bg-red-600 text-xs">*/}
        {/*  {newMessages}*/}
        {/*</span>*/}
        {/*        )}*/}
            </button>


            {isChatOpen && (
                <div
                    className="fixed bottom-24 left-4 w-96 max-w-[calc(100vw-2rem)] h-[60vh] sm:h-[480px] flex flex-col
        glass glass--strong !rounded-[26px] overflow-hidden z-50 shadow-[var(--shadow-2)]"
                    style={{ animation: "modal-in .6s var(--ease-spring) both" }}
                >


                    <header className="flex justify-between items-center px-5 py-4 border-b border-dashed border-line" dir="rtl">

                        <div className="flex items-center gap-3">
                            <span className="icon-badge !w-9 !h-9 !rounded-xl">
                                <span className="w-2.5 h-2.5 rotate-45 rounded-[3px] bg-current" />
                            </span>
                            <span className="grid">
                                <span className="title text-base">
                                    צ'אט עם מורה
                                </span>
                                <span className="eyebrow !text-[0.58rem]">AI tutor · online</span>
                            </span>
                        </div>

                        <button
                            onClick={toggleChat}
                            className="grid place-items-center w-9 h-9 rounded-xl text-ink-3 hover:text-danger hover:rotate-90 transition-all duration-500 ease-[var(--ease-spring)]"
                            title="סגור"
                        >
                            ✕
                        </button>
                    </header>


                    {/* גוף הצ'אט – גלילה פנימית */}
                    <main className="flex-1 overflow-y-auto">
                        <ChatComponent
                            messages={messages}
                            userMessage={userMessage}
                            setUserMessage={setUserMessage}
                            sendMessage={sendMessage}
                            isLoading={isLoading}
                            setIsOpen={setIsChatOpen}
                        />
                    </main>
                </div>
            )}

        </div>
    );

}
export default QuestionPractice;
