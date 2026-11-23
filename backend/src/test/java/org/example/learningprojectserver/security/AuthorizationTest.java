package org.example.learningprojectserver.security;

import org.example.learningprojectserver.config.SecurityConfig;
import org.example.learningprojectserver.controller.AuditLogController;
import org.example.learningprojectserver.controller.ImpersonationController;
import org.example.learningprojectserver.controller.SchoolManagerController;
import org.example.learningprojectserver.controller.StudentController;
import org.example.learningprojectserver.controller.SystemAdminController;
import org.example.learningprojectserver.controller.TeacherController;
import org.example.learningprojectserver.controller.TestController;
import org.example.learningprojectserver.controller.userController;
import org.example.learningprojectserver.repository.AccessQueryRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.AuditService;
import org.example.learningprojectserver.service.ClassRoomService;
import org.example.learningprojectserver.service.FeatureFlagService;
import org.example.learningprojectserver.service.ImpersonationService;
import org.example.learningprojectserver.service.JwtService;
import org.springframework.test.context.TestPropertySource;
import org.example.learningprojectserver.service.SchoolGradeService;
import org.example.learningprojectserver.service.SchoolManagerService;
import org.example.learningprojectserver.service.SchoolService;
import org.example.learningprojectserver.service.StudentService;
import org.example.learningprojectserver.service.SystemAdminService;
import org.example.learningprojectserver.service.TeacherService;
import org.example.learningprojectserver.service.TestResultService;
import org.example.learningprojectserver.service.TestService;
import org.example.learningprojectserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role enforcement (SecurityConfig) and ownership checks (AccessGuard) against real JWTs:
 * each test calls an endpoint about another user's resource and expects 403.
 */
@WebMvcTest(controllers = {
        StudentController.class, TeacherController.class, SchoolManagerController.class,
        SystemAdminController.class, TestController.class, userController.class,
        AuditLogController.class, ImpersonationController.class
})
@Import({SecurityConfig.class, JwtService.class, AccessGuard.class, FeatureFlags.class, AccessDeniedAdvice.class,
        AuthCookies.class, RateLimiter.class, RateLimitProperties.class})
// The production profile is active by default and JwtService refuses to start without a key there.
@TestPropertySource(properties = "JWT_SECRET=dGVzdC1vbmx5LWp3dC1zZWNyZXQtMzItYnl0ZXMtbG9uZyEh")
class AuthorizationTest {

    private static final String AUTH = "Authorization";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @MockitoBean private AccessQueryRepository accessQueryRepository;
    @MockitoBean private FeatureFlagService featureFlagService;
    @MockitoBean private StudentService studentService;
    @MockitoBean private TestResultService testResultService;
    @MockitoBean private TeacherService teacherService;
    @MockitoBean private ClassRoomService classRoomService;
    @MockitoBean private SchoolGradeService schoolGradeService;
    @MockitoBean private SchoolManagerService schoolManagerService;
    @MockitoBean private SchoolService schoolService;
    @MockitoBean private SystemAdminService systemAdminService;
    @MockitoBean private TestService testService;
    @MockitoBean private UserService userService;
    @MockitoBean private AuditService auditService;
    @MockitoBean private ImpersonationService impersonationService;
    @MockitoBean private org.example.learningprojectserver.service.AuthTokenService authTokenService;

    private String studentA;
    private String teacherT;
    private String managerM;
    private String admin;

    @BeforeEach
    void setUp() {
        studentA = bearer("111", "STUDENT");
        teacherT = bearer("222", "TEACHER");
        managerM = bearer("333", "SCHOOLMANAGER");
        admin = bearer("999", "SYSTEM_ADMIN");

        when(accessQueryRepository.findStudentSchoolCode("111")).thenReturn("S1");
        when(accessQueryRepository.findStudentSchoolCode("112")).thenReturn("S1");
        when(accessQueryRepository.findTeacherSchoolCode("222")).thenReturn("S1");
        when(accessQueryRepository.findManagerSchoolCode("333")).thenReturn("S1");
        when(accessQueryRepository.findTeacherSchoolCode("444")).thenReturn("S2");
        when(featureFlagService.flagsForSchool(anyString())).thenReturn(Map.of());
    }

    private String bearer(String userId, String role) {
        return "Bearer " + jwtService.generateToken(userId, "user" + userId, role);
    }

