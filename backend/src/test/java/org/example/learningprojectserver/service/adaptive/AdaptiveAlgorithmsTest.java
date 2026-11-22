package org.example.learningprojectserver.service.adaptive;

import org.example.learningprojectserver.entities.ReviewItemEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveAlgorithmsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    private static ReviewItemEntity newMistake() {
        ReviewItemEntity item = new ReviewItemEntity();
        Sm2Scheduler.resetAsNewMistake(item, TODAY);
        return item;
    }

    @Test
    void newMistakeIsDueTodayWithDefaultEasiness() {
        ReviewItemEntity item = newMistake();
        assertEquals(TODAY, item.getDueDate());
        assertEquals(2.5, item.getEasiness());
        assertEquals(0, item.getRepetitions());
    }

    @Test
    void correctReviewsFollowSm2Intervals() {
        ReviewItemEntity item = newMistake();
        Sm2Scheduler.review(item, 4, TODAY);
        assertEquals(1, item.getIntervalDays());
        assertEquals(TODAY.plusDays(1), item.getDueDate());

        Sm2Scheduler.review(item, 4, TODAY.plusDays(1));
        assertEquals(6, item.getIntervalDays());

        Sm2Scheduler.review(item, 4, TODAY.plusDays(7));
        // quality 4 keeps the easiness at 2.5, so 6 * 2.5 = 15
        assertEquals(15, item.getIntervalDays());
        assertEquals(TODAY.plusDays(22), item.getDueDate());
        assertEquals(3, item.getRepetitions());
    }

    @Test
    void wrongReviewRestartsScheduleAndLowersEasiness() {
        ReviewItemEntity item = newMistake();
        Sm2Scheduler.review(item, 4, TODAY);
        Sm2Scheduler.review(item, 1, TODAY.plusDays(1));
        assertEquals(0, item.getRepetitions());
        assertEquals(1, item.getIntervalDays());
        assertEquals(TODAY.plusDays(2), item.getDueDate());
        assertTrue(item.getEasiness() < 2.5);
        assertEquals(1, item.getLapses());
    }

    @Test
    void easinessNeverDropsBelowMinimum() {
        ReviewItemEntity item = newMistake();
        for (int i = 0; i < 20; i++) {
            Sm2Scheduler.review(item, 0, TODAY);
        }
        assertEquals(Sm2Scheduler.MIN_EASINESS, item.getEasiness(), 1e-9);
    }

    @Test
    void eloRisesOnCorrectAndFallsOnWrong() {
        double start = EloRating.INITIAL_RATING;
        assertTrue(EloRating.update(start, 0, 2, 1.0) > start);
        assertTrue(EloRating.update(start, 0, 2, 0.0) < start);
        // A correct answer on a harder question is worth more.
        assertTrue(EloRating.update(start, 0, 5, 1.0) > EloRating.update(start, 0, 1, 1.0));
    }

    @Test
    void eloStaysWithinBounds() {
        assertEquals(EloRating.MAX_RATING, EloRating.update(EloRating.MAX_RATING, 50, 5, 1.0));
        assertEquals(EloRating.MIN_RATING, EloRating.update(EloRating.MIN_RATING, 50, 1, 0.0));
    }

    @Test
    void pickLevelAimsSlightlyAboveCurrentRating() {
        assertEquals(2, EloRating.pickLevel(EloRating.INITIAL_RATING));
        assertEquals(1, EloRating.pickLevel(700));
        assertEquals(3, EloRating.pickLevel(1100));
        assertEquals(5, EloRating.pickLevel(1600));
        for (double r = 600; r <= 1700; r += 25) {
            int level = EloRating.pickLevel(r);
            assertTrue(level >= EloRating.MIN_LEVEL && level <= EloRating.MAX_LEVEL);
        }
    }

    @Test
    void colorThresholds() {
        assertEquals("RED", EloRating.color(999));
        assertEquals("YELLOW", EloRating.color(1000));
        assertEquals("GREEN", EloRating.color(1200));
    }

    @Test
    void eachHintReducesPracticePoints() {
        assertEquals(10, PracticeScoring.points(true, 0));
        assertEquals(7, PracticeScoring.points(true, 1));
        assertEquals(4, PracticeScoring.points(true, 2));
        assertEquals(0, PracticeScoring.points(true, 3));
        assertEquals(0, PracticeScoring.points(false, 0));
        assertTrue(PracticeScoring.eloOutcome(true, 1) < PracticeScoring.eloOutcome(true, 0));
        assertEquals(0.0, PracticeScoring.eloOutcome(true, 3));
    }

    @Test
    void catalogMapsGeneratorAliases() {
        assertEquals("חישוב מרחק בין שתי נקודות בגרף", SkillCatalog.canonicalSubTopic("חישוב מרחק"));
        assertEquals("הנדסה אנליטית", SkillCatalog.topicOf("חישוב מרחק", null));
        assertEquals("שברים", SkillCatalog.topicOf("חיבור שברים", null));
        assertNull(SkillCatalog.topicOf("לא קיים", null));
    }
}
