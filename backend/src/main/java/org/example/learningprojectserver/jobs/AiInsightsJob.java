package org.example.learningprojectserver.jobs;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.entities.TeacherEntity;
import org.example.learningprojectserver.repository.TeacherRepository;
import org.example.learningprojectserver.service.ai.AiUserResolver;
import org.example.learningprojectserver.service.ai.insights.TeacherInsightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Every Sunday at 07:00 Israel time, writes last week's insight for every teacher. */
@Component
@RequiredArgsConstructor
public class AiInsightsJob {

    private static final Logger log = LoggerFactory.getLogger(AiInsightsJob.class);

    private final TeacherRepository teacherRepository;
    private final AiUserResolver userResolver;
    private final TeacherInsightService insightService;

    @Scheduled(cron = "${ai.insights.cron:0 0 7 * * SUN}", zone = "Asia/Jerusalem")
    public void generateWeeklyInsights() {
        int done = 0;
        for (TeacherEntity teacher : teacherRepository.findAll()) {
            try {
                insightService.generate(userResolver.resolve(teacher.getUserId()));
                done++;
            } catch (RuntimeException e) {
                log.warn("Weekly insight failed for teacher {}: {}", teacher.getUserId(), e.getMessage());
            }
        }
        log.info("Weekly AI insights written for {} teachers", done);
    }
}
