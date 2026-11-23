package org.example.learningprojectserver.jobs;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.service.HomeworkService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Every 15 minutes: reminds students about homework due within 24 hours that they have not
 * submitted yet. The reminder is an SSE notification (HOMEWORK_REMINDER); students who are
 * offline see the homework as pending on their homework page instead.
 */
@Component
@RequiredArgsConstructor
public class HomeworkReminderJob {

    private static final Logger LOG = Logger.getLogger(HomeworkReminderJob.class.getName());

    private final HomeworkService homeworkService;

    @Scheduled(cron = "0 */15 * * * *", zone = "Asia/Jerusalem")
    public void remindDueHomework() {
        try {
            int sent = homeworkService.sendDueReminders();
            if (sent > 0) LOG.info("Homework reminders sent: " + sent);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Homework reminder job failed", e);
        }
    }
}
