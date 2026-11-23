package org.example.learningprojectserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.DispatcherType;
import org.example.learningprojectserver.filter.JwtAuthenticationFilter;
import org.example.learningprojectserver.filter.LoggingFilter;
import org.example.learningprojectserver.filter.MetricsFilter;
import org.example.learningprojectserver.filter.CookieCsrfGuardFilter;
import org.example.learningprojectserver.filter.RateLimitFilter;
import org.example.learningprojectserver.security.RateLimitProperties;
import org.example.learningprojectserver.security.RateLimiter;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.AccessDeniedAdvice;
import org.example.learningprojectserver.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.cache.CacheManager;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.example.learningprojectserver.constants.SecurityConstants.SecurityMatcherConstants.*;

@Configuration
@EnableAsync
@EnableCaching
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final ObjectMapper ERROR_WRITER = new ObjectMapper();


    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    public LoggingFilter loggingFilter(JwtService jwtService) {
        return new LoggingFilter(jwtService);
    }
    @Bean
    public MetricsFilter metricsFilter() {
        return new MetricsFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   LoggingFilter loggingFilter,
                                                   MetricsFilter metricsFilter,
                                                   RateLimiter rateLimiter,
                                                   RateLimitProperties rateLimitProperties) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "נדרשת התחברות למערכת"))
                        .accessDeniedHandler((request, response, e) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, AccessDeniedAdvice.ACCESS_DENIED_MESSAGE))
                )
                // URL rules pick the roles; @PreAuthorize on the controllers (security/AccessGuard)
                // checks that the user, school or test in the request belongs to the caller.
                .authorizeHttpRequests(auth -> auth
                        // The AI tutor streams over SSE: the stream's async/error dispatches carry no JWT,
                        // the original request was already authorized.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                V3_API_DOCS,
                                SWAGGER_UI_RESOURCES,
                                SWAGGER_UI_HTML
                        ).permitAll()
                        .requestMatchers(USER_ALL).permitAll()
                        .requestMatchers(VALIDATE_TOKEN_ALL).permitAll()
                        // The SSE stream authenticates with the access_token cookie (EventSource withCredentials).
                        .requestMatchers(NOTIFICATION_PUSH_KEY).permitAll()
                        .requestMatchers(NOTIFICATION_ALL).authenticated()
                        .requestMatchers(CALENDAR_FEED).permitAll()
                        .requestMatchers(SYSTEM_ADMIN_ALL).hasRole(SYSTEM_ADMIN)
                        .requestMatchers(SCHOOL_MANAGER_ALL).hasAnyRole(SCHOOL_MANAGER, SYSTEM_ADMIN)
                        .requestMatchers(TEACHER_TEST_SUBMIT).hasAnyRole(STUDENT, SYSTEM_ADMIN)
                        .requestMatchers(TEACHER_ALL).hasAnyRole(TEACHER, SCHOOL_MANAGER, SYSTEM_ADMIN)
                        .requestMatchers(STUDENT_ALL).hasAnyRole(STUDENT, SYSTEM_ADMIN)
                        .requestMatchers(TEST_ALL).hasAnyRole(STUDENT, TEACHER, SYSTEM_ADMIN)
                        .requestMatchers(AUDIT_LOG_ALL).hasAnyRole(SCHOOL_MANAGER, SYSTEM_ADMIN)
                        .requestMatchers(IMPERSONATION_START).hasRole(SYSTEM_ADMIN)
                        .requestMatchers(IMPERSONATION_ALL).authenticated()
                        .requestMatchers(ACTIVE_USER_ALL).authenticated()
                        .requestMatchers(QUESTION_ALL).authenticated()
                        .requestMatchers(USER_STATISTIC_ALL).authenticated()
                        .requestMatchers(MESSAGE_ALL).authenticated()
                        .requestMatchers(LESSON_ALL).authenticated()
                        .requestMatchers(SCHEDULE_ALL).authenticated()
                        .requestMatchers(UPCOMING_EVENTS_ALL).authenticated()
                        // Actuator: health (and its liveness/readiness groups) is public for Render's health check;
                        // every other endpoint (info, metrics) needs a SYSTEM_ADMIN JWT.
                        .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
                        .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole(SYSTEM_ADMIN)
                        .requestMatchers(EXAM_ALL).hasAnyRole(STUDENT, TEACHER, SCHOOL_MANAGER, SYSTEM_ADMIN)
                        .requestMatchers(ENGAGEMENT_ALL).hasAnyRole(STUDENT, TEACHER, SYSTEM_ADMIN)
                        .requestMatchers(STUDY_PLAN_ALL).hasAnyRole(STUDENT, SYSTEM_ADMIN)
                        .requestMatchers(LESSON_CONTENT_ALL).hasAnyRole(STUDENT, TEACHER, SYSTEM_ADMIN)
                        .requestMatchers(HOMEWORK_ALL).hasAnyRole(STUDENT, TEACHER, SYSTEM_ADMIN)
                        .requestMatchers(ACCOMMODATION_ALL).hasAnyRole(STUDENT, TEACHER, SYSTEM_ADMIN)
                        // AI routes take the userId from the token (quota, exam lock, ownership); parents have no AI screens.
                        // Staff routes also check the role.
                        .requestMatchers(AI_STAFF_ALL).hasAnyRole(TEACHER, SCHOOL_MANAGER, SYSTEM_ADMIN)
                        .requestMatchers(AI_ALL).hasAnyRole(STUDENT, TEACHER, SCHOOL_MANAGER, SYSTEM_ADMIN)
                        .requestMatchers(QUESTION_BANK_ALL).hasAnyRole(TEACHER, SYSTEM_ADMIN)
                        .requestMatchers(GRADEBOOK_ALL).hasAnyRole(TEACHER, SYSTEM_ADMIN)
                        .requestMatchers(LIVE_QUIZ_ALL).hasAnyRole(TEACHER, STUDENT, SYSTEM_ADMIN)
                        .requestMatchers(ATTENDANCE_ALL).hasAnyRole(TEACHER, SCHOOL_MANAGER, SYSTEM_ADMIN)
                        .requestMatchers(PARENT_ALL).hasAnyRole(PARENT, SYSTEM_ADMIN)
                        .requestMatchers(CALENDAR_ALL).authenticated()
                        .requestMatchers(SEARCH_ALL).authenticated()
                        // [s02] class forum and student-authored questions
                        .requestMatchers(CLASS_COMMUNITY_ALL).hasAnyRole(STUDENT, TEACHER, SYSTEM_ADMIN)
                        // [s07] real-world cards and simulations; editing and simulation records also check the role per method.
                        .requestMatchers(REAL_WORLD_ALL).hasAnyRole(STUDENT, TEACHER, SYSTEM_ADMIN)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(metricsFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loggingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Not beans on purpose: a Filter bean would also be registered with the servlet container.
                .addFilterBefore(new CookieCsrfGuardFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new RateLimitFilter(rateLimiter, rateLimitProperties), UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(ERROR_WRITER.writeValueAsString(new BasicResponse<>(false, message)));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(ALLOWED_ORIGINS);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(CORS_MAPPING_PATH, configuration);
        return source;
    }


}