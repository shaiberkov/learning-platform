import {Routes, Route, useLocation} from 'react-router-dom';
import {UserContext} from "../context/UserContext.jsx";
import SideBar from "../components/SideBar.jsx";
import PublicOnlyRoutesLayout from "../components/PublicOnlyRoutesLayout.jsx";
import React, {lazy, Suspense, useContext, useEffect, useRef} from "react";
import {useTranslation} from "react-i18next";
import Footer from "../components/Footer.jsx";
import ImpersonationBanner from "../components/ImpersonationBanner.jsx";
import AccommodationsSync from "../components/AccommodationsSync.jsx";
import LiveQuizBanner from "../components/LiveQuizBanner.jsx";
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

// Added by the improvement branches (exams, adaptive, engagement, AI, teacher tools, school admin, parents)
const AuditLog = lazy(() => import("../pages/admin/AuditLog.jsx"));
const SystemHealth = lazy(() => import("../pages/admin/SystemHealth.jsx"));
const FeatureFlags = lazy(() => import("../pages/admin/FeatureFlags.jsx"));
const Impersonate = lazy(() => import("../pages/admin/Impersonate.jsx"));
const ExamSession = lazy(() => import("../pages/student/ExamSession.jsx"));
const ExamBuilder = lazy(() => import("../pages/teacher/ExamBuilder.jsx"));
const TeacherExams = lazy(() => import("../pages/teacher/TeacherExams.jsx"));
const ExamMonitor = lazy(() => import("../pages/teacher/ExamMonitor.jsx"));
const ItemAnalysis = lazy(() => import("../pages/teacher/ItemAnalysis.jsx"));
const MistakesNotebook = lazy(() => import("../pages/student/MistakesNotebook.jsx"));
const SkillMap = lazy(() => import("../pages/student/SkillMap.jsx"));
const ClassSkillMap = lazy(() => import("../pages/teacher/ClassSkillMap.jsx"));
const HintEditor = lazy(() => import("../pages/teacher/HintEditor.jsx"));
const Achievements = lazy(() => import("../pages/student/Achievements.jsx"));
const StudyPlan = lazy(() => import("../pages/student/StudyPlan.jsx"));
const Lessons = lazy(() => import("../pages/student/Lessons.jsx"));
const LessonView = lazy(() => import("../pages/student/LessonView.jsx"));
const Homework = lazy(() => import("../pages/student/Homework.jsx"));
const AccessibilitySettings = lazy(() => import("../pages/student/AccessibilitySettings.jsx"));
const LessonAuthoring = lazy(() => import("../pages/teacher/LessonAuthoring.jsx"));
const HomeworkBoard = lazy(() => import("../pages/teacher/HomeworkBoard.jsx"));
const StudentAccommodations = lazy(() => import("../pages/teacher/StudentAccommodations.jsx"));
const ClassChallenges = lazy(() => import("../pages/teacher/ClassChallenges.jsx"));
const AiQuestionStudio = lazy(() => import("../pages/teacher/AiQuestionStudio.jsx"));
const AiGrading = lazy(() => import("../pages/teacher/AiGrading.jsx"));
const AiInsights = lazy(() => import("../pages/teacher/AiInsights.jsx"));
const AiTutorPage = lazy(() => import("../pages/student/AiTutorPage.jsx"));
const OpenQuestions = lazy(() => import("../pages/student/OpenQuestions.jsx"));
const AiSchoolOverview = lazy(() => import("../pages/manager/AiSchoolOverview.jsx"));
const QuestionBank = lazy(() => import("../pages/teacher/QuestionBank.jsx"));
const Gradebook = lazy(() => import("../pages/teacher/Gradebook.jsx"));
const LiveQuizHost = lazy(() => import("../pages/teacher/LiveQuizHost.jsx"));
const Attendance = lazy(() => import("../pages/teacher/Attendance.jsx"));
const LiveQuizPlay = lazy(() => import("../pages/student/LiveQuizPlay.jsx"));
const AttendanceReport = lazy(() => import("../pages/manager/AttendanceReport.jsx"));
const BulkImport = lazy(() => import("../pages/manager/BulkImport.jsx"));
const SchoolAnalytics = lazy(() => import("../pages/manager/SchoolAnalytics.jsx"));
const TimetableBuilder = lazy(() => import("../pages/manager/TimetableBuilder.jsx"));
const SchoolYears = lazy(() => import("../pages/manager/SchoolYears.jsx"));
const ParentLinks = lazy(() => import("../pages/manager/ParentLinks.jsx"));
const MessageParents = lazy(() => import("../pages/teacher/MessageParents.jsx"));
const ParentDashboard = lazy(() => import("../pages/parent/ParentDashboard.jsx"));
// [s01] Study rooms and focus mode
const StudyRooms = lazy(() => import("../pages/student/StudyRooms.jsx"));
const FocusMode = lazy(() => import("../pages/student/FocusMode.jsx"));
// [s01] end
// [s03] Duels and the daily challenge (student)
const Duels = lazy(() => import("../pages/student/Duels.jsx"));
const DailyChallenge = lazy(() => import("../pages/student/DailyChallenge.jsx"));
// [s04] Flashcards and the personal study notebook
const Flashcards = lazy(() => import("../pages/student/Flashcards.jsx"));
const StudyNotebook = lazy(() => import("../pages/student/StudyNotebook.jsx"));
// [s05] Summary sheet and study podcast (student).
const StudySummarySheet = lazy(() => import("../pages/student/StudySummarySheet.jsx"));
const StudyPodcast = lazy(() => import("../pages/student/StudyPodcast.jsx"));
// [s06] "Explain it to me" (Feynman) and mistake-type diagnosis
const ExplainIt = lazy(() => import("../pages/student/ExplainIt.jsx"));
const MistakeDiagnosis = lazy(() => import("../pages/student/MistakeDiagnosis.jsx"));
const ClassDiagnosis = lazy(() => import("../pages/teacher/ClassDiagnosis.jsx"));

