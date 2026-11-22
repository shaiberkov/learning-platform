import {BrowserRouter, Routes, Route, useLocation, Link} from 'react-router-dom';
import Register from "../features/auth/Register.jsx";
import Login from "../features/auth/Login.jsx";
import TwoFactorAuthForm from "../features/auth/TwoFactorAuthForm.jsx";
import {UserContext, UserProvider} from "../context/UserContext.jsx";
import ProtectedRoute from "../components/ProtectedRoute.jsx";
import SideBar from "../components/SideBar.jsx";
import PublicOnlyRoutesLayout from "../components/PublicOnlyRoutesLayout.jsx";
import ResetPassword from "../features/auth/ResetPassword.jsx";
import SchoolRegistration from "../pages/admin/SchoolRegistration.jsx";
import RemoveSchoolManager from "../pages/admin/RemoveSchoolManager.jsx";
import React, {useContext} from "react";
import SystemAdminDashboard from "../pages/admin/SystemAdminDashboard.jsx";
import AssignTeacher from "../pages/manager/AssignTeacher.jsx";
import SchoolManagerDashboard from "../pages/manager/SchoolManagerDashboard.jsx";
import AddSchoolGrades from "../pages/manager/AddSchoolGrades.jsx";
import RemoveSchoolGrades from "../pages/manager/RemoveSchoolGrades.jsx";
import RemoveTeacherFromSchool from "../pages/manager/RemoveTeacherFromSchool.jsx";
import AddClassesToGrade from "../pages/manager/AddClassesToGrade.jsx";
import AddAdditionalClassToGrade from "../pages/manager/AddAdditionalClassToGrade.jsx";
import AddTeachingSubjectToTeacher from "../pages/manager/AddTeachingSubjectToTeacher.jsx";
import RemoveTeachingSubjectFromTeacher from "../pages/manager/RemoveTeachingSubjectFromTeacher.jsx";
import SendMessage from "../components/messages/SendMessage.jsx";
import CreateLesson from "../pages/manager/CreateLesson.jsx";
import AsignTeacherToClassRoom from "../pages/manager/AsignTeacherToClassRooms.jsx";
import AsignTeacherToClassRooms from "../pages/manager/AsignTeacherToClassRooms.jsx";
import TeacherWeeklySchedule from "../pages/teacher/TeacherWeeklySchedule.jsx";
import AssignStudentToClass from "../pages/manager/AssignStudentToClass.jsx";
import StudentWeeklySchedule from "../pages/student/StudentWeeklySchedule.jsx";
import ClassScheduleViewerForManager from "../pages/manager/ClassScheduleViewerForManager.jsx";
import QuestionPracticeSelector from "../components/QuestionPracticeSelector.jsx";
import QuestionPractice from "../pages/student/QuestionPractice.jsx";
import TestConfigurator from "../components/TestConfigurator.jsx";
import TestSession from "../components/TestSession.jsx";
import TestsTable from "../pages/student/TestsTable.jsx";
import TeacherDashboard from "../pages/teacher/TeacherDashboard.jsx";
import StudentDashboard from "../pages/student/StudentDashboard.jsx";
import TermsOfService from "../pages/footer/TermsOfService.jsx";
import PrivacyPolicy from "../pages/footer/PrivacyPolicy.jsx";
import Faq from "../pages/footer/Faq.jsx";
import About from "../pages/footer/About.jsx";
import Footer from "../components/Footer.jsx";
import AuditLog from "../pages/admin/AuditLog.jsx";
import SystemHealth from "../pages/admin/SystemHealth.jsx";
import FeatureFlags from "../pages/admin/FeatureFlags.jsx";
import Impersonate from "../pages/admin/Impersonate.jsx";
import ImpersonationBanner from "../components/ImpersonationBanner.jsx";
import ExamSession from "../pages/student/ExamSession.jsx";
import ExamBuilder from "../pages/teacher/ExamBuilder.jsx";
import TeacherExams from "../pages/teacher/TeacherExams.jsx";
import ExamMonitor from "../pages/teacher/ExamMonitor.jsx";
import ItemAnalysis from "../pages/teacher/ItemAnalysis.jsx";
import MistakesNotebook from "../pages/student/MistakesNotebook.jsx";
import SkillMap from "../pages/student/SkillMap.jsx";
import ClassSkillMap from "../pages/teacher/ClassSkillMap.jsx";
import HintEditor from "../pages/teacher/HintEditor.jsx";
import AccommodationsSync from "../components/AccommodationsSync.jsx";
import Achievements from "../pages/student/Achievements.jsx";
import StudyPlan from "../pages/student/StudyPlan.jsx";
import Lessons from "../pages/student/Lessons.jsx";
import LessonView from "../pages/student/LessonView.jsx";
import Homework from "../pages/student/Homework.jsx";
import AccessibilitySettings from "../pages/student/AccessibilitySettings.jsx";
import LessonAuthoring from "../pages/teacher/LessonAuthoring.jsx";
import HomeworkBoard from "../pages/teacher/HomeworkBoard.jsx";
import StudentAccommodations from "../pages/teacher/StudentAccommodations.jsx";
import ClassChallenges from "../pages/teacher/ClassChallenges.jsx";
import AiQuestionStudio from "../pages/teacher/AiQuestionStudio.jsx";
import AiGrading from "../pages/teacher/AiGrading.jsx";
import AiInsights from "../pages/teacher/AiInsights.jsx";
import AiTutorPage from "../pages/student/AiTutorPage.jsx";
import OpenQuestions from "../pages/student/OpenQuestions.jsx";
import AiSchoolOverview from "../pages/manager/AiSchoolOverview.jsx";
import QuestionBank from "../pages/teacher/QuestionBank.jsx";
import Gradebook from "../pages/teacher/Gradebook.jsx";
import LiveQuizHost from "../pages/teacher/LiveQuizHost.jsx";
import Attendance from "../pages/teacher/Attendance.jsx";
import LiveQuizPlay from "../pages/student/LiveQuizPlay.jsx";
import AttendanceReport from "../pages/manager/AttendanceReport.jsx";
import LiveQuizBanner from "../components/LiveQuizBanner.jsx";
export default function AppRoutes() {

    const {user}=useContext(UserContext)
    const location = useLocation();


    return (
        <div className="flex flex-col min-h-screen text-ink">


                <SideBar />
                <AccommodationsSync />


            <main className="flex-1 px-4 sm:px-6 lg:px-10 pt-28 sm:pt-32">
                {user?.role === 'STUDENT' && user?.schoolCode && <LiveQuizBanner />}
                <div key={location.pathname} className="page-enter">
                <Routes>
                    <Route element={<PublicOnlyRoutesLayout />}>
                        <Route path="/login" element={<Login />} />
                        <Route path="/register" element={<Register />} />
                        <Route path="/two-factor-auth" element={<TwoFactorAuthForm />} />
                        <Route path="/reset-password" element={<ResetPassword />} />
                    </Route>

                    {user?.role === 'SYSTEM_ADMIN' && (
                        <>
                            <Route path="/system-admin-dashboard" element={<SystemAdminDashboard />} />
                            <Route path="/school-registration" element={<SchoolRegistration />} />
                            <Route path="/remove-manager-from-school" element={<RemoveSchoolManager />} />
                            <Route path="/send-message" element={<SendMessage />} />
                            <Route path="/audit-log" element={<AuditLog />} />
                            <Route path="/system-health" element={<SystemHealth />} />
                            <Route path="/feature-flags" element={<FeatureFlags />} />
                            <Route path="/impersonate" element={<Impersonate />} />
                        </>
                    )}

                    {user?.role === 'SCHOOLMANAGER' && (
                        <>
                            <Route path="/school-mananger-dashboard" element={<SchoolManagerDashboard />} />
                            <Route path="/assign-teacher" element={<AssignTeacher />} />
                            <Route path="/remove-teacher" element={<RemoveTeacherFromSchool />} />
                            <Route path="/add-school-grades" element={<AddSchoolGrades />} />
                            <Route path="/remove-school-grades" element={<RemoveSchoolGrades />} />
                            <Route path="/add-classes-to-grade" element={<AddClassesToGrade />} />
                            <Route path="/add-additional-class-to-grade" element={<AddAdditionalClassToGrade />} />
                            <Route path="/add-teaching-subject-to-teacher" element={<AddTeachingSubjectToTeacher />} />
                            <Route path="/remove-teaching-subject-to-teacher" element={<RemoveTeachingSubjectFromTeacher />} />
                            <Route path="/send-message" element={<SendMessage />} />
                            <Route path="/assign-lesson-to-teacher" element={<CreateLesson />} />
                            <Route path="/assign-teacher-to-class" element={<AsignTeacherToClassRooms />} />
                            <Route path="/assign-student-to-class" element={<AssignStudentToClass />} />
                            <Route path="/classes-weekly-schedule" element={<ClassScheduleViewerForManager />} />
                            <Route path="/audit-log" element={<AuditLog />} />
                            <Route path="/ai-school" element={<AiSchoolOverview />} />
                            <Route path="/attendance-report" element={<AttendanceReport />} />
                        </>
                    )}

                    {user?.role === 'TEACHER' && (
                        <>
                            <Route path="/teacher-dashboard" element={<TeacherDashboard />} />
                            <Route path="/teacher-weekly-schedule" element={<TeacherWeeklySchedule />} />
                            <Route path="/test-configurator" element={<TestConfigurator type="teacher" />} />
                            <Route path="/exam-builder" element={<ExamBuilder />} />
                            <Route path="/teacher-exams" element={<TeacherExams />} />
                            <Route path="/teacher-exams/:testId/monitor" element={<ExamMonitor />} />
                            <Route path="/teacher-exams/:testId/analysis" element={<ItemAnalysis />} />
                            <Route path="/class-skill-map" element={<ClassSkillMap />} />
                            <Route path="/hint-editor" element={<HintEditor />} />
                            <Route path="/lesson-authoring" element={<LessonAuthoring />} />
                            <Route path="/homework-board" element={<HomeworkBoard />} />
                            <Route path="/class-challenges" element={<ClassChallenges />} />
                            <Route path="/student-accommodations" element={<StudentAccommodations />} />
                            <Route path="/ai-questions" element={<AiQuestionStudio />} />
                            <Route path="/ai-grading" element={<AiGrading />} />
                            <Route path="/ai-insights" element={<AiInsights />} />
                            <Route path="/question-bank" element={<QuestionBank />} />
                            <Route path="/gradebook" element={<Gradebook />} />
                            <Route path="/live-quiz-host" element={<LiveQuizHost />} />
                            <Route path="/attendance" element={<Attendance />} />
                        </>
                    )}

                    {user?.role === 'STUDENT'&&user?.schoolCode && (
                        <>
                            <Route path="/student-dashboard" element={<StudentDashboard />} />
                            <Route path="/student-weekly-schedule" element={<StudentWeeklySchedule />} />
                            <Route path="/question-practice-selector" element={<QuestionPracticeSelector />} />
                            <Route path="/test-configurator" element={<TestConfigurator type="student" />} />
                            <Route path="/teacher-tests" element={<TestsTable />} />
                            <Route path="/mistakes-notebook" element={<MistakesNotebook />} />
                            <Route path="/skill-map" element={<SkillMap />} />
                            <Route path="/ai-tutor" element={<AiTutorPage />} />
                            <Route path="/open-questions" element={<OpenQuestions />} />
                            <Route path="/live-quiz" element={<LiveQuizPlay />} />
                            <Route path="/exercises/:subjectName/:topicName/:exerciseName" element={<QuestionPractice />} />
                            <Route path="/test/:selectedSubject/:selectedTopic/:selectedDifficulty/:selectedQuestionCount/:selectedTimeMinutes" element={<TestSession type="practiceTest" />} />
                            <Route path="/test/:TeacherTestId/:timeLimitMinutes" element={<TestSession type="teacherTest" />} />
                            <Route path="/exam/:testId" element={<ExamSession />} />
                            <Route path="/lessons" element={<Lessons />} />
                            <Route path="/lessons/:lessonId" element={<LessonView />} />
                            <Route path="/homework" element={<Homework />} />
                            <Route path="/study-plan" element={<StudyPlan />} />
                            <Route path="/achievements" element={<Achievements />} />
                            <Route path="/accessibility" element={<AccessibilitySettings />} />
                        </>
                    )}
                    {user?.role === 'STUDENT'&&!user?.schoolCode && (
                        <>
                            <Route path="/student-dashboard" element={<StudentDashboard />} />
                        </>
                    )}

                    <Route path="/about" element={<About />} />
                    <Route path="/faq" element={<Faq />} />
                    <Route path="/privacy-policy" element={<PrivacyPolicy />} />
                    <Route path="/terms-of-service" element={<TermsOfService />} />
                </Routes>
                </div>
            </main>

            <Footer />
            <ImpersonationBanner />
        </div>
    );
}
