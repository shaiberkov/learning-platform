package org.example.learningprojectserver.utils.gptApi;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/** Counts OpenAI calls made through GptMassenger since startup, for the system health screen. */
@Component
public class AiUsageMetrics {

    private final AtomicLong requests = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private volatile LocalDateTime lastRequestAt;

    public void recordRequest() {
        requests.incrementAndGet();
        lastRequestAt = LocalDateTime.now();
    }

    public void recordFailure() {
        failures.incrementAndGet();
    }

    public long getRequests() { return requests.get(); }
    public long getFailures() { return failures.get(); }
    public LocalDateTime getLastRequestAt() { return lastRequestAt; }
}
