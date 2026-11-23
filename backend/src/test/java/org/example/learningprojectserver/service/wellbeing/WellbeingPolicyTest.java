package org.example.learningprojectserver.service.wellbeing;

import org.example.learningprojectserver.service.wellbeing.WellbeingPolicy.GoalStatus;
import org.example.learningprojectserver.service.wellbeing.WellbeingPolicy.GoalType;
import org.example.learningprojectserver.service.wellbeing.WellbeingPolicy.Mood;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WellbeingPolicyTest {

    @Test
    void weekStartsOnSunday() {
        assertEquals(LocalDate.of(2026, 11, 22), WellbeingPolicy.weekStart(LocalDate.of(2026, 11, 22))); // Sunday
        assertEquals(LocalDate.of(2026, 11, 22), WellbeingPolicy.weekStart(LocalDate.of(2026, 11, 28))); // Saturday
        assertEquals(LocalDate.of(2026, 11, 29), WellbeingPolicy.weekStart(LocalDate.of(2026, 11, 30)));
    }

    @Test
    void measurableGoalFollowsProgressNotTheSelfMark() {
        assertEquals(GoalStatus.ACHIEVED, WellbeingPolicy.goalStatus(GoalType.PRACTICE_QUESTIONS, 20, 20, null, false));
        assertEquals(GoalStatus.IN_PROGRESS, WellbeingPolicy.goalStatus(GoalType.XP, 100, 40, true, false));
        assertEquals(GoalStatus.NOT_ACHIEVED, WellbeingPolicy.goalStatus(GoalType.ACTIVE_DAYS, 5, 3, true, true));
    }

    @Test
    void freeGoalFollowsTheStudentsMark() {
        assertEquals(GoalStatus.ACHIEVED, WellbeingPolicy.goalStatus(GoalType.FREE, null, 0, true, true));
        assertEquals(GoalStatus.IN_PROGRESS, WellbeingPolicy.goalStatus(GoalType.FREE, null, 0, null, false));
        assertEquals(GoalStatus.UNMARKED, WellbeingPolicy.goalStatus(GoalType.FREE, null, 0, null, true));
        assertEquals(GoalStatus.NOT_ACHIEVED, WellbeingPolicy.goalStatus(GoalType.FREE, null, 0, false, true));
    }

    @Test
    void distressPatternIsThreeOfTheLatestFive() {
        assertTrue(WellbeingPolicy.distressPattern(List.of(true, false, true, false, true)));
        assertFalse(WellbeingPolicy.distressPattern(List.of(true, false, true, false, false, true, true)));
        assertEquals(2, WellbeingPolicy.distressCount(List.of(true, true)));
    }

    @Test
    void alertNeedsConsentAndCooldown() {
        assertTrue(WellbeingPolicy.shouldAlert(true, true, false));
        assertFalse(WellbeingPolicy.shouldAlert(true, false, false));
        assertFalse(WellbeingPolicy.shouldAlert(true, true, true));
        assertFalse(WellbeingPolicy.shouldAlert(false, true, false));
    }

    @Test
    void stressGetsABreathingExerciseCalmGetsATip() {
        assertEquals("BREATHING", WellbeingPolicy.suggestionFor(Mood.OVERWHELMED, 3).kind());
        assertEquals("BREATHING", WellbeingPolicy.suggestionFor(Mood.NERVOUS, 3).kind());
        assertEquals("TIP", WellbeingPolicy.suggestionFor(Mood.GREAT, 3).kind());
        assertTrue(Mood.STRESSED.distress());
        assertFalse(Mood.NERVOUS.distress());
        assertNull(Mood.parse("happy"));
    }
}
