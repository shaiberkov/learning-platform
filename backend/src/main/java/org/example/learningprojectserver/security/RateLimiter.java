package org.example.learningprojectserver.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory Bucket4j buckets, one per (rule, ip) and per (rule, user).
 * Per process, lost on restart (the server runs as a single instance). Idle buckets are dropped
 * once they would have refilled completely, so dropping them changes nothing.
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private record Entry(Bucket bucket, Duration period, long[] lastSeen) { }

    private final RateLimitProperties properties;
    private final Map<String, Entry> buckets = new ConcurrentHashMap<>();

    /**
     * Takes one token from the rule's IP bucket and, when a user is known, from its user bucket.
     * @return 0 when allowed, otherwise the seconds to wait before retrying
     */
    public long tryConsume(String ruleName, String ip, String userId) {
        RateLimitProperties.Rule rule = properties.getRules().get(ruleName);
        if (!properties.isEnabled() || rule == null) {
            return 0;
        }
        long waitSeconds = consume(ruleName + "|ip|" + ip, rule.getIpCapacity(), rule.getPeriod());
        if (waitSeconds > 0) {
            return waitSeconds;
        }
        if (userId != null && !userId.isBlank()) {
            return consume(ruleName + "|user|" + userId, rule.getUserCapacity(), rule.getPeriod());
        }
        return 0;
    }

    private long consume(String key, int capacity, Duration period) {
        Entry entry = buckets.computeIfAbsent(key, k -> new Entry(
                Bucket.builder()
                        .addLimit(Bandwidth.builder().capacity(capacity).refillGreedy(capacity, period).build())
                        .build(),
                period,
                new long[1]));
        entry.lastSeen()[0] = System.currentTimeMillis();
        ConsumptionProbe probe = entry.bucket().tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return 0;
        }
        return Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds() + 1);
    }

    @Scheduled(fixedRate = 600_000)
    public void evictIdleBuckets() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(e -> now - e.getValue().lastSeen()[0] > e.getValue().period().toMillis());
    }

    int size() {
        return buckets.size();
    }
}
