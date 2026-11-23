package org.example.learningprojectserver.jobs;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.service.wellbeing.ReflectionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * [s10] Sunday 08:00 Israel time: reminds students who have not written this week's reflection
 * (three short questions and a goal). The dashboard card shows the same prompt until they do.
 */
@Component
@RequiredArgsConstructor
public class ReflectionReminderJob {

    private static final Logger LOG = Logger.getLogger(ReflectionReminderJob.class.getName());

    private final ReflectionService reflectionService;

    @Scheduled(cron = "0 0 8 * * SUN", zone = "Asia/Jerusalem")
    public void remindReflection() {
        try {
            int sent = reflectionService.sendSundayReminders();
            if (sent > 0) LOG.info("Reflection reminders sent: " + sent);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Reflection reminder job failed", e);
        }
    }
}
