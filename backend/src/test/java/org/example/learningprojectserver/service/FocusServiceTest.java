package org.example.learningprojectserver.service;

import org.example.learningprojectserver.dto.FocusDTOs.Insights;
import org.example.learningprojectserver.dto.FocusDTOs.SessionRequest;
import org.example.learningprojectserver.entities.FocusSessionEntity;
import org.example.learningprojectserver.entities.StudentDailyActivityEntity;
import org.example.learningprojectserver.mappers.FocusSessionEntityToItemMapper;
import org.example.learningprojectserver.repository.FocusSessionRepository;
import org.example.learningprojectserver.repository.StudentDailyActivityRepository;
import org.example.learningprojectserver.service.FocusService.AnswerRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FocusServiceTest {

    @Mock FocusSessionRepository focusSessionRepository;
    @Mock StudentDailyActivityRepository dailyActivityRepository;
    @Spy FocusSessionEntityToItemMapper sessionMapper = new FocusSessionEntityToItemMapper();
    @InjectMocks FocusService service;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 23, 20, 0);

    private static List<AnswerRow> answersAt(int hour, int correct, int total) {
        List<AnswerRow> rows = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            rows.add(new AnswerRow(NOW.withHour(hour).withMinute(i % 60), null, i < correct));
        }
        return rows;
    }

    @Test
    void bestHoursInsightReportsTheLiftOfTheBestTwoHourWindow() {
        List<AnswerRow> answers = new ArrayList<>();
        answers.addAll(answersAt(17, 9, 10));  // 90%
        answers.addAll(answersAt(18, 9, 10));  // 90%
        answers.addAll(answersAt(10, 7, 10));  // 70%
        answers.addAll(answersAt(12, 8, 10));  // 80%  -> rest 75%, lift 20%

        assertEquals("אתה מצליח 20% יותר כשאתה לומד בין 17:00 ל-19:00", FocusService.bestHoursInsight(answers));
    }

    @Test
    void bestHoursInsightNeedsEnoughAnswersAndARealDifference() {
        assertNull(FocusService.bestHoursInsight(answersAt(17, 3, 4)));
        List<AnswerRow> flat = new ArrayList<>();
        flat.addAll(answersAt(9, 8, 10));
        flat.addAll(answersAt(17, 8, 10));
        assertNull(FocusService.bestHoursInsight(flat));
    }

    @Test
    void minutesIntoTestAccumulateSecondsPerTest() {
        List<Object[]> rows = List.of(
                new Object[]{1L, NOW, 600, true},
                new Object[]{1L, NOW, 600, false},
                new Object[]{1L, NOW, 60, true},
                new Object[]{2L, NOW, 30, true},
                new Object[]{3L, NOW, null, true},
                new Object[]{3L, NOW, 30, true});

        List<AnswerRow> answers = FocusService.toAnswerRows(rows);

        assertEquals(List.of(0, 10, 20, 0), answers.subList(0, 4).stream().map(AnswerRow::minutesIntoTest).toList());
        assertNull(answers.get(4).minutesIntoTest());
        assertNull(answers.get(5).minutesIntoTest(), "after an unknown timing the rest of the test is unknown");
        assertFalse(answers.get(1).correct());
    }

    @Test
    void accuracyDropFindsTheFirstThresholdWithAClearDrop() {
        List<AnswerRow> answers = new ArrayList<>();
        for (int i = 0; i < 20; i++) answers.add(new AnswerRow(NOW, i, i % 10 != 0));           // 0-19 min: 90%
        for (int i = 20; i < 40; i++) answers.add(new AnswerRow(NOW, i, i % 10 != 0));          // 20-39: 90%
        for (int i = 40; i < 60; i++) answers.add(new AnswerRow(NOW, i, i % 2 == 0));           // 40-59: 50%

        assertEquals(40, FocusService.accuracyDropMinute(answers));
    }

    @Test
    void noDropWhenAccuracyStaysTheSame() {
        List<AnswerRow> answers = new ArrayList<>();
        for (int i = 0; i < 60; i++) answers.add(new AnswerRow(NOW, i, i % 4 != 0));
        assertNull(FocusService.accuracyDropMinute(answers));
    }

    @Test
    void insightsCombineFocusSessionsAndWeekdays() {
        FocusSessionEntity done = session(NOW.minusDays(1), 25, true);
        FocusSessionEntity old = session(NOW.minusDays(20), 45, false);
        List<StudentDailyActivityEntity> activity = new ArrayList<>();
        LocalDate sunday = LocalDate.of(2026, 11, 22);
        activity.add(day(sunday, 100));
        activity.add(day(sunday.minusWeeks(1), 90));
        activity.add(day(sunday.plusDays(1), 10));
        activity.add(day(sunday.plusDays(2), 20));

        Insights insights = service.buildInsights(List.of(), List.of(done, old), activity, 2, NOW);

        assertEquals(1, insights.focus().sessionsThisWeek());
        assertEquals(25, insights.focus().minutesThisWeek());
        assertEquals(50, insights.focus().completionPercent());
        assertTrue(insights.insights().contains("השבוע למדת 25 דקות במצב ריכוז ב-1 מפגשים"));
        assertTrue(insights.insights().contains("היום הכי פורה שלך הוא יום ראשון"));
        assertEquals(2, insights.recentSessions().size());
        assertEquals(9, insights.byHour().size());
        assertEquals(7, insights.bySessionMinute().size());
    }

    @Test
    void recordSessionSavesTheCallersSessionAndRejectsEmptyOnes() {
        assertFalse(service.recordSession("noa", new SessionRequest(25, 0, null, false)).isSuccess());
        verify(focusSessionRepository, never()).save(any());

        assertTrue(service.recordSession("noa", new SessionRequest(25, 30, " משוואות ", true)).isSuccess());
        verify(focusSessionRepository).save(argThat(s -> s.getStudentUserId().equals("noa")
                && s.getActualMinutes() == 25 && "משוואות".equals(s.getTask()) && s.isCompleted()
                && s.getStartedAt().plusMinutes(25).equals(s.getEndedAt())));
    }

    private static FocusSessionEntity session(LocalDateTime start, int minutes, boolean completed) {
        FocusSessionEntity s = new FocusSessionEntity();
        s.setId((long) minutes);
        s.setStudentUserId("noa");
        s.setStartedAt(start);
        s.setEndedAt(start.plusMinutes(minutes));
        s.setPlannedMinutes(minutes);
        s.setActualMinutes(minutes);
        s.setCompleted(completed);
        return s;
    }

    private static StudentDailyActivityEntity day(LocalDate date, int xp) {
        StudentDailyActivityEntity d = new StudentDailyActivityEntity("noa", date);
        d.setXp(xp);
        return d;
    }
}
