import {useContext, useEffect, useState} from "react";
import {UserContext} from "../../context/UserContext.jsx";
import DashboardHero from "../../components/ui/DashboardHero.jsx";
import MessageList from "../../components/messages/MessageList.jsx";
import BarChart from "../../components/charts/BarChart.jsx";
import {getGreeting} from "../../Utils/Greeting.jsx";
import {parentGet} from "../../features/school/schoolApi.js";
import {CHILD_OVERVIEW, GET_CHILDREN} from "../../constants/pages.constants.js";

function Stat({label, value, tone = ""}) {
    return (
        <div className="glass--sunk p-4">
            <span className="faint text-xs">{label}</span>
            <p className={`num text-3xl font-semibold mt-2 ${tone}`}>{value ?? "—"}</p>
        </div>
    );
}

function ParentDashboard() {
    const {user} = useContext(UserContext);
    const [children, setChildren] = useState([]);
    const [childId, setChildId] = useState("");
    const [overview, setOverview] = useState(null);
    const [message, setMessage] = useState("");

    useEffect(() => {
        if (!user?.userId) return;
        parentGet(GET_CHILDREN, {parentId: user.userId}).then((res) => {
            setMessage(res.errorCode || "");
            if (res.success) {
                setChildren(res.data);
                if (res.data.length > 0) setChildId(res.data[0].userId);
            }
        });
    }, [user?.userId]);

    useEffect(() => {
        if (!childId) return;
        setOverview(null);
        parentGet(CHILD_OVERVIEW, {parentId: user.userId, childId}).then((res) => {
            if (res.success) setOverview(res.data);
            else setMessage(res.errorCode);
        });
    }, [childId, user?.userId]);

    const skills = overview?.skillLevels ? Object.entries(overview.skillLevels) : [];

    return (
        <div className="page" dir="rtl">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 sm:gap-5 items-stretch">
                <DashboardHero
                    className="lg:col-span-8"
                    eyebrow="Parent · הורה"
                    greeting={getGreeting()}
                    name={user?.username}
                    subtitle="מעקב אחר ההתקדמות, הציונים וההודעות של הילדים"
                />

                <div className="lg:col-span-4 glass p-6 reveal" style={{"--i": 1}}>
                    <span className="eyebrow">הילדים שלי</span>
                    <div className="flex flex-wrap gap-2 mt-3">
                        {children.map((c) => (
                            <button key={c.userId} onClick={() => setChildId(c.userId)}
                                    className={`choice ${c.userId === childId ? "is-active" : ""}`}>
                                {c.username}{c.className ? ` · ${c.className}` : ""}
                            </button>
                        ))}
                    </div>
                    {message && <p className="notice mt-3">{message}</p>}
                </div>

                {overview && (
                    <>
                        <div className="lg:col-span-12 glass p-6 reveal grid grid-cols-2 md:grid-cols-5 gap-3" style={{"--i": 2}}>
                            <Stat label="ממוצע מבחני מורים" value={overview.teacherTestsAverage} tone="text-accent"/>
                            <Stat label="מבחנים שנעשו" value={overview.testResults.length}/>
                            <Stat label="מבחנים שהוקצו" value={overview.assignedTestsCount}/>
                            <Stat label="מבחני תרגול עצמי" value={overview.practiceTestsCount}/>
                            <Stat label="ממוצע תרגול עצמי" value={overview.practiceTestsAverage} tone="text-mint"/>
                        </div>

                        <div className="lg:col-span-6 glass p-6 reveal" style={{"--i": 3}}>
                            <span className="eyebrow">Grades by subject</span>
                            <h3 className="title text-lg mt-1 mb-4">ממוצע לפי מקצוע</h3>
                            <BarChart data={overview.averageBySubject}/>
                        </div>

                        <div className="lg:col-span-6 glass p-6 reveal" style={{"--i": 4}}>
                            <span className="eyebrow">Attendance</span>
                            <h3 className="title text-lg mt-1 mb-4">נוכחות</h3>
                            {overview.attendanceAvailable
                                ? null
                                : <p className="faint text-sm">נתוני נוכחות עדיין לא נאספים במערכת. הם יוצגו כאן כשבית הספר יתחיל לרשום נוכחות.</p>}
                            {skills.length > 0 && (
                                <>
                                    <h3 className="title text-lg mt-6 mb-3">רמת מיומנות בתרגול</h3>
                                    <div className="flex flex-wrap gap-2">
                                        {skills.map(([topic, level]) => (
                                            <span key={topic} className="chip">{topic} · <span className="num">{level}</span></span>
                                        ))}
                                    </div>
                                </>
                            )}
                        </div>

                        <div className="lg:col-span-12 glass p-6 reveal" style={{"--i": 5}}>
                            <h3 className="title text-lg mb-3">ציוני מבחנים</h3>
                            {overview.testResults.length === 0
                                ? <p className="faint text-sm">עדיין אין ציונים.</p>
                                : (
                                    <div className="overflow-x-auto">
                                        <table className="data-table">
                                            <thead><tr><th>תאריך</th><th>מקצוע</th><th>נושא</th><th>מורה</th><th>ציון</th></tr></thead>
                                            <tbody>
                                            {overview.testResults.map((r, i) => (
                                                <tr key={i}>
                                                    <td className="num">{r.testStartTime}</td>
                                                    <td>{r.subject}</td>
                                                    <td>{r.topic}</td>
                                                    <td>{r.teacherName}</td>
                                                    <td className="num font-semibold">{r.score}</td>
                                                </tr>
                                            ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                        </div>
                    </>
                )}

                {user && (
                    <div className="lg:col-span-12 reveal" style={{"--i": 6}}>
                        <MessageList userId={user.userId}/>
                    </div>
                )}
            </div>
        </div>
    );
}

export default ParentDashboard;
