package org.example.learningprojectserver.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.AuthCookies;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * CSRF guard for cookie auth. The auth cookies are SameSite=None in production (frontend and API are on
 * different sites), so a foreign page could make the browser send them with a form POST.
 * A state-changing request that carries an auth cookie must also carry {@code X-Requested-With}:
 * a custom header forces a CORS preflight, which only the allowed origins pass.
 * The frontend sets the header on every axios request.
 */
public class CookieCsrfGuardFilter extends OncePerRequestFilter {

    public static final String REQUIRED_HEADER = "X-Requested-With";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SAFE_METHODS.contains(request.getMethod()) || !hasAuthCookie(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(REQUIRED_HEADER);
        if (header != null && !header.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(MAPPER.writeValueAsString(
                new BasicResponse<Void>(false, "הבקשה נחסמה מסיבות אבטחה")));
    }

    private static boolean hasAuthCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if (AuthCookies.ACCESS_COOKIE.equals(cookie.getName()) || AuthCookies.REFRESH_COOKIE.equals(cookie.getName())) {
                return true;
            }
        }
        return false;
    }
}
