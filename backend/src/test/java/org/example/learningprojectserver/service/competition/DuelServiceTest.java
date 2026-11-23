package org.example.learningprojectserver.service.competition;

import org.example.learningprojectserver.dto.DuelDTOs.*;
import org.example.learningprojectserver.entities.DuelEntity;
import org.example.learningprojectserver.entities.DuelQuestionEntity;
import org.example.learningprojectserver.entities.TopicMasteryEntity;
import org.example.learningprojectserver.enums.DuelMode;
import org.example.learningprojectserver.enums.DuelStatus;
import org.example.learningprojectserver.mappers.DuelEntityToDuelSummaryDTOMapper;
import org.example.learningprojectserver.notification.publisher.NotificationEventPublisher;
import org.example.learningprojectserver.repository.DuelRepository;
import org.example.learningprojectserver.repository.TopicMasteryRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.adaptive.EloRating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DuelServiceTest {

    private static final String SUB_TOPIC = "חיבור מספרים שלמים";

    @Mock DuelRepository duelRepository;
    @Mock TopicMasteryRepository topicMasteryRepository;
    @Mock NotificationEventPublisher publisher;
    @Spy DuelEntityToDuelSummaryDTOMapper mapper = new DuelEntityToDuelSummaryDTOMapper();
    @InjectMocks DuelService service;

    private DuelEntity saved;

    @BeforeEach
    void setUp() {
        lenient().when(duelRepository.findStudentInfo("a")).thenReturn(rows(new Object[]{"אלה", "S1", 7L}));
        lenient().when(duelRepository.findStudentInfo("b")).thenReturn(rows(new Object[]{"בני", "S1", 7L}));
        lenient().when(duelRepository.findStudentInfo("x")).thenReturn(rows(new Object[]{"זר", "S2", 9L}));
        lenient().when(duelRepository.save(any())).thenAnswer(inv -> {
            saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        lenient().when(duelRepository.findForUpdate(1L)).thenAnswer(inv -> Optional.ofNullable(saved));
        lenient().when(duelRepository.findById(1L)).thenAnswer(inv -> Optional.ofNullable(saved));
    }

    @Test
    void inviteCreatesPendingDuelAtTheWeakerPlayersLevelAndNotifies() {
        when(topicMasteryRepository.findByStudentUserIdIn(anyCollection()))
                .thenReturn(List.of(mastery("a", 1600), mastery("b", 700)));

        BasicResponse<DuelStateDTO> res = service.invite("a", new DuelInviteRequest("b", SUB_TOPIC, DuelMode.COMPETITIVE));

        assertTrue(res.isSuccess());
        assertEquals(DuelStatus.PENDING, saved.getStatus());
        assertEquals(5, saved.getQuestions().size());
        assertEquals(EloRating.pickLevel(700), saved.getLevel());
        assertTrue(res.getData().questions().isEmpty(), "questions stay hidden until the duel is accepted");
        verify(publisher).publish(eq(List.of("b")), any());
    }

    @Test
    void inviteRejectsSelfOtherSchoolAndUnknownTopic() {
        assertFalse(service.invite("a", new DuelInviteRequest("a", SUB_TOPIC, DuelMode.COMPETITIVE)).isSuccess());
        assertFalse(service.invite("a", new DuelInviteRequest("x", SUB_TOPIC, DuelMode.COMPETITIVE)).isSuccess());
        assertFalse(service.invite("a", new DuelInviteRequest("b", "אינטגרלים", DuelMode.COMPETITIVE)).isSuccess());
        verify(duelRepository, never()).save(any());
    }

    @Test
    void onlyTheInvitedStudentCanAccept() {
        service.invite("a", new DuelInviteRequest("b", SUB_TOPIC, DuelMode.COMPETITIVE));

        assertFalse(service.accept("a", 1L).isSuccess());
        assertFalse(service.accept("x", 1L).isSuccess());
        BasicResponse<DuelStateDTO> res = service.accept("b", 1L);
        assertTrue(res.isSuccess());
        assertEquals(DuelStatus.ACTIVE, saved.getStatus());
        assertEquals(5, res.getData().questions().size());
        assertNull(res.getData().questions().get(0).correctAnswer(), "the answer is not sent during the duel");
    }

    @Test
    void competitiveDuelFinishesWhenBothAnsweredAndMoreCorrectWins() {
        service.invite("a", new DuelInviteRequest("b", SUB_TOPIC, DuelMode.COMPETITIVE));
        service.accept("b", 1L);

        for (DuelQuestionEntity q : new ArrayList<>(saved.getQuestions())) {
            assertTrue(service.answer("a", 1L, new DuelAnswerRequest(q.getPosition(), q.getAnswer())).isSuccess());
            String bAnswer = q.getPosition() == 0 ? q.getAnswer() : "wrong";
            assertTrue(service.answer("b", 1L, new DuelAnswerRequest(q.getPosition(), bAnswer)).isSuccess());
        }

        assertEquals(DuelStatus.FINISHED, saved.getStatus());
        assertEquals(5, saved.getChallengerCorrect());
        assertEquals(1, saved.getOpponentCorrect());
        assertEquals("CHALLENGER", saved.getOutcome());
        DuelStateDTO state = service.getState("b", 1L).getData();
        assertNotNull(state.questions().get(0).correctAnswer(), "answers are revealed once finished");
        assertEquals(5, state.duel().challengerAnswered());
    }

    @Test
    void answerRejectsDuplicatesStrangersAndInactiveDuels() {
        service.invite("a", new DuelInviteRequest("b", SUB_TOPIC, DuelMode.COMPETITIVE));
        assertFalse(service.answer("a", 1L, new DuelAnswerRequest(0, "1")).isSuccess(), "pending duel");

        service.accept("b", 1L);
        assertTrue(service.answer("a", 1L, new DuelAnswerRequest(0, "1")).isSuccess());
        assertFalse(service.answer("a", 1L, new DuelAnswerRequest(0, "2")).isSuccess(), "second answer");
        assertFalse(service.answer("x", 1L, new DuelAnswerRequest(1, "2")).isSuccess(), "not a participant");
        assertFalse(service.getState("x", 1L).isSuccess());
        assertEquals(1, saved.getChallengerAnswered());
    }

    @Test
    void eitherPlayerCanCancelOnlyBeforeTheStart() {
        service.invite("a", new DuelInviteRequest("b", SUB_TOPIC, DuelMode.COOP));
        assertNotNull(saved.getSystemScore());
        assertTrue(service.decline("a", 1L).isSuccess());
        assertEquals(DuelStatus.DECLINED, saved.getStatus());
        assertFalse(service.accept("b", 1L).isSuccess());
    }

    @Test
    void coopOutcomeComparesTheTeamWithTheSystemScore() {
        DuelEntity d = new DuelEntity();
        d.setMode(DuelMode.COOP);
        d.setSystemScore(6);
        d.setChallengerCorrect(4);
        d.setOpponentCorrect(3);
        assertEquals("TEAM", DuelService.outcome(d));
        d.setOpponentCorrect(2);
        assertEquals("DRAW", DuelService.outcome(d));
        d.setOpponentCorrect(0);
        assertEquals("SYSTEM", DuelService.outcome(d));
    }

    @Test
    void competitiveTieGoesToWhoeverFinishedFirst() {
        DuelEntity d = new DuelEntity();
        d.setMode(DuelMode.COMPETITIVE);
        d.setChallengerCorrect(3);
        d.setOpponentCorrect(3);
        LocalDateTime now = LocalDateTime.now();
        d.setChallengerFinishedAt(now.plusSeconds(5));
        d.setOpponentFinishedAt(now);
        assertEquals("OPPONENT", DuelService.outcome(d));
        d.setOpponentFinishedAt(d.getChallengerFinishedAt());
        assertEquals("DRAW", DuelService.outcome(d));
    }

    @Test
    void opponentsAreOrderedByMasteryGap() {
        when(duelRepository.findSchoolmates("S1", "a")).thenReturn(List.of(
                new Object[]{"far", "רחוק", 7L}, new Object[]{"near", "קרוב", 8L}, new Object[]{"mid", "בינוני", 7L}));
        when(topicMasteryRepository.findByStudentUserIdIn(anyCollection())).thenReturn(List.of(
                mastery("a", 1000), mastery("far", 1500), mastery("near", 1020), mastery("mid", 1200)));

        DuelOpponentsDTO res = service.getOpponents("a", SUB_TOPIC).getData();

        assertEquals(1000, res.myRating());
        assertEquals(List.of("near", "mid", "far"), res.opponents().stream().map(DuelOpponentDTO::userId).toList());
        assertTrue(res.opponents().get(0).recommended());
        assertFalse(res.opponents().get(2).recommended());
        assertTrue(res.opponents().get(1).sameClass());
    }

    @Test
    void duelLevelUsesTheWeakerPlayerOrTheCoopAverage() {
        assertEquals(EloRating.pickLevel(700), DuelService.duelLevel(DuelMode.COMPETITIVE, 1600, 700));
        assertEquals(EloRating.pickLevel(1150), DuelService.duelLevel(DuelMode.COOP, 1600, 700));
    }

    private static List<Object[]> rows(Object[] row) {
        List<Object[]> list = new ArrayList<>();
        list.add(row);
        return list;
    }

    private static TopicMasteryEntity mastery(String userId, double rating) {
        TopicMasteryEntity m = new TopicMasteryEntity();
        m.setStudentUserId(userId);
        m.setSubject("מתמטיקה");
        m.setSubTopic(SUB_TOPIC);
        m.setRating(rating);
        return m;
    }
}
