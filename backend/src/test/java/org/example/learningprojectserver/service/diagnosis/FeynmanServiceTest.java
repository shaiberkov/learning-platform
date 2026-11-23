package org.example.learningprojectserver.service.diagnosis;

import org.example.learningprojectserver.dto.DiagnosisDTOs.FeynmanExplanationDTO;
import org.example.learningprojectserver.dto.DiagnosisDTOs.FeynmanSubmitRequest;
import org.example.learningprojectserver.entities.FeynmanExplanationEntity;
import org.example.learningprojectserver.enums.Role;
import org.example.learningprojectserver.mappers.FeynmanExplanationEntityToDTOMapper;
import org.example.learningprojectserver.repository.FeynmanExplanationRepository;
import org.example.learningprojectserver.repository.TopicMasteryRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.ai.AiException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeynmanServiceTest {

    private static final String KEY = "equation-divide-both-sides";
    private static final String GOOD = "משוואה היא כמו מאזניים: שני הצדדים שווים. כדי לבודד את x מחלקים את שני הצדדים "
            + "באותו מספר, וכך השוויון נשמר. אסור לחלק באפס כי זה לא מוגדר.";

    @Mock AiGateway gateway;
    @Mock AiUserResolver userResolver;
    @Mock FeynmanExplanationRepository repository;
    @Mock TopicMasteryRepository topicMasteryRepository;

    FeynmanService service;

    @BeforeEach
    void setUp() {
        service = new FeynmanService(gateway, userResolver, new ContentSafetyFilter(), repository,
                topicMasteryRepository, new FeynmanExplanationEntityToDTOMapper());
    }

    @Test
    void withoutAiTheKeyConceptCheckIsSavedWithTheNotConfiguredNotice() {
        when(userResolver.resolve("s1")).thenReturn(new AiUser("s1", Role.STUDENT, "school-1"));
        when(gateway.isAvailable()).thenReturn(false);

        BasicResponse<FeynmanExplanationDTO> response = service.submit("s1", new FeynmanSubmitRequest(KEY, GOOD, "TEXT"));

        assertTrue(response.isSuccess());
        assertTrue(response.getErrorCode().startsWith(AiGateway.NOT_CONFIGURED));
        assertEquals("KEYWORDS", response.getData().evaluatedBy());
        assertEquals(100, response.getData().score());
        assertTrue(response.getData().gaps().isEmpty());
        verify(gateway, never()).complete(any(), any(), any(), anyBoolean());
        verify(repository).save(any(FeynmanExplanationEntity.class));
    }

    @Test
    void shortExplanationWithoutKeyIdeasGetsGaps() {
        FeynmanPromptCatalog.Evaluation evaluation = FeynmanPromptCatalog.evaluateByKeywords(
                FeynmanPromptCatalog.find(KEY).orElseThrow(), "ככה המורה אמרה לעשות");
        assertTrue(evaluation.score() <= 40);
        assertTrue(evaluation.gaps().size() >= 4);
    }

    @Test
    void aiScoreAndGapsAreStored() {
        when(userResolver.resolve("s1")).thenReturn(new AiUser("s1", Role.STUDENT, "school-1"));
        when(gateway.isAvailable()).thenReturn(true);
        when(gateway.complete(any(), eq(FeynmanService.FEATURE), any(), eq(true)))
                .thenReturn("{\"score\": 72, \"gaps\": [\"לא הוסבר למה אסור לחלק באפס\"], \"feedback\": \"כיוון טוב\"}");

        BasicResponse<FeynmanExplanationDTO> response = service.submit("s1", new FeynmanSubmitRequest(KEY, GOOD, "VOICE"));

        assertTrue(response.isSuccess());
        assertNull(response.getErrorCode());
        ArgumentCaptor<FeynmanExplanationEntity> saved = ArgumentCaptor.forClass(FeynmanExplanationEntity.class);
        verify(repository).save(saved.capture());
        assertEquals(72, saved.getValue().getScore());
        assertEquals("AI", saved.getValue().getEvaluatedBy());
        assertEquals("VOICE", saved.getValue().getInputMode());
        assertEquals(List.of("לא הוסבר למה אסור לחלק באפס"), response.getData().gaps());
    }

    @Test
    void quotaErrorFallsBackToKeyConcepts() {
        when(userResolver.resolve("s1")).thenReturn(new AiUser("s1", Role.STUDENT, "school-1"));
        when(gateway.isAvailable()).thenReturn(true);
        when(gateway.complete(any(), any(), any(), anyBoolean())).thenThrow(new AiException(AiGateway.QUOTA_EXCEEDED));

        BasicResponse<FeynmanExplanationDTO> response = service.submit("s1", new FeynmanSubmitRequest(KEY, GOOD, "TEXT"));

        assertTrue(response.isSuccess());
        assertEquals("KEYWORDS", response.getData().evaluatedBy());
        assertTrue(response.getErrorCode().startsWith(AiGateway.QUOTA_EXCEEDED));
    }

    @Test
    void unknownPromptAndTooShortTextAreRejected() {
        assertFalse(service.submit("s1", new FeynmanSubmitRequest("nope", GOOD, "TEXT")).isSuccess());
        assertFalse(service.submit("s1", new FeynmanSubmitRequest(KEY, "קצר", "TEXT")).isSuccess());
        verifyNoInteractions(repository);
    }

    @Test
    void teacherSeesOnlyOwnClasses() {
        when(topicMasteryRepository.findTeacherClassRooms("t1")).thenReturn(List.<Object[]>of(new Object[]{5L, "ז1", "ז"}));
        assertFalse(service.classExplanations("t1", 9L).isSuccess());
        verifyNoInteractions(repository);
    }
}
