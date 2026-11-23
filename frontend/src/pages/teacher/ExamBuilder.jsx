import { useContext, useState } from "react";
import { Link } from "react-router-dom";
import { FaClipboardList } from "react-icons/fa";
import { FiPlus, FiSend } from "react-icons/fi";
import { UserContext } from "../../context/UserContext.jsx";
import QuestionEditor from "../../components/exam/QuestionEditor.jsx";
import { emptyQuestion, toPayload, emptySchedule, schedulePayload } from "../../components/exam/examForms.js";
import ExamScheduleFields from "../../components/exam/ExamScheduleFields.jsx";
import { examPost } from "../../components/exam/examApi.js";
import { CREATE_EXAM } from "../../constants/pages.constants.js";

// Teacher exam builder: authored questions of every type, schedule, publish time, A/B versions.
export default function ExamBuilder() {
    const { user } = useContext(UserContext);
    const [title, setTitle] = useState("");
    const [subject, setSubject] = useState("מתמטיקה");
    const [topic, setTopic] = useState("");
    const [shuffleQuestions, setShuffleQuestions] = useState(true);
    const [shuffleOptions, setShuffleOptions] = useState(true);
    const [questions, setQuestions] = useState([emptyQuestion()]);
    const [schedule, setSchedule] = useState(emptySchedule());
    const [message, setMessage] = useState("");
    const [success, setSuccess] = useState(false);
    const [busy, setBusy] = useState(false);

    const updateQuestion = (i, q) => setQuestions(questions.map((x, j) => (j === i ? q : x)));

    const submit = async () => {
        setBusy(true);
        setMessage("");
        try {
            const res = await examPost(CREATE_EXAM, { teacherId: user.userId }, {
                title, subject, topic,
                shuffleQuestions, shuffleOptions,
                questions: questions.map(toPayload),
                ...schedulePayload(schedule),
            });
            setSuccess(res.success);
            setMessage(res.errorCode || (res.success ? "המבחן נשמר" : "השמירה נכשלה"));
            if (res.success) {
                setQuestions([emptyQuestion()]);
                setTitle("");
            }
        } catch {
            setSuccess(false);
            setMessage("אירעה שגיאה בשמירת המבחן");
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-5 reveal">
                <div className="space-y-2">
                    <span className="eyebrow">Exam builder · בניית מבחן</span>
                    <h2 className="display text-4xl flex items-center gap-3">
                        <span className="icon-badge icon-badge--mint"><FaClipboardList /></span>
                        בניית מבחן
                    </h2>
                    <p className="faint text-sm">
                        בחירה מרובה, תשובה מספרית עם סטייה, התאמה, סידור ותשובה קצרה. הבדיקה נעשית בשרת,
                        והתשובות הנכונות לא נשלחות לתלמידים לפני ההגשה.
                    </p>
                </div>
                <div className="grid sm:grid-cols-3 gap-4">
                    <div>
                        <label className="label">כותרת</label>
                        <input className="field" value={title} onChange={(e) => setTitle(e.target.value)} />
                    </div>
                    <div>
                        <label className="label">מקצוע</label>
                        <input className="field" value={subject} onChange={(e) => setSubject(e.target.value)} />
                    </div>
                    <div>
                        <label className="label">נושא</label>
                        <input className="field" value={topic} onChange={(e) => setTopic(e.target.value)} />
                    </div>
                </div>
                <div className="flex flex-wrap gap-5">
                    <label className="flex items-center gap-2 text-sm text-ink">
                        <input type="checkbox" checked={shuffleQuestions} onChange={(e) => setShuffleQuestions(e.target.checked)} />
                        סדר שאלות שונה לכל תלמיד
                    </label>
                    <label className="flex items-center gap-2 text-sm text-ink">
                        <input type="checkbox" checked={shuffleOptions} onChange={(e) => setShuffleOptions(e.target.checked)} />
                        סדר תשובות שונה לכל תלמיד
                    </label>
                </div>
            </div>

            {questions.map((q, i) => (
                <QuestionEditor
                    key={i}
                    index={i}
                    question={q}
                    onChange={(next) => updateQuestion(i, next)}
                    onRemove={() => setQuestions(questions.length > 1 ? questions.filter((_, j) => j !== i) : [emptyQuestion()])}
                />
            ))}
            <button type="button" className="btn btn-ghost" onClick={() => setQuestions([...questions, emptyQuestion()])}>
                <FiPlus /> הוספת שאלה
            </button>

            <div className="glass p-6 sm:p-8 space-y-5 reveal">
                <h3 className="title text-xl">מועד, תלמידים ופרסום</h3>
                <ExamScheduleFields value={schedule} onChange={setSchedule} />
                <button type="button" className="btn btn-primary btn-block" disabled={busy} onClick={submit}>
                    <FiSend /> {schedule.template ? "שמירת תבנית" : "יצירת המבחן"}
                </button>
                {message && (
                    <div className="notice">
                        {message}
                        {success && <> · <Link className="underline" to="/teacher-exams">למבחנים שלי</Link></>}
                    </div>
                )}
            </div>
        </div>
    );
}
