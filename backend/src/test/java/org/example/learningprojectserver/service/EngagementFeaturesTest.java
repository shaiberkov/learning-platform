package org.example.learningprojectserver.service;

import org.example.learningprojectserver.entities.HomeworkSubmissionEntity;
import org.example.learningprojectserver.entities.StudyPlanTaskEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pure logic of gamification, study plans, lessons, homework and accommodations. No database. */
class EngagementFeaturesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    @Test
    void streakStartsGrowsAndResets() {
        assertEquals(1, EngagementService.nextStreak(null, TODAY, 0));
        assertEquals(4, EngagementService.nextStreak(TODAY.minusDays(1), TODAY, 3));
        assertEquals(3, EngagementService.nextStreak(TODAY, TODAY, 3));
        assertEquals(1, EngagementService.nextStreak(TODAY.minusDays(2), TODAY, 9));
    }

    @Test
    void visibleStreakDropsAfterAMissedDay() {
        assertEquals(5, EngagementService.visibleStreak(TODAY.minusDays(1), TODAY, 5));
        assertEquals(0, EngagementService.visibleStreak(TODAY.minusDays(2), TODAY, 5));
        assertEquals(0, EngagementService.visibleStreak(null, TODAY, 0));
    }

    @Test
    void progressMessageIsPersonalAndEncouraging() {
        assertTrue(EngagementService.progressMessage(120, 90).contains("30"));
        assertTrue(EngagementService.progressMessage(50, 80).contains("30"));
        assertNotNull(EngagementService.progressMessage(0, 0));
    }

    @Test
    void planCoversDaysUntilTheExamWithLearnPracticeReview() {
        List<StudyPlanTaskEntity> tasks = StudyPlanService.buildTasks("s1", 7L, "מתמטיקה", "משוואות", "בינוני",
                TODAY, TODAY.plusDays(5));
        assertEquals(5, tasks.size());
        assertEquals(TODAY, tasks.get(0).getTaskDate());
        assertEquals(TODAY.plusDays(4), tasks.get(4).getTaskDate());
        assertEquals("LEARN", tasks.get(0).getTaskType());
        assertEquals("PRACTICE", tasks.get(2).getTaskType());
        assertEquals("REVIEW", tasks.get(4).getTaskType());
        assertEquals(15, tasks.get(0).getMinutes());
        assertTrue(tasks.get(2).getDescription().startsWith("היום: 15 דקות"));
        assertTrue(tasks.get(2).getDescription().contains("משוואות"));
    }

    @Test
    void planIsCappedAndExamTodayGetsOneReview() {
        List<StudyPlanTaskEntity> far = StudyPlanService.buildTasks("s1", 1L, "מתמטיקה", "שברים", "קשה",
                TODAY, TODAY.plusDays(40));
        assertEquals(StudyPlanService.PLAN_HORIZON_DAYS, far.size());
        assertEquals(TODAY.plusDays(39), far.get(far.size() - 1).getTaskDate());

        List<StudyPlanTaskEntity> today = StudyPlanService.buildTasks("s1", 1L, "מתמטיקה", "שברים", "קל",
                TODAY, TODAY);
        assertEquals(1, today.size());
        assertEquals("REVIEW", today.get(0).getTaskType());
        assertEquals(15, today.get(0).getMinutes()); // easy (10) + 5 because the exam is close
    }

    @Test
    void onlyYoutubeAndVimeoAreEmbedded() {
        assertEquals("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ",
                LessonContentService.toEmbedUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10"));
        assertEquals("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ",
                LessonContentService.toEmbedUrl("https://youtu.be/dQw4w9WgXcQ"));
        assertEquals("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ",
                LessonContentService.toEmbedUrl("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ"));
        assertEquals("https://player.vimeo.com/video/123456",
                LessonContentService.toEmbedUrl("https://vimeo.com/123456"));
        assertEquals("https://player.vimeo.com/video/123456",
                LessonContentService.toEmbedUrl("https://player.vimeo.com/video/123456"));
        assertEquals("", LessonContentService.toEmbedUrl("  "));
        assertNull(LessonContentService.toEmbedUrl("https://evil.example.com/video"));
        assertNull(LessonContentService.toEmbedUrl("javascript:alert(1)"));
    }

    @Test
    void homeworkStatus() {
        LocalDateTime due = LocalDateTime.of(2026, 11, 23, 20, 0);
        HomeworkSubmissionEntity onTime = new HomeworkSubmissionEntity();
        HomeworkSubmissionEntity late = new HomeworkSubmissionEntity();
        late.setLate(true);
        assertEquals("SUBMITTED", HomeworkService.status(onTime, due, due.plusDays(1)));
        assertEquals("LATE", HomeworkService.status(late, due, due.plusDays(1)));
        assertEquals("PENDING", HomeworkService.status(null, due, due.minusHours(1)));
        assertEquals("MISSING", HomeworkService.status(null, due, due.plusMinutes(1)));
    }

    @Test
    void extraTimeRoundsUp() {
        assertEquals(60, AccommodationService.adjustedMinutes(60, 0));
        assertEquals(75, AccommodationService.adjustedMinutes(60, 25));
        assertEquals(14, AccommodationService.adjustedMinutes(10, 33));
        assertEquals(90, AccommodationService.adjustedMinutes(45, 100));
    }
}
