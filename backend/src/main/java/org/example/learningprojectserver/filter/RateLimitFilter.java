package org.example.learningprojectserver.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.RateLimitProperties;
import org.example.learningprojectserver.security.RateLimiter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.example.learningprojectserver.constants.ControllerConstants.ChatGpt.*;
import static org.example.learningprojectserver.constants.ControllerConstants.User.*;
import static org.example.learningprojectserver.constants.SecurityConstants.JwtAuthenticationFilterConstants.PRINCIPAL_USER_ID_KEY;

/**
 * Applies {@link RateLimiter} to the abuse-prone endpoints: login, OTP send/verify, password reset
 * and the AI chat. Over the limit → 429 with a Hebrew {@link BasicResponse} and a Retry-After header.
 * Runs after the JWT filter, so a logged-in user is limited by their userId from the token.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    public static final String RULE_LOGIN = "login";
    public static final String RULE_OTP_SEND = "otp-send";
    public static final String RULE_OTP_VERIFY = "otp-verify";
    public static final String RULE_PASSWORD_RESET = "password-reset";
    public static final String RULE_AI_CHAT = "ai-chat";

    private static final Map<String, String> RULE_BY_PATH = Map.of(
            USER_BASE_PATH + LOGIN, RULE_LOGIN,
            USER_BASE_PATH + SEND_OTP, RULE_OTP_SEND,
            USER_BASE_PATH + VERIFY_OTP, RULE_OTP_VERIFY,
            USER_BASE_PATH + FORGOT_PASSWORD, RULE_PASSWORD_RESET,
            USER_BASE_PATH + RESET_PASSWORD, RULE_PASSWORD_RESET,
            CHAT_BASE_PATH + WITH_MEMORY, RULE_AI_CHAT,
            CHAT_BASE_PATH + SINGLE_RESPONSE, RULE_AI_CHAT
    );

    private static final String TOO_MANY_REQUESTS_MESSAGE = "יותר מדי בקשות. נסו שוב בעוד %d שניות.";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;

    public RateLimitFilter(RateLimiter rateLimiter, RateLimitProperties properties) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !RULE_BY_PATH.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String rule = RULE_BY_PATH.get(request.getRequestURI());
        long waitSeconds = rateLimiter.tryConsume(rule, clientIp(request), userKey(request));
        if (waitSeconds == 0) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(waitSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        BasicResponse<Void> body = new BasicResponse<>(false, String.format(TOO_MANY_REQUESTS_MESSAGE, waitSeconds));
        response.getWriter().write(MAPPER.writeValueAsString(body));
    }

    private String clientIp(HttpServletRequest request) {
        if (properties.isTrustForwardedFor()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /** The logged-in user's id from the JWT, otherwise the userId request parameter. */
    private String userKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Map<?, ?> principal
                && principal.get(PRINCIPAL_USER_ID_KEY) != null) {
            return principal.get(PRINCIPAL_USER_ID_KEY).toString();
        }
        return request.getParameter("userId");
    }
}
