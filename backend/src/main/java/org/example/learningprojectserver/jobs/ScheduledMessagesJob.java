package org.example.learningprojectserver.jobs;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Delivers messages whose scheduled send time has come (checked every minute). */
@Component
@RequiredArgsConstructor
public class ScheduledMessagesJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduledMessagesJob.class);

    private final MessageService messageService;

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void deliverDueMessages() {
        try {
            int delivered = messageService.deliverDueScheduledMessages();
            if (delivered > 0) {
                log.info("Delivered {} scheduled message copies", delivered);
            }
        } catch (Exception e) {
            log.warn("Scheduled message delivery failed: {}", e.getMessage());
        }
    }
}
