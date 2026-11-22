import {Routes, Route, useLocation} from 'react-router-dom';
import {UserContext} from "../context/UserContext.jsx";
import SideBar from "../components/SideBar.jsx";
import PublicOnlyRoutesLayout from "../components/PublicOnlyRoutesLayout.jsx";
import React, {lazy, Suspense, useContext, useEffect, useRef} from "react";
import {useTranslation} from "react-i18next";
import Footer from "../components/Footer.jsx";
import {PageSkeleton} from "../components/ui/Skeleton.jsx";

// Code splitting: every page is its own chunk, so a role downloads only the screens it opens.
// Auth screens stay in their own small chunks too; the shell (navbar, footer) is eager.
const Register = lazy(() => import("../features/auth/Register.jsx"));
const Login = lazy(() => import("../features/auth/Login.jsx"));
const TwoFactorAuthForm = lazy(() => import("../features/auth/TwoFactorAuthForm.jsx"));
const ResetPassword = lazy(() => import("../features/auth/ResetPassword.jsx"));

// System admin
const SystemAdminDashboard = lazy(() => import("../pages/admin/SystemAdminDashboard.jsx"));
const SchoolRegistration = lazy(() => import("../pages/admin/SchoolRegistration.jsx"));
const RemoveSchoolManager = lazy(() => import("../pages/admin/RemoveSchoolManager.jsx"));

// School manager
const SchoolManagerDashboard = lazy(() => import("../pages/manager/SchoolManagerDashboard.jsx"));
const AssignTeacher = lazy(() => import("../pages/manager/AssignTeacher.jsx"));
const AddSchoolGrades = lazy(() => import("../pages/manager/AddSchoolGrades.jsx"));
const RemoveSchoolGrades = lazy(() => import("../pages/manager/RemoveSchoolGrades.jsx"));
const RemoveTeacherFromSchool = lazy(() => import("../pages/manager/RemoveTeacherFromSchool.jsx"));
const AddClassesToGrade = lazy(() => import("../pages/manager/AddClassesToGrade.jsx"));
const AddAdditionalClassToGrade = lazy(() => import("../pages/manager/AddAdditionalClassToGrade.jsx"));
const AddTeachingSubjectToTeacher = lazy(() => import("../pages/manager/AddTeachingSubjectToTeacher.jsx"));
const RemoveTeachingSubjectFromTeacher = lazy(() => import("../pages/manager/RemoveTeachingSubjectFromTeacher.jsx"));
const CreateLesson = lazy(() => import("../pages/manager/CreateLesson.jsx"));
const AsignTeacherToClassRooms = lazy(() => import("../pages/manager/AsignTeacherToClassRooms.jsx"));
const AssignStudentToClass = lazy(() => import("../pages/manager/AssignStudentToClass.jsx"));
const ClassScheduleViewerForManager = lazy(() => import("../pages/manager/ClassScheduleViewerForManager.jsx"));

// Teacher
const TeacherDashboard = lazy(() => import("../pages/teacher/TeacherDashboard.jsx"));
const TeacherWeeklySchedule = lazy(() => import("../pages/teacher/TeacherWeeklySchedule.jsx"));

// Student
const StudentDashboard = lazy(() => import("../pages/student/StudentDashboard.jsx"));
const StudentWeeklySchedule = lazy(() => import("../pages/student/StudentWeeklySchedule.jsx"));
const QuestionPracticeSelector = lazy(() => import("../components/QuestionPracticeSelector.jsx"));
const QuestionPractice = lazy(() => import("../pages/student/QuestionPractice.jsx"));
const TestsTable = lazy(() => import("../pages/student/TestsTable.jsx"));
const TestSession = lazy(() => import("../components/TestSession.jsx"));

// Shared between roles
const SendMessage = lazy(() => import("../components/messages/SendMessage.jsx"));
const TestConfigurator = lazy(() => import("../components/TestConfigurator.jsx"));
const MessagesPage = lazy(() => import("../pages/common/MessagesPage.jsx"));
const SettingsPage = lazy(() => import("../pages/common/SettingsPage.jsx"));

