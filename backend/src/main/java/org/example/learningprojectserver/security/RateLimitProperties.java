package org.example.learningprojectserver.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Rate-limit settings ({@code rate-limit.*} in application properties).
 * Each rule has two token buckets: one per client IP and one per user (userId).
 * A bucket holds {@code capacity} requests and refills that many over {@code period}.
 */
@Component
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private boolean enabled = true;

    /** Use the first X-Forwarded-For address as the client IP (true behind Render's proxy). */
    private boolean trustForwardedFor = false;

    private Map<String, Rule> rules = new HashMap<>();

    @Getter
    @Setter
    public static class Rule {
        private int ipCapacity = 30;
        private int userCapacity = 10;
        private Duration period = Duration.ofMinutes(15);
    }
}
