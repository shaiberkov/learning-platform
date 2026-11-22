package org.example.learningprojectserver.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Counts requests and failed responses since startup for the system health screen.
 * Runs first, outside Spring Security, so 401/403 rejections are counted too. In memory, per process.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ErrorCountingFilter extends OncePerRequestFilter {

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong serverErrors = new AtomicLong();
    private final AtomicLong clientErrors = new AtomicLong();
    private final AtomicLong deniedRequests = new AtomicLong();
    private volatile String lastServerErrorPath;
    private volatile LocalDateTime lastServerErrorAt;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        totalRequests.incrementAndGet();
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException e) {
            recordServerError(request);
            throw e;
        }
        int status = response.getStatus();
        if (status >= 500) {
            recordServerError(request);
        } else if (status >= 400) {
            clientErrors.incrementAndGet();
            if (status == HttpServletResponse.SC_UNAUTHORIZED || status == HttpServletResponse.SC_FORBIDDEN) {
                deniedRequests.incrementAndGet();
            }
        }
    }

    private void recordServerError(HttpServletRequest request) {
        serverErrors.incrementAndGet();
        lastServerErrorPath = request.getMethod() + " " + request.getRequestURI();
        lastServerErrorAt = LocalDateTime.now();
    }

    public long getTotalRequests() { return totalRequests.get(); }
    public long getServerErrors() { return serverErrors.get(); }
    public long getClientErrors() { return clientErrors.get(); }
    public long getDeniedRequests() { return deniedRequests.get(); }
    public String getLastServerErrorPath() { return lastServerErrorPath; }
    public LocalDateTime getLastServerErrorAt() { return lastServerErrorAt; }
}
