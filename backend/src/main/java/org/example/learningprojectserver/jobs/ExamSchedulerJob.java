package org.example.learningprojectserver.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.learningprojectserver.service.ExamAttemptService;
import org.example.learningprojectserver.service.ExamAuthoringService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every 30 seconds: publishes exams whose publish time has come and grades attempts whose
 * deadline passed without a submission. Each item runs in its own transaction, so one failure
 * does not block the rest.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamSchedulerJob {

    private final ExamAuthoringService examAuthoringService;
    private final ExamAttemptService examAttemptService;

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void run() {
        for (Long testId : examAuthoringService.findDueForPublish()) {
            try {
                examAuthoringService.publishScheduled(testId);
            } catch (RuntimeException e) {
                log.warn("Scheduled publish failed for test {}", testId, e);
            }
        }
        for (Long attemptId : examAttemptService.findExpiredAttemptIds()) {
            try {
                examAttemptService.autoSubmitExpired(attemptId);
            } catch (RuntimeException e) {
                log.warn("Auto-submit failed for attempt {}", attemptId, e);
            }
        }
    }
}
