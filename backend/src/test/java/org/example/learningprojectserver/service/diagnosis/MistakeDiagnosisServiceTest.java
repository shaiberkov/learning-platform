package org.example.learningprojectserver.service.diagnosis;

import org.example.learningprojectserver.dto.DiagnosisDTOs.ClassMistakeSummaryDTO;
import org.example.learningprojectserver.dto.DiagnosisDTOs.MistakeDiagnosisDTO;
import org.example.learningprojectserver.dto.DiagnosisDTOs.MistakeItemDTO;
import org.example.learningprojectserver.entities.QuestionEntity;
import org.example.learningprojectserver.entities.ReviewItemEntity;
import org.example.learningprojectserver.enums.MistakeType;
import org.example.learningprojectserver.enums.Role;
import org.example.learningprojectserver.mappers.ReviewItemEntityToMistakeItemMapper;
import org.example.learningprojectserver.repository.ReviewItemRepository;
import org.example.learningprojectserver.repository.TopicMasteryRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.ReviewService;
import org.example.learningprojectserver.service.ai.AiGateway;
import org.example.learningprojectserver.service.ai.AiUser;
import org.example.learningprojectserver.service.ai.AiUserResolver;
import org.example.learningprojectserver.service.ai.safety.ContentSafetyFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MistakeDiagnosisServiceTest {

    @Mock ReviewItemRepository reviewItemRepository;
    @Mock TopicMasteryRepository topicMasteryRepository;
    @Mock AiGateway gateway;
    @Mock AiUserResolver userResolver;

    MistakeDiagnosisService service;

    @BeforeEach
    void setUp() {
        service = new MistakeDiagnosisService(reviewItemRepository, topicMasteryRepository,
                new ReviewItemEntityToMistakeItemMapper(), gateway, userResolver, new ContentSafetyFilter());
    }

    private static ReviewItemEntity item(MistakeType type) {
        ReviewItemEntity i = new ReviewItemEntity();
        i.setId(3L);
        i.setStudentUserId("s1");
        i.setQuestionText("6*7=?");
        i.setAnswer("42");
        i.setGivenAnswer("41");
        i.setErrorType(type);
        i.setErrorTypeSource("RULE");
        return i;
    }

    @Test
    void addMistakeStoresTheGivenAnswerAndItsType() {
        ReviewService reviewService = new ReviewService(reviewItemRepository);
        QuestionEntity q = new QuestionEntity("מתמטיקה", "מספרים שלמים", "כפל מספרים שלמים", "6*7=?", "42");
        q.setId(7L);
        when(reviewItemRepository.findByStudentUserIdAndQuestionId("s1", 7L)).thenReturn(Optional.empty());

        reviewService.addMistake("s1", q, "PRACTICE", "-42");

        ArgumentCaptor<ReviewItemEntity> saved = ArgumentCaptor.forClass(ReviewItemEntity.class);
        verify(reviewItemRepository).save(saved.capture());
        assertEquals("-42", saved.getValue().getGivenAnswer());
        assertEquals(MistakeType.CALCULATION, saved.getValue().getErrorType());
        assertEquals("RULE", saved.getValue().getErrorTypeSource());
    }

    @Test
    void summaryCountsPerTypeAndOldItemsAsUnclassified() {
        when(reviewItemRepository.countByErrorType("s1")).thenReturn(List.of(
                new Object[]{MistakeType.CALCULATION, 4L},
                new Object[]{MistakeType.CONCEPTUAL, 1L},
                new Object[]{null, 2L}));
        when(reviewItemRepository.findTop200ByStudentUserIdOrderByCreatedAtDescIdDesc("s1")).thenReturn(List.of(item(null)));

        MistakeDiagnosisDTO dto = service.diagnosis("s1").getData();

        assertEquals(4L, dto.counts().get("CALCULATION"));
        assertEquals(2L, dto.counts().get("UNCLASSIFIED"));
        assertEquals(7L, dto.total());
        assertEquals("CALCULATION", dto.dominantType());
        assertEquals("UNCLASSIFIED", dto.items().get(0).errorType());
    }

    @Test
    void selfCheckComparesWithoutChangingTheSchedule() {
        ReviewItemEntity i = item(MistakeType.CALCULATION);
        when(reviewItemRepository.findByIdAndStudentUserId(3L, "s1")).thenReturn(Optional.of(i));

        assertTrue(service.selfCheck("s1", 3L, " 42 ").getData().correct());
        assertFalse(service.selfCheck("s1", 3L, "41").getData().correct());
        verify(reviewItemRepository, never()).save(any());
    }

    @Test
    void aiAnalysisWithoutAKeyAnswersNotConfigured() {
        when(reviewItemRepository.findByIdAndStudentUserId(3L, "s1")).thenReturn(Optional.of(item(MistakeType.CALCULATION)));
        when(gateway.isAvailable()).thenReturn(false);

        BasicResponse<MistakeItemDTO> response = service.analyzeWithAi("s1", 3L);

        assertFalse(response.isSuccess());
        assertEquals(AiGateway.NOT_CONFIGURED, response.getErrorCode());
        verify(reviewItemRepository, never()).save(any());
    }

    @Test
    void aiAnalysisOverridesTheRuleType() {
        ReviewItemEntity i = item(MistakeType.CALCULATION);
        when(reviewItemRepository.findByIdAndStudentUserId(3L, "s1")).thenReturn(Optional.of(i));
        when(gateway.isAvailable()).thenReturn(true);
        when(userResolver.resolve("s1")).thenReturn(new AiUser("s1", Role.STUDENT, "school-1"));
        when(gateway.complete(any(), eq(MistakeDiagnosisService.FEATURE), any(), eq(true)))
                .thenReturn("{\"type\": \"conceptual\", \"explanation\": \"נראה שחיברת במקום לכפול\"}");

        BasicResponse<MistakeItemDTO> response = service.analyzeWithAi("s1", 3L);

        assertTrue(response.isSuccess());
        assertEquals("CONCEPTUAL", response.getData().errorType());
        assertEquals("AI", i.getErrorTypeSource());
        verify(reviewItemRepository).save(i);
    }

    @Test
    void classSummaryOnlyForTheTeachersOwnClass() {
        when(topicMasteryRepository.findTeacherClassRooms("t1")).thenReturn(List.<Object[]>of(new Object[]{5L, "ז1", "ז"}));
        assertFalse(service.classSummary("t1", 9L).isSuccess());

        when(topicMasteryRepository.findStudentsOfClassRoom(5L)).thenReturn(List.<Object[]>of(new Object[]{"s1", "dana"}));
        when(reviewItemRepository.countByStudentAndErrorType(List.of("s1")))
                .thenReturn(List.<Object[]>of(new Object[]{"s1", MistakeType.MISREAD, 3L}));
        ClassMistakeSummaryDTO dto = service.classSummary("t1", 5L).getData();
        assertEquals(3L, dto.totals().get("MISREAD"));
        assertEquals(3L, dto.students().get(0).counts().get("MISREAD"));
    }
}
