package org.example.learningprojectserver.service.ai;

import org.example.learningprojectserver.entities.AiUsageEntity;
import org.example.learningprojectserver.enums.Role;
import org.example.learningprojectserver.repository.AiUsageRepository;
import org.example.learningprojectserver.service.ai.provider.AiMessage;
import org.example.learningprojectserver.service.ai.provider.AiProvider;
import org.example.learningprojectserver.service.ai.provider.AiProviderException;
import org.example.learningprojectserver.service.ai.provider.AiRequest;
import org.example.learningprojectserver.service.ai.provider.AiResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiGatewayTest {

    @Mock
    private AiProvider provider;
    @Mock
    private AiUsageRepository usageRepository;

    private AiProperties properties;
    private AiGateway gateway;
    private final AiUser student = new AiUser("s1", Role.STUDENT, "school-1");
    private final AiRequest request = AiRequest.json(List.of(AiMessage.user("hi")), 100);

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        ReflectionTestUtils.setField(properties, "provider", "openai");
        ReflectionTestUtils.setField(properties, "model", "test-model");
        ReflectionTestUtils.setField(properties, "visionModel", "test-vision");
        ReflectionTestUtils.setField(properties, "studentDailyQuota", 2);
        ReflectionTestUtils.setField(properties, "staffDailyQuota", 5);
        ReflectionTestUtils.setField(properties, "priceInputPer1k", 0.001);
        ReflectionTestUtils.setField(properties, "priceOutputPer1k", 0.002);
        when(provider.name()).thenReturn("openai");
        when(provider.isConfigured()).thenReturn(true);
        gateway = new AiGateway(List.of(provider), properties, usageRepository,
                new ConcurrentMapCacheManager(AiGateway.PROMPT_CACHE));
    }

    @Test
    void missingKeyGivesAHebrewErrorAndNoCall() {
        when(provider.isConfigured()).thenReturn(false);
        AiException e = assertThrows(AiException.class, () -> gateway.complete(student, "X", request, false));
        assertEquals(AiGateway.NOT_CONFIGURED, e.getMessage());
        verify(provider, never()).complete(any());
    }

    @Test
    void quotaIsEnforcedPerUserPerDay() {
        when(usageRepository.countBilledSince(eq("s1"), any())).thenReturn(2L);
        AiException e = assertThrows(AiException.class, () -> gateway.complete(student, "X", request, false));
        assertEquals(AiGateway.QUOTA_EXCEEDED, e.getMessage());
    }

    @Test
    void usageIsRecordedWithSchoolTokensAndCost() {
        when(provider.complete(any())).thenReturn(new AiResult("ok", 1000, 500, "test-model"));

        assertEquals("ok", gateway.complete(student, "GRADING", request, false));

        ArgumentCaptor<AiUsageEntity> usage = ArgumentCaptor.forClass(AiUsageEntity.class);
        verify(usageRepository).save(usage.capture());
        assertEquals("school-1", usage.getValue().getSchoolCode());
        assertEquals("GRADING", usage.getValue().getFeature());
        assertEquals(0.002, usage.getValue().getCostUsd(), 1e-9);
        assertFalse(usage.getValue().isCached());
    }

    @Test
    void identicalCacheableRequestIsServedFromCacheForFree() {
        when(provider.complete(any())).thenReturn(new AiResult("answer", 10, 10, "test-model"));

        gateway.complete(student, "QUESTION_GEN", request, true);
        // Even with the quota used up, a cache hit is still served.
        when(usageRepository.countBilledSince(anyString(), any())).thenReturn(99L);
        assertEquals("answer", gateway.complete(student, "QUESTION_GEN", request, true));

        verify(provider, times(1)).complete(any());
        ArgumentCaptor<AiUsageEntity> usage = ArgumentCaptor.forClass(AiUsageEntity.class);
        verify(usageRepository, times(2)).save(usage.capture());
        assertTrue(usage.getAllValues().get(1).isCached());
        assertEquals(0.0, usage.getAllValues().get(1).getCostUsd());
    }

    @Test
    void providerFailureBecomesAHebrewError() {
        when(provider.complete(any())).thenThrow(new AiProviderException("500"));
        AiException e = assertThrows(AiException.class, () -> gateway.complete(student, "X", request, false));
        assertEquals(AiGateway.PROVIDER_FAILED, e.getMessage());
    }

    @Test
    void modelComesFromConfigurationAndVisionUsesTheVisionModel() {
        when(provider.complete(any())).thenReturn(new AiResult("ok", 1, 1, "m"));
        gateway.complete(student, "X", request, false);
        gateway.complete(student, "X", AiRequest.chat(List.of(AiMessage.userWithImage("x", "data:image/png;base64,AA"))), false);

        ArgumentCaptor<AiRequest> sent = ArgumentCaptor.forClass(AiRequest.class);
        verify(provider, times(2)).complete(sent.capture());
        assertEquals("test-model", sent.getAllValues().get(0).model());
        assertEquals("test-vision", sent.getAllValues().get(1).model());
    }
}
