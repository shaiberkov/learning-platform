package org.example.learningprojectserver.service.ai;

import org.example.learningprojectserver.entities.AiUsageEntity;
import org.example.learningprojectserver.enums.Role;
import org.example.learningprojectserver.repository.AiUsageRepository;
import org.example.learningprojectserver.service.ai.provider.AiProvider;
import org.example.learningprojectserver.service.ai.provider.AiProviderException;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** [s05] Text to speech through the gateway: same configuration, quota and usage handling as completions. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiGatewaySpeechTest {

    @Mock private AiProvider provider;
    @Mock private AiUsageRepository usageRepository;

    private AiGateway gateway;
    private final AiUser student = new AiUser("s1", Role.STUDENT, "school-1");

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        ReflectionTestUtils.setField(properties, "provider", "openai");
        ReflectionTestUtils.setField(properties, "studentDailyQuota", 2);
        ReflectionTestUtils.setField(properties, "ttsModel", "tts-test");
        ReflectionTestUtils.setField(properties, "ttsVoice", "alloy");
        ReflectionTestUtils.setField(properties, "priceTtsPer1kChars", 0.015);
        when(provider.name()).thenReturn("openai");
        when(provider.isConfigured()).thenReturn(true);
        gateway = new AiGateway(List.of(provider), properties, usageRepository,
                new ConcurrentMapCacheManager(AiGateway.PROMPT_CACHE));
    }

    @Test
    void notConfiguredAnswersInHebrewWithoutCallingTheProvider() {
        when(provider.isConfigured()).thenReturn(false);

        AiException e = assertThrows(AiException.class, () -> gateway.speak(student, "TTS", "שלום"));

        assertEquals(AiGateway.NOT_CONFIGURED, e.getMessage());
        verify(provider, never()).speak(any(), any(), any());
    }

    @Test
    void speechIsRecordedAsUsage() {
        when(provider.speak("שלום", "alloy", "he")).thenReturn(new byte[]{9});

        assertArrayEquals(new byte[]{9}, gateway.speak(student, "TTS", "שלום"));

        ArgumentCaptor<AiUsageEntity> usage = ArgumentCaptor.forClass(AiUsageEntity.class);
        verify(usageRepository).save(usage.capture());
        assertEquals("TTS", usage.getValue().getFeature());
        assertEquals("tts-test", usage.getValue().getModel());
    }

    @Test
    void providerFailureBecomesAHebrewMessage() {
        when(provider.speak(any(), any(), any())).thenThrow(new AiProviderException("boom"));

        AiException e = assertThrows(AiException.class, () -> gateway.speak(student, "TTS", "שלום"));

        assertEquals(AiGateway.SPEECH_FAILED, e.getMessage());
    }
}