// Public static pages
const TermsOfService = lazy(() => import("../pages/footer/TermsOfService.jsx"));
const PrivacyPolicy = lazy(() => import("../pages/footer/PrivacyPolicy.jsx"));
const Faq = lazy(() => import("../pages/footer/Faq.jsx"));
const About = lazy(() => import("../pages/footer/About.jsx"));
const AccessibilityStatement = lazy(() => import("../pages/footer/AccessibilityStatement.jsx"));

export default function AppRoutes() {

    const {user}=useContext(UserContext)
    const location = useLocation();
    const { t } = useTranslation();
    const mainRef = useRef(null);
    const firstRender = useRef(true);

    // On route change, move focus to the main region so screen-reader and keyboard users
    // start at the new page instead of the link they clicked.
    useEffect(() => {
        if (firstRender.current) {
            firstRender.current = false;
            return;
        }
        mainRef.current?.focus({ preventScroll: true });
    }, [location.pathname]);

    const commonRoutes = (
        <>
            <Route path="/messages" element={<MessagesPage />} />
            <Route path="/settings" element={<SettingsPage />} />
        </>
    );

    return (
        <div className="flex flex-col min-h-screen text-ink">

                <a href="#main-content" className="skip-link">{t("a11y.skipToContent")}</a>

                <SideBar />


            <main id="main-content" ref={mainRef} tabIndex={-1} className="flex-1 px-4 sm:px-6 lg:px-10 pt-28 sm:pt-32 outline-none">
                <div key={location.pathname} className="page-enter">
                <Suspense fallback={<PageSkeleton />}>
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
                            {commonRoutes}
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
                            {commonRoutes}
                        </>
                    )}

                    {user?.role === 'TEACHER' && (
                        <>
                            <Route path="/teacher-dashboard" element={<TeacherDashboard />} />
                            <Route path="/teacher-weekly-schedule" element={<TeacherWeeklySchedule />} />
                            <Route path="/test-configurator" element={<TestConfigurator type="teacher" />} />
                            <Route path="/send-message" element={<SendMessage />} />
                            {commonRoutes}
                        </>
                    )}

                    {user?.role === 'STUDENT'&&user?.schoolCode && (
                        <>
                            <Route path="/student-dashboard" element={<StudentDashboard />} />
                            <Route path="/student-weekly-schedule" element={<StudentWeeklySchedule />} />
                            <Route path="/question-practice-selector" element={<QuestionPracticeSelector />} />
                            <Route path="/test-configurator" element={<TestConfigurator type="student" />} />
                            <Route path="/teacher-tests" element={<TestsTable />} />
                            <Route path="/exercises/:subjectName/:topicName/:exerciseName" element={<QuestionPractice />} />
                            <Route path="/test/:selectedSubject/:selectedTopic/:selectedDifficulty/:selectedQuestionCount/:selectedTimeMinutes" element={<TestSession type="practiceTest" />} />
                            <Route path="/test/:TeacherTestId/:timeLimitMinutes" element={<TestSession type="teacherTest" />} />
                            {commonRoutes}
                        </>
                    )}
                    {user?.role === 'STUDENT'&&!user?.schoolCode && (
                        <>
                            <Route path="/student-dashboard" element={<StudentDashboard />} />
                            {commonRoutes}
                        </>
                    )}

                    <Route path="/about" element={<About />} />
                    <Route path="/faq" element={<Faq />} />
                    <Route path="/privacy-policy" element={<PrivacyPolicy />} />
                    <Route path="/terms-of-service" element={<TermsOfService />} />
                    <Route path="/accessibility" element={<AccessibilityStatement />} />
                </Routes>
                </Suspense>
                </div>
            </main>

            <Footer />
        </div>
    );
}
