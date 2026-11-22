package org.example.learningprojectserver.integration;

import org.example.learningprojectserver.entities.StudentEntity;
import org.example.learningprojectserver.entities.StudentProgressEntity;
import org.example.learningprojectserver.entities.StudentQuestionHistoryEntity;
import org.example.learningprojectserver.enums.Role;
import org.example.learningprojectserver.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.learningprojectserver.constants.ControllerConstants.Student.GENERATE_PRACTICE_TEST;
import static org.example.learningprojectserver.constants.ControllerConstants.Student.STUDENT_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.User.LOGIN;
import static org.example.learningprojectserver.constants.ControllerConstants.User.USER_BASE_PATH;
import static org.example.learningprojectserver.utils.GeneratorUtils.generateSalt;
import static org.example.learningprojectserver.utils.GeneratorUtils.hashPassword;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login, validation, the practice-test guard and the monitoring endpoints against a real MySQL whose
 * schema was built by Flyway (V1 baseline) and validated by Hibernate.
 */
class CriticalFlowsIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String STUDENT_ID = "000000018";
    private static final String PASSWORD = "Integration-Test-1";
    private static final String PHONE = "0500000018";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedStudent() throws Exception {
        Integer existing = jdbcTemplate.queryForObject(
                "select count(*) from user_entity where user_id = ?", Integer.class, STUDENT_ID);
        if (existing != null && existing > 0) {
            return;
        }
        String salt = generateSalt();
        StudentEntity student = new StudentEntity();
        student.setRole(Role.STUDENT);
        student.setUserId(STUDENT_ID);
        student.setUsername("integration-student");
        student.setEmail("integration-student@example.invalid");
        student.setPhoneNumber(PHONE);
        student.setSalt(salt);
        student.setPasswordHash(hashPassword(PASSWORD, salt));
        StudentQuestionHistoryEntity history = new StudentQuestionHistoryEntity();
        StudentProgressEntity progress = new StudentProgressEntity();
        history.setStudent(student);
        progress.setStudent(student);
        student.setQuestionHistoryEntity(history);
        student.setStudentProgressEntity(progress);
        studentRepository.save(student);
    }

    @Test
    void flywayAppliedTheBaseline() {
        Integer applied = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success = 1", Integer.class);
        assertThat(applied).isEqualTo(1);
    }

    @Test
    void loginWithCorrectPasswordSucceeds() throws Exception {
        mockMvc.perform(post(USER_BASE_PATH + LOGIN).param("userId", STUDENT_ID).param("password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void loginWithWrongPasswordFailsWithHebrewMessage() throws Exception {
        mockMvc.perform(post(USER_BASE_PATH + LOGIN).param("userId", STUDENT_ID).param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("הסיסמה שגויה"));
    }

    @Test
    void invalidInputIsRejectedAsProblemDetailThroughTheFullFilterChain() throws Exception {
        mockMvc.perform(post(USER_BASE_PATH + LOGIN).param("userId", "").param("password", PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.userId").exists());
    }

    @Test
    void practiceTestNeedsAClassRoom() throws Exception {
        mockMvc.perform(post(STUDENT_BASE_PATH + GENERATE_PRACTICE_TEST)
                        .param("userId", STUDENT_ID).param("subject", "math").param("topic", "addition")
                        .param("difficulty", "easy").param("questionCount", "5").param("timeLimitMinutes", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("תלמיד עוד לא משובץ לכיתה"));
    }

    @Test
    void practiceTestRejectsAnOutOfRangeQuestionCount() throws Exception {
        mockMvc.perform(post(STUDENT_BASE_PATH + GENERATE_PRACTICE_TEST)
                        .param("userId", STUDENT_ID).param("subject", "math").param("topic", "addition")
                        .param("difficulty", "easy").param("questionCount", "0").param("timeLimitMinutes", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.questionCount").value("מספר השאלות חייב להיות לפחות 1"));
    }

    @Test
    void healthIsPublicAndMetricsAreNot() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isForbidden());
    }
}