    // --- public endpoints still open ---

    @Test
    void loginStaysPublic() throws Exception {
        when(userService.loginUser("111", "pw")).thenReturn(new BasicResponse(true, null));
        mockMvc.perform(post("/Learning-App/user/login").param("userId", "111").param("password", "pw"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutTokenIs401() throws Exception {
        mockMvc.perform(get("/Learning-App/student/get-student-tests-status").param("studentId", "111"))
                .andExpect(status().isUnauthorized());
    }

    // --- role enforcement ---

    @Test
    void studentCannotCallSystemAdminEndpoints() throws Exception {
        mockMvc.perform(get("/Learning-App/system-admin/get-all-schools").header(AUTH, studentA))
                .andExpect(status().isForbidden());
        verify(systemAdminService, never()).getAllSchoolsDTO();
    }

    @Test
    void studentCannotCallSchoolManagerEndpoints() throws Exception {
        mockMvc.perform(get("/Learning-App/school-manager/get-all-students").param("schoolCode", "S1")
                        .header(AUTH, studentA))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotReadTheAuditLog() throws Exception {
        mockMvc.perform(get("/Learning-App/audit-log/list").header(AUTH, studentA))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotStartImpersonation() throws Exception {
        mockMvc.perform(post("/Learning-App/impersonation/start").param("targetUserId", "111")
                        .header(AUTH, managerM))
                .andExpect(status().isForbidden());
        verify(impersonationService, never()).start(anyString(), org.mockito.ArgumentMatchers.any());
    }

    // --- student: own exams and results only ---

    @Test
    void studentCannotReadAnotherStudentsTestResults() throws Exception {
        mockMvc.perform(get("/Learning-App/student/get-student-tests-status").param("studentId", "112")
                        .header(AUTH, studentA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(AccessDeniedAdvice.ACCESS_DENIED_MESSAGE));
        verify(studentService, never()).getStudentTestsStatus(anyString());
    }

    @Test
    void studentCanReadOwnTestResults() throws Exception {
        when(studentService.getStudentTestsStatus("111")).thenReturn(List.of());
        mockMvc.perform(get("/Learning-App/student/get-student-tests-status").param("studentId", "111")
                        .header(AUTH, studentA))
                .andExpect(status().isOk());
    }

    @Test
    void studentCannotStartATestNotAssignedToThem() throws Exception {
        when(accessQueryRepository.countTeacherTestAssignments(7L, "111")).thenReturn(0L);
        mockMvc.perform(post("/Learning-App/student/start-test").param("userId", "111").param("testId", "7")
                        .header(AUTH, studentA))
                .andExpect(status().isForbidden());
        verify(testResultService, never()).startTest(anyLong(), anyString());
    }

    @Test
    void studentCannotSubmitATeacherTestAsAnotherStudent() throws Exception {
        when(accessQueryRepository.countTeacherTestAssignments(7L, "112")).thenReturn(1L);
        mockMvc.perform(post("/Learning-App/teacher/check-teacher-test").param("userId", "112").param("testId", "7")
                        .contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header(AUTH, studentA))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCanSubmitTheirAssignedTeacherTest() throws Exception {
        when(accessQueryRepository.countTeacherTestAssignments(7L, "111")).thenReturn(1L);
        when(testResultService.checkTeacherTest(eq("111"), eq(7L), anyMap())).thenReturn(new BasicResponse(true, null));
        mockMvc.perform(post("/Learning-App/teacher/check-teacher-test").param("userId", "111").param("testId", "7")
                        .contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header(AUTH, studentA))
                .andExpect(status().isOk());
    }

    @Test
    void studentCannotOpenAnotherStudentsPracticeTest() throws Exception {
        when(accessQueryRepository.countTeacherTestAssignments(9L, "111")).thenReturn(0L);
        when(accessQueryRepository.countPracticeTestsOwnedBy(9L, "111")).thenReturn(0L);
        mockMvc.perform(get("/Learning-App/test/get-test").param("testId", "9").header(AUTH, studentA))
                .andExpect(status().isForbidden());
    }

    // --- teacher: own classes only ---

    @Test
    void teacherCannotReadAnotherTeachersSchedule() throws Exception {
        mockMvc.perform(get("/Learning-App/teacher/schedule-for-teacher").param("teacherId", "444")
                        .header(AUTH, teacherT))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotGiveATestToStudentsOutsideTheirClasses() throws Exception {
        when(accessQueryRepository.countStudentsInTeacherClasses(eq("222"), anyCollection())).thenReturn(0L);
        mockMvc.perform(generateTest("222", "[\"555\"]").header(AUTH, teacherT))
                .andExpect(status().isForbidden());
        verify(teacherService, never()).generateTestForStudents(anyList(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void teacherCanGiveATestToTheirOwnStudents() throws Exception {
        when(accessQueryRepository.countStudentsInTeacherClasses(eq("222"), anyCollection())).thenReturn(1L);
        when(teacherService.generateTestForStudents(anyList(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt(), anyInt())).thenReturn(new BasicResponse(true, null));
        mockMvc.perform(generateTest("222", "[\"111\"]").header(AUTH, teacherT))
                .andExpect(status().isOk());
    }

    @Test
    void teacherCannotCreateATestInAnotherTeachersName() throws Exception {
        mockMvc.perform(generateTest("444", "[\"111\"]").header(AUTH, teacherT))
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledFeatureIsRefusedWithHebrewMessage() throws Exception {
        when(accessQueryRepository.countStudentsInTeacherClasses(eq("222"), anyCollection())).thenReturn(1L);
        when(featureFlagService.flagsForSchool("S1")).thenReturn(Map.of("TEACHER_TESTS", false));
        mockMvc.perform(generateTest("222", "[\"111\"]").header(AUTH, teacherT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(org.hamcrest.Matchers.containsString("כבויה")));
    }

    // --- school manager: own school only ---

    @Test
    void managerCannotListAnotherSchoolsStudents() throws Exception {
        mockMvc.perform(get("/Learning-App/school-manager/get-all-students").param("schoolCode", "S2")
                        .header(AUTH, managerM))
                .andExpect(status().isForbidden());
        verify(schoolManagerService, never()).getAllStudents(anyString());
    }

    @Test
    void managerCanListOwnSchoolsStudents() throws Exception {
        when(schoolManagerService.getAllStudents("S1")).thenReturn(new BasicResponse(true, null));
        mockMvc.perform(get("/Learning-App/school-manager/get-all-students").param("schoolCode", "S1")
                        .header(AUTH, managerM))
                .andExpect(status().isOk());
    }

    @Test
    void managerCannotRemoveATeacherOfAnotherSchool() throws Exception {
        mockMvc.perform(post("/Learning-App/school-manager/remove-teacher-from-school").param("userId", "444")
                        .header(AUTH, managerM))
                .andExpect(status().isForbidden());
        verify(schoolManagerService, never()).removeTeacherFromSchool(anyString());
    }

    @Test
    void managerCannotReadAnotherManagersSchool() throws Exception {
        mockMvc.perform(get("/Learning-App/school-manager/get-school").param("schoolManagerId", "334")
                        .header(AUTH, managerM))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPassesOwnershipChecks() throws Exception {
        when(schoolManagerService.getAllStudents("S2")).thenReturn(new BasicResponse(true, null));
        mockMvc.perform(get("/Learning-App/school-manager/get-all-students").param("schoolCode", "S2")
                        .header(AUTH, admin))
                .andExpect(status().isOk());
    }

    // --- impersonation ---

    @Test
    void impersonationTokenActsAsTheTargetUser() throws Exception {
        String impersonating = "Bearer " + jwtService.generateImpersonationToken("111", "user111", "STUDENT", "999", "admin");
        mockMvc.perform(get("/Learning-App/system-admin/get-all-schools").header(AUTH, impersonating))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/Learning-App/student/get-student-tests-status").param("studentId", "112")
                        .header(AUTH, impersonating))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder generateTest(String teacherId, String studentsJson) {
        return post("/Learning-App/teacher/generate-test-for-students")
                .param("teacherId", teacherId)
                .param("testStartTime", "01/01/2030 10:00")
                .param("subject", "math")
                .param("topic", "t")
                .param("difficulty", "easy")
                .param("questionCount", "5")
                .param("timeLimitMinutes", "30")
                .contentType(MediaType.APPLICATION_JSON)
                .content(studentsJson);
    }
}
