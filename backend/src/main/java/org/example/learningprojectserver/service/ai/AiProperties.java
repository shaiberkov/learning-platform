package org.example.learningprojectserver.service.ai;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * All AI settings in one place. Every value has a default, so nothing is required in the
 * properties files; the only secret, the provider key, is read from the environment
 * ({@code GPT_TOKEN}) by the provider itself.
 */
@Getter
@Component
public class AiProperties {

    /** Which {@link org.example.learningprojectserver.service.ai.provider.AiProvider} to use, by its name(). */
    @Value("${ai.provider:openai}")
    private String provider;

    @Value("${ai.model:gpt-4o-mini}")
    private String model;

    @Value("${ai.vision-model:gpt-4o-mini}")
    private String visionModel;

    @Value("${ai.transcription-model:whisper-1}")
    private String transcriptionModel;

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${ai.timeout-seconds:60}")
    private int timeoutSeconds;

    /** Requests per user per day (Israel time). Cached answers do not count. */
    @Value("${ai.quota.student-daily:40}")
    private int studentDailyQuota;

    @Value("${ai.quota.staff-daily:150}")
    private int staffDailyQuota;

    /** USD per 1,000 tokens, used only to estimate cost per school. */
    @Value("${ai.price.input-per-1k:0.00015}")
    private double priceInputPer1k;

    @Value("${ai.price.output-per-1k:0.0006}")
    private double priceOutputPer1k;

    @Value("${ai.price.transcription-per-request:0.003}")
    private double priceTranscriptionPerRequest;

    /** Days a tutor conversation is kept before the retention job deletes it. */
    @Value("${ai.retention.days:30}")
    private int retentionDays;

    /** Also ask the provider's moderation endpoint about student input (free on OpenAI). */
    @Value("${ai.safety.provider-moderation:true}")
    private boolean providerModeration;

    @Value("${ai.tutor.history-messages:10}")
    private int tutorHistoryMessages;
}
