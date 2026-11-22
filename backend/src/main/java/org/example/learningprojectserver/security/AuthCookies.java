package org.example.learningprojectserver.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static org.example.learningprojectserver.constants.ControllerConstants.User.USER_BASE_PATH;
import static org.example.learningprojectserver.constants.SharedConstants.SharedControllerConstants.AUTH_HEADER;
import static org.example.learningprojectserver.constants.SharedConstants.SharedControllerConstants.BEARER_PREFIX;

/**
 * Writes and reads the auth cookies. Both are httpOnly, so page scripts never see a token.
 * The refresh cookie is scoped to the user endpoints (refresh-token, logout) only.
 * Secure / SameSite come from properties: production is cross-site (Secure + SameSite=None),
 * dev runs over http on localhost (no Secure, SameSite=Lax).
 */
@Component
public class AuthCookies {

    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";
    private static final String ACCESS_PATH = "/";
    private static final String REFRESH_PATH = USER_BASE_PATH;

    @Value("${auth.cookie.secure:true}")
    private boolean secure;

    @Value("${auth.cookie.same-site:None}")
    private String sameSite;

    @Value("${auth.access-token.ttl-minutes:15}")
    private long accessTtlMinutes;

    @Value("${auth.refresh-token.ttl-days:7}")
    private long refreshTtlDays;

    public void write(HttpServletResponse response, IssuedTokens tokens) {
        add(response, ACCESS_COOKIE, tokens.accessToken(), ACCESS_PATH, Duration.ofMinutes(accessTtlMinutes));
        add(response, REFRESH_COOKIE, tokens.refreshToken(), REFRESH_PATH, Duration.ofDays(refreshTtlDays));
    }

    /** Replaces only the access cookie (impersonation); the refresh cookie still belongs to the admin. */
    public void writeAccessOnly(HttpServletResponse response, String accessToken, Duration maxAge) {
        add(response, ACCESS_COOKIE, accessToken, ACCESS_PATH, maxAge);
    }

    public void clear(HttpServletResponse response) {
        add(response, ACCESS_COOKIE, "", ACCESS_PATH, Duration.ZERO);
        add(response, REFRESH_COOKIE, "", REFRESH_PATH, Duration.ZERO);
    }

    private void add(HttpServletResponse response, String name, String value, String path, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * The access token from the httpOnly cookie, or from an {@code Authorization: Bearer} header
     * (kept for Swagger and API clients). Stale values such as "Bearer undefined" are ignored.
     */
    public static String resolveAccessToken(HttpServletRequest request) {
        String fromCookie = readCookie(request, ACCESS_COOKIE);
        if (fromCookie != null) {
            return fromCookie;
        }
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty() || "undefined".equals(token) || "null".equals(token)) {
            return null;
        }
        return token;
    }

    public static String readRefreshToken(HttpServletRequest request) {
        return readCookie(request, REFRESH_COOKIE);
    }

    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