// Public static pages
const TermsOfService = lazy(() => import("../pages/footer/TermsOfService.jsx"));
const PrivacyPolicy = lazy(() => import("../pages/footer/PrivacyPolicy.jsx"));
const Faq = lazy(() => import("../pages/footer/Faq.jsx"));
const About = lazy(() => import("../pages/footer/About.jsx"));
const AccessibilityStatement = lazy(() => import("../pages/footer/AccessibilityStatement.jsx"));

// [s02] Class forum and student-authored questions
const ClassForum = lazy(() => import("../pages/student/ClassForum.jsx"));
const PeerQuestions = lazy(() => import("../pages/student/PeerQuestions.jsx"));
const ClassForumModeration = lazy(() => import("../pages/teacher/ClassForumModeration.jsx"));
const PeerQuestionReview = lazy(() => import("../pages/teacher/PeerQuestionReview.jsx"));

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
                <AccommodationsSync />


            <main id="main-content" ref={mainRef} tabIndex={-1} className="flex-1 px-4 sm:px-6 lg:px-10 pt-28 sm:pt-32 outline-none">
                {user?.role === 'STUDENT' && user?.schoolCode && <LiveQuizBanner />}
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
                            <Route path="/audit-log" element={<AuditLog />} />
                            <Route path="/system-health" element={<SystemHealth />} />
                            <Route path="/feature-flags" element={<FeatureFlags />} />
                            <Route path="/impersonate" element={<Impersonate />} />
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
                            <Route path="/audit-log" element={<AuditLog />} />
                            <Route path="/ai-school" element={<AiSchoolOverview />} />
                            <Route path="/attendance-report" element={<AttendanceReport />} />
                            <Route path="/bulk-import" element={<BulkImport />} />
                            <Route path="/school-analytics" element={<SchoolAnalytics />} />
                            <Route path="/timetable-builder" element={<TimetableBuilder />} />
                            <Route path="/school-years" element={<SchoolYears />} />
                            <Route path="/parent-links" element={<ParentLinks />} />
                            <Route path="/message-parents" element={<MessageParents />} />
                            {commonRoutes}
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
                            <Route path="/message-parents" element={<MessageParents />} />
                            <Route path="/send-message" element={<SendMessage />} />
                            {/* [s02] class forum and student-authored questions */}
                            <Route path="/class-forum" element={<ClassForumModeration />} />
                            <Route path="/peer-question-review" element={<PeerQuestionReview />} />
                            {/* [s06] class mistake types and Feynman explanations */}
                            <Route path="/class-diagnosis" element={<ClassDiagnosis />} />
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
                            <Route path="/accessibility-settings" element={<AccessibilitySettings />} />
                            {/* [s01] Study rooms and focus mode */}
                            <Route path="/study-rooms" element={<StudyRooms />} />
                            <Route path="/focus-mode" element={<FocusMode />} />
                            {/* [s01] end */}
                            {/* [s02] class forum and student-authored questions */}
                            <Route path="/class-forum" element={<ClassForum />} />
                            <Route path="/peer-questions" element={<PeerQuestions />} />
                            {/* [s03] Duels and the daily challenge */}
                            <Route path="/duels" element={<Duels />} />
                            <Route path="/duels/:duelId" element={<Duels />} />
                            <Route path="/daily-challenge" element={<DailyChallenge />} />
                            {/* [s04] */}
                            <Route path="/flashcards" element={<Flashcards />} />
                            <Route path="/study-notebook" element={<StudyNotebook />} />
                            {/* [s05] Summary sheet and study podcast. */}
                            <Route path="/study-summary-sheet" element={<StudySummarySheet />} />
                            <Route path="/study-podcast" element={<StudyPodcast />} />
                            {/* [s06] "Explain it to me" (Feynman) and mistake-type diagnosis */}
                            <Route path="/explain-it" element={<ExplainIt />} />
                            <Route path="/mistake-diagnosis" element={<MistakeDiagnosis />} />
                            {commonRoutes}
                        </>
                    )}
                    {user?.role === 'PARENT' && (
                        <>
                            <Route path="/parent-dashboard" element={<ParentDashboard />} />
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
            <ImpersonationBanner />
        </div>
    );
}
