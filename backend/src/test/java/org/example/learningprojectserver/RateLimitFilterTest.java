package org.example.learningprojectserver;

import org.example.learningprojectserver.filter.RateLimitFilter;
import org.example.learningprojectserver.security.RateLimitProperties;
import org.example.learningprojectserver.security.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.example.learningprojectserver.constants.ControllerConstants.Lesson.LESSON_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.User.LOGIN;
import static org.example.learningprojectserver.constants.ControllerConstants.User.USER_BASE_PATH;
import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private RateLimitProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        RateLimitProperties.Rule login = new RateLimitProperties.Rule();
        login.setIpCapacity(5);
        login.setUserCapacity(2);
        login.setPeriod(Duration.ofMinutes(15));
        properties.getRules().put(RateLimitFilter.RULE_LOGIN, login);
        filter = new RateLimitFilter(new RateLimiter(properties), properties);
    }

    private MockHttpServletResponse post(String path, String userId, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        if (userId != null) {
            request.setParameter("userId", userId);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void perUserLimitReturns429WithHebrewMessageAndRetryAfter() throws Exception {
        assertEquals(200, post(USER_BASE_PATH + LOGIN, "111", "10.0.0.1").getStatus());
        assertEquals(200, post(USER_BASE_PATH + LOGIN, "111", "10.0.0.1").getStatus());

        MockHttpServletResponse blocked = post(USER_BASE_PATH + LOGIN, "111", "10.0.0.1");
        assertEquals(429, blocked.getStatus());
        assertNotNull(blocked.getHeader("Retry-After"));
        String body = blocked.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body.contains("\"success\":false"));
        assertTrue(body.contains("יותר מדי בקשות"));

        // another user from the same IP is still allowed
        assertEquals(200, post(USER_BASE_PATH + LOGIN, "222", "10.0.0.1").getStatus());
    }

    @Test
    void perIpLimitAppliesAcrossUsers() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(200, post(USER_BASE_PATH + LOGIN, "u" + i, "10.0.0.9").getStatus());
        }
        assertEquals(429, post(USER_BASE_PATH + LOGIN, "u-new", "10.0.0.9").getStatus());
        assertEquals(200, post(USER_BASE_PATH + LOGIN, "u-new", "10.0.0.10").getStatus());
    }

    @Test
    void otherPathsAndDisabledLimiterAreNotLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(200, post(LESSON_BASE_PATH + "/x", "111", "10.0.0.2").getStatus());
        }
        properties.setEnabled(false);
        for (int i = 0; i < 10; i++) {
            assertEquals(200, post(USER_BASE_PATH + LOGIN, "333", "10.0.0.3").getStatus());
        }
    }
}
