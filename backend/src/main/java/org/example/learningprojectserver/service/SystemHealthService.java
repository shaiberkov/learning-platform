package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.filter.ErrorCountingFilter;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.utils.gptApi.AiUsageMetrics;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/** Builds the system admin's health screen: uptime, database, error counts, AI usage, memory. */
@Service
@RequiredArgsConstructor
public class SystemHealthService {

    private static final int DB_VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;
    private final ErrorCountingFilter errorCountingFilter;
    private final AiUsageMetrics aiUsageMetrics;
    private final ActiveUserService activeUserService;
    private final AuditService auditService;
    private final Environment environment;

    public BasicResponse<Map<String, Object>> getHealth() {
        Map<String, Object> health = new LinkedHashMap<>();

        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        health.put("startedAt", LocalDateTime.ofInstant(Instant.ofEpochMilli(runtime.getStartTime()), ZoneId.systemDefault()));
        health.put("uptimeSeconds", runtime.getUptime() / 1000);

        health.put("database", databaseStatus());

        Map<String, Object> requests = new LinkedHashMap<>();
        requests.put("total", errorCountingFilter.getTotalRequests());
        requests.put("serverErrors", errorCountingFilter.getServerErrors());
        requests.put("clientErrors", errorCountingFilter.getClientErrors());
        requests.put("denied", errorCountingFilter.getDeniedRequests());
        requests.put("lastServerErrorPath", errorCountingFilter.getLastServerErrorPath());
        requests.put("lastServerErrorAt", errorCountingFilter.getLastServerErrorAt());
        health.put("requests", requests);

        Map<String, Object> ai = new LinkedHashMap<>();
        String gptToken = environment.getProperty("GPT_TOKEN");
        ai.put("configured", gptToken != null && !gptToken.isBlank());
        ai.put("requests", aiUsageMetrics.getRequests());
        ai.put("failures", aiUsageMetrics.getFailures());
        ai.put("lastRequestAt", aiUsageMetrics.getLastRequestAt());
        health.put("ai", ai);

        Runtime jvm = Runtime.getRuntime();
        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("usedMb", (jvm.totalMemory() - jvm.freeMemory()) / (1024 * 1024));
        memory.put("maxMb", jvm.maxMemory() / (1024 * 1024));
        health.put("memory", memory);

        health.put("activeUsers", activeUserService.getActiveUsers().size());
        health.put("auditEntriesLast24h", safeAuditCount());

        BasicResponse<Map<String, Object>> response = new BasicResponse<>(true, null);
        response.setData(health);
        return response;
    }

    private Map<String, Object> databaseStatus() {
        Map<String, Object> db = new LinkedHashMap<>();
        long start = System.nanoTime();
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(DB_VALIDATION_TIMEOUT_SECONDS);
            db.put("up", valid);
            db.put("latencyMs", (System.nanoTime() - start) / 1_000_000);
            db.put("product", connection.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            db.put("up", false);
            db.put("error", e.getClass().getSimpleName());
        }
        return db;
    }

    private Long safeAuditCount() {
        try {
            return auditService.countSince(LocalDateTime.now().minusHours(24));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
