import {useContext} from "react";
import {UserContext} from "../../context/UserContext.jsx";
import MessageList from "../../components/messages/MessageList.jsx";
import WeeklySchedule from "../../components/WeeklySchedule.jsx";
import {getGreeting} from "../../Utils/Greeting.jsx";
import EventForm from "../../components/EventForm.jsx";
import { FaCalendarAlt, FaClipboardList } from "react-icons/fa";
import DashboardHero, { QuickTile } from "../../components/ui/DashboardHero.jsx";

function  TeacherDashboard(){

      const { user } = useContext(UserContext);





      return (

          <div className="page" dir="rtl">
              <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 sm:gap-5">
                  <DashboardHero
                      className="lg:col-span-8"
                      eyebrow="Teacher · חדר מורים"
                      greeting={getGreeting()}
                      name={user?.username}
                      subtitle="השיעורים של היום, הודעות ואירועים — וכל מה שצריך כדי לבנות את המבחן הבא."
                  />

                  <div className="lg:col-span-4 grid grid-cols-2 lg:grid-cols-1 gap-4 sm:gap-5">
                      <QuickTile index={1} to="/test-configurator" icon={<FaClipboardList />} label="יצירת מבחן" hint="לתלמידים שלך" tone="icon-badge--mint" />
                      <QuickTile index={2} to="/teacher-weekly-schedule" icon={<FaCalendarAlt />} label="מערכת שעות" hint="כל השבוע" />
                  </div>

                  <div className="lg:col-span-5 lg:row-span-2 reveal" style={{ "--i": 3 }}>
                      <WeeklySchedule type="teacher" singleDayMode={true}/>
                  </div>

                  <div className="lg:col-span-7 reveal" style={{ "--i": 4 }}>
                      {user && (
                          <MessageList userId={user.userId}/>
                      )}
                  </div>

                  <div className="lg:col-span-7 reveal" style={{ "--i": 5 }}>
                      {user && (
                          <EventForm/>
                      )}
                  </div>
              </div>
          </div>

      )
}

export default TeacherDashboard
