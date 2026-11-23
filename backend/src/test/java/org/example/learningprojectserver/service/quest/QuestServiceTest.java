package org.example.learningprojectserver.service.quest;

import org.example.learningprojectserver.dto.QuestDTOs;
import org.example.learningprojectserver.entities.QuestProgressEntity;
import org.example.learningprojectserver.entities.QuestionEntity;
import org.example.learningprojectserver.entities.StudentCosmeticEntity;
import org.example.learningprojectserver.mappers.StudentCosmeticEntityToItemMapper;
import org.example.learningprojectserver.repository.QuestProgressRepository;
import org.example.learningprojectserver.repository.StudentCosmeticRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.EngagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuestServiceTest {

    private static final String STUDENT = "s1";

    @Mock QuestProgressRepository progressRepository;
    @Mock StudentCosmeticRepository cosmeticRepository;
    @Mock QuestQuestionSource questionSource;
    @Mock EngagementService engagementService;

    private QuestService service;
    private final List<StudentCosmeticEntity> owned = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new QuestService(progressRepository, cosmeticRepository, questionSource, engagementService,
                new StudentCosmeticEntityToItemMapper());
        when(questionSource.generate(anyString(), anyInt()))
                .thenReturn(new QuestionEntity("מתמטיקה", "מספרים שלמים", "חיבור מספרים שלמים", "2+3=?", "5"));
        when(cosmeticRepository.findByStudentUserId(STUDENT)).thenReturn(owned);
        when(cosmeticRepository.save(any())).thenAnswer(inv -> {
            owned.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
    }

    private QuestProgressEntity progress(String questCode, int chapter, int correct) {
        QuestProgressEntity p = new QuestProgressEntity();
        p.setStudentUserId(STUDENT);
        p.setQuestCode(questCode);
        p.setChapterIndex(chapter);
        p.setChapterCorrect(correct);
        p.setPendingQuestion("2+3=?");
        p.setPendingAnswer("5");
        p.setStartedAt(LocalDateTime.now());
        when(progressRepository.findByStudentUserIdAndQuestCode(STUDENT, questCode)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void answersMatchAcceptsEquivalentNumbers() {
        assertTrue(QuestService.answersMatch(" 5 ", "5"));
        assertTrue(QuestService.answersMatch("2/4", "1/2"));
        assertTrue(QuestService.answersMatch("0.5", "1/2"));
        assertTrue(QuestService.answersMatch("‏-3", "-3"));
        assertFalse(QuestService.answersMatch("4", "5"));
        assertFalse(QuestService.answersMatch("", "5"));
        assertFalse(QuestService.answersMatch("abc", "5"));
        assertFalse(QuestService.answersMatch("1/0", "5"));
    }

    @Test
    void startCreatesProgressWithAQuestionButNeverSendsTheAnswer() {
        when(progressRepository.findByStudentUserIdAndQuestCode(STUDENT, "MARS")).thenReturn(Optional.empty());

        BasicResponse<QuestDTOs.QuestState> response = service.start(STUDENT, "MARS");

        assertTrue(response.isSuccess());
        assertEquals("2+3=?", response.getData().question());
        assertEquals(QuestDTOs.IN_PROGRESS, response.getData().status());
        ArgumentCaptor<QuestProgressEntity> saved = ArgumentCaptor.forClass(QuestProgressEntity.class);
        verify(progressRepository).save(saved.capture());
        assertEquals("5", saved.getValue().getPendingAnswer());
        assertEquals(STUDENT, saved.getValue().getStudentUserId());
    }

    @Test
    void unknownQuestIsRejected() {
        assertFalse(service.start(STUDENT, "NOPE").isSuccess());
        assertFalse(service.answer(STUDENT, "NOPE", "1").isSuccess());
    }

    @Test
    void correctAnswerCountsAndGivesXp() {
        QuestProgressEntity p = progress("MARS", 0, 0);

        BasicResponse<QuestDTOs.AnswerResult> response = service.answer(STUDENT, "MARS", "5");

        assertTrue(response.getData().correct());
        assertFalse(response.getData().chapterCompleted());
        assertEquals(1, p.getChapterCorrect());
        verify(engagementService).recordActivity(eq(STUDENT), eq(QuestService.XP_CORRECT), eq(List.of()));
    }

    @Test
    void enoughCorrectAnswersMoveToTheNextChapter() {
        QuestProgressEntity p = progress("MARS", 0, 2);

        BasicResponse<QuestDTOs.AnswerResult> response = service.answer(STUDENT, "MARS", "5");

        assertTrue(response.getData().chapterCompleted());
        assertFalse(response.getData().questCompleted());
        assertEquals(1, p.getChapterIndex());
        assertEquals(0, p.getChapterCorrect());
        assertNotNull(p.getPendingAnswer());
    }

    @Test
    void finishingTheLastChapterUnlocksRewardsAndBadge() {
        int last = QuestCatalog.quest("MARS").chapters().size() - 1;
        QuestProgressEntity p = progress("MARS", last, 2);

        BasicResponse<QuestDTOs.AnswerResult> response = service.answer(STUDENT, "MARS", "5");

        assertTrue(response.getData().questCompleted());
        assertNotNull(p.getCompletedAt());
        assertNull(p.getPendingAnswer());
        assertEquals(2, response.getData().unlocked().size());
        assertEquals(QuestDTOs.COMPLETED, response.getData().state().status());
        assertNull(response.getData().state().question());
        verify(cosmeticRepository, times(2)).save(any(StudentCosmeticEntity.class));
        verify(engagementService).recordActivity(eq(STUDENT), eq(QuestService.XP_CORRECT + QuestService.XP_QUEST_DONE),
                argThat(list -> list.size() == 1 && list.get(0).code().equals("QUEST:MARS")));
    }

    @Test
    void wrongAnswersRevealAfterMaxTriesAndReplaceTheQuestion() {
        QuestProgressEntity p = progress("MARS", 0, 1);

        for (int i = 1; i < QuestService.MAX_TRIES; i++) {
            BasicResponse<QuestDTOs.AnswerResult> r = service.answer(STUDENT, "MARS", "7");
            assertFalse(r.getData().correct());
            assertNull(r.getData().revealedAnswer());
        }
        BasicResponse<QuestDTOs.AnswerResult> last = service.answer(STUDENT, "MARS", "7");

        assertEquals("5", last.getData().revealedAnswer());
        assertEquals(0, p.getWrongAttempts());
        assertEquals(1, p.getChapterCorrect());
        verify(questionSource, atLeastOnce()).generate(anyString(), anyInt());
        verify(engagementService, never()).recordActivity(anyString(), anyInt(), anyList());
    }

    @Test
    void answeringACompletedQuestIsRejected() {
        QuestProgressEntity p = progress("MARS", 4, 0);
        p.setCompletedAt(LocalDateTime.now());

        assertFalse(service.answer(STUDENT, "MARS", "5").isSuccess());
    }

    @Test
    void lockedItemCannotBeEquipped() {
        BasicResponse<QuestDTOs.Cosmetics> response = service.equip(STUDENT, "ROOM_SPACE");

        assertFalse(response.isSuccess());
    }

    @Test
    void equippingAnUnlockedItemReplacesTheSameKindOnly() {
        owned.add(cosmetic("AVATAR_ASTRONAUT", QuestCatalog.AVATAR, true));
        owned.add(cosmetic("AVATAR_PIRATE", QuestCatalog.AVATAR, false));
        owned.add(cosmetic("ROOM_SPACE", QuestCatalog.ROOM, true));

        BasicResponse<QuestDTOs.Cosmetics> response = service.equip(STUDENT, "AVATAR_PIRATE");

        assertTrue(response.isSuccess());
        assertEquals("AVATAR_PIRATE", response.getData().avatar().code());
        assertEquals("ROOM_SPACE", response.getData().room().code());
        assertFalse(owned.get(0).isEquipped());
    }

    @Test
    void defaultsAreShownWhenNothingIsEquipped() {
        QuestDTOs.Cosmetics cosmetics = service.getCosmetics(STUDENT).getData();

        assertEquals(QuestCatalog.DEFAULT_AVATAR.code(), cosmetics.avatar().code());
        assertEquals(QuestCatalog.DEFAULT_ROOM.code(), cosmetics.room().code());
        assertTrue(cosmetics.items().stream().filter(i -> i.code().equals("ROOM_SPACE")).noneMatch(QuestDTOs.CosmeticItem::unlocked));
    }

    @Test
    void everyChapterUsesASubTopicTheGeneratorsKnow() {
        QuestQuestionSource real = new QuestQuestionSource();
        for (QuestCatalog.Quest quest : QuestCatalog.QUESTS) {
            for (QuestCatalog.Chapter chapter : quest.chapters()) {
                QuestionEntity q = real.generate(chapter.subTopic(), chapter.difficulty());
                assertNotNull(q.getQuestionText(), chapter.subTopic());
                assertNotNull(q.getAnswer(), chapter.subTopic());
            }
            for (String item : quest.rewardItems()) {
                assertNotNull(QuestCatalog.item(item), item);
            }
        }
    }

    private static StudentCosmeticEntity cosmetic(String code, String kind, boolean equipped) {
        StudentCosmeticEntity c = new StudentCosmeticEntity();
        c.setStudentUserId(STUDENT);
        c.setItemCode(code);
        c.setKind(kind);
        c.setEquipped(equipped);
        c.setUnlockedAt(LocalDateTime.now());
        return c;
    }
}
