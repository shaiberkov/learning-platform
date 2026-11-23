package org.example.learningprojectserver.exception;

import org.example.learningprojectserver.controller.SchoolManagerController;
import org.example.learningprojectserver.controller.userController;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.ClassRoomService;
import org.example.learningprojectserver.service.SchoolGradeService;
import org.example.learningprojectserver.service.SchoolManagerService;
import org.example.learningprojectserver.service.SchoolService;
import org.example.learningprojectserver.service.TeacherService;
import org.example.learningprojectserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.example.learningprojectserver.constants.ControllerConstants.SchoolManager.ADD_LESSON_TO_TEACHER;
import static org.example.learningprojectserver.constants.ControllerConstants.SchoolManager.SCHOOL_MANAGER_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.User.LOGIN;
import static org.example.learningprojectserver.constants.ControllerConstants.User.USER_BASE_PATH;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs the real controllers and the advice without a database: validation and error shapes only. */
class GlobalExceptionHandlerTest {

    private UserService userService;
    private TeacherService teacherService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        teacherService = mock(TeacherService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new userController(userService, mock(org.example.learningprojectserver.service.AuthTokenService.class),
                                mock(org.example.learningprojectserver.security.AuthCookies.class)),
                        new SchoolManagerController(mock(ClassRoomService.class), mock(SchoolGradeService.class),
                                mock(SchoolManagerService.class), mock(SchoolService.class), teacherService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void blankRequestParamReturnsProblemDetailInHebrew() throws Exception {
        mockMvc.perform(post(USER_BASE_PATH + LOGIN).param("userId", " ").param("password", "secret"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value(GlobalExceptionHandler.TYPE_VALIDATION.toString()))
                .andExpect(jsonPath("$.detail").value("יש להזין תעודת זהות"))
                .andExpect(jsonPath("$.errors.userId").value("יש להזין תעודת זהות"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("יש להזין תעודת זהות"));
        verifyNoInteractions(userService);
    }

    @Test
    void missingRequestParamReturnsProblemDetail() throws Exception {
        mockMvc.perform(post(USER_BASE_PATH + LOGIN).param("userId", "123456789"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("שדה חובה חסר"));
    }

    @Test
    void invalidRequestBodyReturnsFieldErrors() throws Exception {
        String body = "{\"subject\":\"\",\"dayOfWeek\":\"MONDAY\",\"startTime\":\"25:00\",\"endTime\":\"10:00\",\"classRoomName\":\"A1\"}";
        mockMvc.perform(post(SCHOOL_MANAGER_BASE_PATH + ADD_LESSON_TO_TEACHER).param("teacherId", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.VALIDATION_DETAIL))
                .andExpect(jsonPath("$.errors.subject").value("יש לבחור מקצוע"))
                .andExpect(jsonPath("$.errors.startTime").value("שעת ההתחלה צריכה להיות בפורמט HH:mm"));
        verifyNoInteractions(teacherService);
    }

    @Test
    void unexpectedExceptionReturns500WithoutLeakingInternals() throws Exception {
        when(userService.loginUser(anyString(), anyString())).thenThrow(new IllegalStateException("db password is hunter2"));
        mockMvc.perform(post(USER_BASE_PATH + LOGIN).param("userId", "123456789").param("password", "x"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.INTERNAL_DETAIL))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("hunter2"))));
    }

    @Test
    void validRequestKeepsBasicResponseShape() throws Exception {
        when(userService.loginUser("123456789", "pw")).thenReturn(new BasicResponse<>(true, null));
        mockMvc.perform(post(USER_BASE_PATH + LOGIN).param("userId", "123456789").param("password", "pw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
