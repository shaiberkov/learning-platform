package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.service.EngagementService.BadgeGrant;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The one place learning flows report activity for gamification. Every method swallows its own
 * failure: XP and badges must never break or roll back the student's real work.
 */
@Component
@RequiredArgsConstructor
public class EngagementEvents {

    private static final Logger LOG = Logger.getLogger(EngagementEvents.class.getName());

    static final int XP_CORRECT_ANSWER = 10;
    static final int XP_WRONG_ANSWER = 2;
    static final int XP_PRACTICE_TEST = 5;
    static final int XP_TEACHER_TEST = 10;
    static final int XP_HOMEWORK_ON_TIME = 20;
    static final int XP_HOMEWORK_LATE = 5;
    static final int XP_LESSON_STAGE = 5;
    static final int XP_STUDY_TASK = 10;

    private final EngagementService engagementService;

    /** A practice answer; skillLevel is the sub-topic level after the answer. */
    public void practiceAnswer(String userId, boolean correct, String subTopic, Integer skillLevel) {
        List<BadgeGrant> badges = new ArrayList<>();
        if (correct && skillLevel != null && skillLevel >= EngagementService.MASTERY_LEVEL && subTopic != null) {
            badges.add(new BadgeGrant("MASTERED:" + subTopic, "שליטה ב" + subTopic,
                    "הגעת לרמה " + EngagementService.MASTERY_LEVEL + " ב" + subTopic));
        }
        safely(userId, correct ? XP_CORRECT_ANSWER : XP_WRONG_ANSWER, badges);
    }

    public void testCompleted(String userId, int score, boolean teacherTest) {
        List<BadgeGrant> badges = new ArrayList<>();
        if (score >= 100) {
            badges.add(new BadgeGrant("PERFECT_TEST", "מבחן מושלם", "קיבלת 100 במבחן"));
        }
        int base = teacherTest ? XP_TEACHER_TEST : XP_PRACTICE_TEST;
        safely(userId, base + Math.max(score, 0) / 10, badges);
    }

    public void homeworkSubmitted(String userId, boolean late, long onTimeSubmissionsSoFar) {
        List<BadgeGrant> badges = new ArrayList<>();
        if (onTimeSubmissionsSoFar >= 5) {
            badges.add(new BadgeGrant("HOMEWORK_ON_TIME_5", "תמיד בזמן", "הגשת 5 שיעורי בית בזמן"));
        }
        safely(userId, late ? XP_HOMEWORK_LATE : XP_HOMEWORK_ON_TIME, badges);
    }

    public void lessonStage(String userId, boolean lessonFinished) {
        List<BadgeGrant> badges = new ArrayList<>();
        if (lessonFinished) {
            badges.add(new BadgeGrant("FIRST_LESSON", "לומד/ת עצמאי/ת", "השלמת שיעור: למידה, תרגול וחזרה"));
        }
        safely(userId, XP_LESSON_STAGE, badges);
    }

    public void studyTaskDone(String userId) {
        safely(userId, XP_STUDY_TASK, List.of());
    }

    private void safely(String userId, int xp, List<BadgeGrant> badges) {
        try {
            engagementService.recordActivity(userId, xp, badges);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Engagement update failed for " + userId, e);
        }
    }
}
