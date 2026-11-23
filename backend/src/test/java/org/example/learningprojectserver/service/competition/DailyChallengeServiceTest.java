package org.example.learningprojectserver.service.competition;

import org.example.learningprojectserver.dto.DailyChallengeDTOs.DailyChallengeTodayDTO;
import org.example.learningprojectserver.entities.DailyChallengeAttemptEntity;
import org.example.learningprojectserver.entities.DailyChallengeEntity;
import org.example.learningprojectserver.mappers.DailyChallengeEntityToQuestionDTOMapper;
import org.example.learningprojectserver.repository.DailyChallengeAttemptRepository;
import org.example.learningprojectserver.repository.DailyChallengeRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyChallengeServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Jerusalem");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    @Mock DailyChallengeRepository challengeRepository;
    @Mock DailyChallengeAttemptRepository attemptRepository;
    @Spy DailyChallengeEntityToQuestionDTOMapper mapper = new DailyChallengeEntityToQuestionDTOMapper();
    @InjectMocks DailyChallengeService service;

    /** In-memory tables behind the mocks. */
    private final Map<String, DailyChallengeEntity> challenges = new HashMap<>();
    private final Map<String, DailyChallengeAttemptEntity> attempts = new HashMap<>();
    private long nextId = 1;

    @BeforeEach
    void setUp() {
        at(TODAY.atTime(10, 0));
        List<Object[]> scope = new ArrayList<>();
        scope.add(new Object[]{"S1", "ח'"});
        lenient().when(challengeRepository.findStudentScope(anyString())).thenReturn(scope);
        lenient().when(challengeRepository.findBySchoolCodeAndGradeNameAndChallengeDate(anyString(), anyString(), any()))
                .thenAnswer(inv -> Optional.ofNullable(challenges.get(
                        inv.getArgument(0) + "|" + inv.getArgument(1) + "|" + inv.getArgument(2))));
        lenient().when(challengeRepository.save(any())).thenAnswer(inv -> {
            DailyChallengeEntity c = inv.getArgument(0);
            c.setId(nextId++);
            challenges.put(c.getSchoolCode() + "|" + c.getGradeName() + "|" + c.getChallengeDate(), c);
            return c;
        });
        lenient().when(attemptRepository.findByChallengeIdAndStudentUserId(anyLong(), anyString()))
                .thenAnswer(inv -> Optional.ofNullable(attempts.get(inv.getArgument(0) + "|" + inv.getArgument(1))));
        lenient().when(attemptRepository.save(any())).thenAnswer(inv -> {
            DailyChallengeAttemptEntity a = inv.getArgument(0);
            attempts.put(a.getChallengeId() + "|" + a.getStudentUserId(), a);
            return a;
        });
        lenient().when(attemptRepository.findCorrectDates(anyString(), any())).thenReturn(List.of());
    }

    @Test
    void sameQuestionForTheWholeGradeAndHiddenUntilStarted() {
        DailyChallengeTodayDTO first = service.getToday("s1").getData();
        DailyChallengeTodayDTO second = service.getToday("s2").getData();

        verify(challengeRepository, times(1)).save(any());
        assertEquals("NOT_STARTED", first.status());
        assertNull(first.questionText());
        assertEquals(first.subTopic(), second.subTopic());

        DailyChallengeTodayDTO s1 = service.start("s1").getData();
        DailyChallengeTodayDTO s2 = service.start("s2").getData();
        assertEquals("IN_PROGRESS", s1.status());
        assertNotNull(s1.questionText());
        assertEquals(s1.questionText(), s2.questionText());
        assertEquals(120, s1.secondsLeft());
    }

    @Test
    void correctAnswerInTimeCountsButTheSolutionStaysHidden() {
        service.start("s1");
        at(TODAY.atTime(10, 1));
        String answer = challenges.values().iterator().next().getAnswer();

        BasicResponse<DailyChallengeTodayDTO> res = service.answer("s1", " " + answer + " ");

        assertTrue(res.isSuccess());
        assertEquals("ANSWERED", res.getData().status());
        assertTrue(res.getData().correct());
        assertFalse(service.answer("s1", answer).isSuccess(), "one answer per day");
    }

    @Test
    void answerAfterTwoMinutesIsTimedOutAndNotCorrect() {
        service.start("s1");
        at(TODAY.atTime(10, 3));
        assertEquals("TIMED_OUT", service.getToday("s1").getData().status());

        String answer = challenges.values().iterator().next().getAnswer();
        DailyChallengeTodayDTO res = service.answer("s1", answer).getData();

        assertEquals("TIMED_OUT", res.status());
        assertFalse(res.correct());
    }

    @Test
    void answerWithoutStartIsRejected() {
        assertFalse(service.answer("s1", "5").isSuccess());
    }

    @Test
    void yesterdaysSolutionIsUnlockedToday() {
        service.start("s1");
        String answer = challenges.values().iterator().next().getAnswer();
        service.answer("s1", answer);

        at(TODAY.plusDays(1).atTime(8, 0));
        DailyChallengeTodayDTO next = service.getToday("s1").getData();

        assertNotNull(next.yesterday());
        assertEquals(answer, next.yesterday().challenge().answer());
        assertTrue(next.yesterday().attempted());
        assertTrue(next.yesterday().correct());
    }

    @Test
    void studentWithoutSchoolGetsAHebrewError() {
        when(challengeRepository.findStudentScope("lonely")).thenReturn(List.of());
        BasicResponse<DailyChallengeTodayDTO> res = service.getToday("lonely");
        assertFalse(res.isSuccess());
        assertNotNull(res.getErrorCode());
    }

    @Test
    void subTopicIsDeterministicAndFitsTheGrade() {
        String a = DailyChallengeService.pickSubTopic("S1", "ג'", TODAY);
        assertEquals(a, DailyChallengeService.pickSubTopic("S1", "ג'", TODAY));
        assertTrue(CompetitionCatalog.forGrade(3).contains(a));
        Set<String> overAMonth = new HashSet<>();
        for (int i = 0; i < 30; i++) overAMonth.add(DailyChallengeService.pickSubTopic("S1", "י'", TODAY.plusDays(i)));
        assertTrue(overAMonth.size() > 1, "the sub-topic changes from day to day");
    }

    @Test
    void streakCountsConsecutiveCorrectDays() {
        assertEquals(0, DailyChallengeService.streak(List.of(), TODAY));
        assertEquals(3, DailyChallengeService.streak(List.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2),
                TODAY.minusDays(5)), TODAY));
        // Today's challenge is still open: the streak up to yesterday stands.
        assertEquals(2, DailyChallengeService.streak(List.of(TODAY.minusDays(1), TODAY.minusDays(2)), TODAY));
        assertEquals(0, DailyChallengeService.streak(List.of(TODAY.minusDays(2)), TODAY));
    }

    private void at(LocalDateTime time) {
        service.clock = Clock.fixed(time.atZone(ZONE).toInstant(), ZONE);
    }
}
