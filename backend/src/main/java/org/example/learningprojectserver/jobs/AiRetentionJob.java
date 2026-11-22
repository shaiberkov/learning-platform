package org.example.learningprojectserver.jobs;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.repository.AiConversationMessageRepository;
import org.example.learningprojectserver.service.ai.AiGateway;
import org.example.learningprojectserver.service.ai.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Enforces the AI retention policy shown to users: tutor conversations, flagged ones included,
 * are deleted {@code ai.retention.days} days after they were written. Runs nightly at 03:30.
 */
@Component
@RequiredArgsConstructor
public class AiRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AiRetentionJob.class);

    private final AiConversationMessageRepository conversationRepository;
    private final AiProperties properties;

    @Scheduled(cron = "${ai.retention.cron:0 30 3 * * *}", zone = "Asia/Jerusalem")
    public void deleteExpiredConversations() {
        int deleted = conversationRepository.deleteOlderThan(AiGateway.now().minusDays(properties.getRetentionDays()));
        log.info("AI retention: deleted {} tutor messages older than {} days", deleted, properties.getRetentionDays());
    }
}
