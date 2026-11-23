package org.example.learningprojectserver.security;

import org.example.learningprojectserver.config.SecurityConfig;
import org.example.learningprojectserver.controller.PortfolioController;
import org.example.learningprojectserver.controller.PortfolioPublicController;
import org.example.learningprojectserver.controller.QuestController;
import org.example.learningprojectserver.dto.PortfolioDTOs;
import org.example.learningprojectserver.repository.AccessQueryRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.FeatureFlagService;
import org.example.learningprojectserver.service.JwtService;
import org.example.learningprojectserver.service.portfolio.PortfolioService;
import org.example.learningprojectserver.service.quest.QuestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** [s09] Quests and portfolio take the student from the token; only GET /portfolio-public/{token} is public. */
@WebMvcTest(controllers = {QuestController.class, PortfolioController.class, PortfolioPublicController.class})
@Import({SecurityConfig.class, JwtService.class, AccessGuard.class, FeatureFlags.class, AccessDeniedAdvice.class,
        AuthCookies.class, RateLimiter.class, RateLimitProperties.class})
@TestPropertySource(properties = "JWT_SECRET=dGVzdC1vbmx5LWp3dC1zZWNyZXQtMzItYnl0ZXMtbG9uZyEh")
class QuestPortfolioSecurityTest {

    private static final String AUTH = "Authorization";
    private static final String BASE = "/Learning-App";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @MockitoBean private AccessQueryRepository accessQueryRepository;
    @MockitoBean private FeatureFlagService featureFlagService;
    @MockitoBean private QuestService questService;
    @MockitoBean private PortfolioService portfolioService;
    @MockitoBean private org.example.learningprojectserver.service.AuthTokenService authTokenService;

    private String bearer(String userId, String role) {
        return "Bearer " + jwtService.generateToken(userId, "user" + userId, role);
    }

    @Test
    void portfolioNeedsLogin() throws Exception {
        mockMvc.perform(get(BASE + "/student/portfolio/me")).andExpect(status().isUnauthorized());
        verify(portfolioService, never()).getMine(anyString());
    }

    @Test
    void portfolioUsesTheStudentFromTheToken() throws Exception {
        when(portfolioService.getMine("111")).thenReturn(new BasicResponse<>(true, ""));
        mockMvc.perform(get(BASE + "/student/portfolio/me").param("userId", "999").header(AUTH, bearer("111", "STUDENT")))
                .andExpect(status().isOk());
        verify(portfolioService).getMine("111");
    }

    @Test
    void teachersAndPlainAdminsCannotUseStudentScreens() throws Exception {
        mockMvc.perform(get(BASE + "/student/quests/list").header(AUTH, bearer("222", "TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/student/quests/list").header(AUTH, bearer("999", "SYSTEM_ADMIN")))
                .andExpect(status().isForbidden());
        verify(questService, never()).listQuests(anyString());
    }

    @Test
    void sharedPortfolioIsPublicByToken() throws Exception {
        String token = "a".repeat(64);
        when(portfolioService.getPublic(token)).thenReturn(new PortfolioDTOs.PublicPortfolio("נועה", "ח'", 2, 250, "🙂",
                List.of(), List.of(), List.of(), List.of(), LocalDate.now()));
        mockMvc.perform(get(BASE + "/portfolio-public/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("נועה"));
    }

    @Test
    void unknownShareTokenIs404() throws Exception {
        mockMvc.perform(get(BASE + "/portfolio-public/" + "b".repeat(64))).andExpect(status().isNotFound());
    }

    @Test
    void onlyTheTokenPathIsOpen() throws Exception {
        mockMvc.perform(post(BASE + "/portfolio-public/" + "a".repeat(64))).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/portfolio-public/a/b")).andExpect(status().isUnauthorized());
    }
}
