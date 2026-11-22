
import { useContext } from "react";
import { UserContext } from "../../context/UserContext.jsx";
import MessageList from "../../components/messages/MessageList.jsx";
import WeeklySchedule from "../../components/WeeklySchedule.jsx";
import { getGreeting } from "../../Utils/Greeting.jsx";
import EventForm from "../../components/EventForm.jsx";
import { FaCalendarAlt, FaChartBar, FaClipboardList, FaProjectDiagram, FaPuzzlePiece } from "react-icons/fa";
import ReviewDueTile from "../../features/adaptive/ReviewDueTile.jsx";
import Prism from "../../components/ui/Prism.jsx";
import DashboardHero, { QuickTile } from "../../components/ui/DashboardHero.jsx";

function StudentDashboard() {
    const { user } = useContext(UserContext);

    if (user && !user.schoolCode) {
        return (
            <div className="page page--medium min-h-[60vh] grid place-items-center" dir="rtl">
                <div className="glass p-8 sm:p-12 text-center space-y-5 reveal">
                    <Prism size={64} orbits={false} className="mx-auto" />
                    <span className="eyebrow">ממתין לשיבוץ</span>
                    <h1 className="display text-2xl sm:text-4xl">
                        אינך משויך כרגע לשום בית ספר
                    </h1>
                </div>
            </div>
        );
    }

    return (
        <>
            <div className="page" dir="rtl">
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 sm:gap-5">
                    <DashboardHero
                        className="lg:col-span-8"
                        eyebrow="Student · מרחב אישי"
                        greeting={getGreeting()}
                        name={user?.username}
                        subtitle="היום שלך במבט אחד: שיעורים, הודעות ואירועים קרובים."
                    />

                    <div className="lg:col-span-4 reveal" style={{ "--i": 1 }}>
                        {user && (
                            <EventForm />
                        )}
                    </div>

                    <div className="lg:col-span-5 lg:row-span-2 reveal" style={{ "--i": 2 }}>
                        <WeeklySchedule type="student" singleDayMode={true} />
                    </div>

                    <div className="lg:col-span-7 reveal" style={{ "--i": 3 }}>
                        {user && (
                            <MessageList userId={user.userId} />
                        )}
                    </div>

                    <div className="lg:col-span-7 grid grid-cols-2 sm:grid-cols-4 gap-4 sm:gap-5">
                        <QuickTile index={4} to="/question-practice-selector" icon={<FaPuzzlePiece />} label="תרגול שאלות" hint="עם מרצה AI" />
                        <QuickTile index={5} to="/test-configurator" icon={<FaClipboardList />} label="תרגול מבחנים" hint="בקצב שלך" tone="icon-badge--mint" />
                        <QuickTile index={6} to="/teacher-tests" icon={<FaChartBar />} label="מבחנים" hint="ציונים ומועדים" tone="icon-badge--amber" />
                        <QuickTile index={7} to="/student-weekly-schedule" icon={<FaCalendarAlt />} label="מערכת שעות" hint="כל השבוע" />
                        <ReviewDueTile index={8} userId={user?.userId} />
                        <QuickTile index={9} to="/skill-map" icon={<FaProjectDiagram />} label="מפת מיומנויות" hint="שליטה לפי נושא" tone="icon-badge--mint" />
                    </div>
                </div>
            </div>
        </>
    );
}

export default StudentDashboard;
