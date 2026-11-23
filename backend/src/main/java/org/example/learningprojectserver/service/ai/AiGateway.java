package org.example.learningprojectserver.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.learningprojectserver.entities.AiUsageEntity;
import org.example.learningprojectserver.repository.AiUsageRepository;
import org.example.learningprojectserver.service.ai.provider.AiProvider;
import org.example.learningprojectserver.service.ai.provider.AiProviderException;
import org.example.learningprojectserver.service.ai.provider.AiRequest;
import org.example.learningprojectserver.service.ai.provider.AiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Consumer;

/**
 * The only way business code reaches a model. Every call goes through the same steps:
 * provider configured? → daily quota → prompt cache (when the caller allows it) → provider →
 * usage row (tokens, estimated cost, school). Errors come back as {@link AiException} with a
 * Hebrew message; nothing here throws a provider error at a controller.
 */
@Service
public class AiGateway {

    public static final ZoneId ZONE = ZoneId.of("Asia/Jerusalem");
    public static final String PROMPT_CACHE = "aiPromptCache";

    public static final String NOT_CONFIGURED = "עוזר ה-AI עדיין לא הופעל בשרת. פנו למנהל המערכת.";
    public static final String PROVIDER_FAILED = "עוזר ה-AI לא זמין כרגע. נסו שוב בעוד כמה דקות.";
    public static final String QUOTA_EXCEEDED = "הגעת למכסת הבקשות היומית לעוזר ה-AI. המכסה מתחדשת מחר.";

    private static final Logger log = LoggerFactory.getLogger(AiGateway.class);

    private final List<AiProvider> providers;
    private final AiProperties properties;
    private final AiUsageRepository usageRepository;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiGateway(List<AiProvider> providers, AiProperties properties,
                     AiUsageRepository usageRepository, CacheManager cacheManager) {
        this.providers = providers;
        this.properties = properties;
        this.usageRepository = usageRepository;
        this.cacheManager = cacheManager;
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    AiProvider provider() {
        return providers.stream()
                .filter(p -> p.name().equalsIgnoreCase(properties.getProvider()))
                .findFirst()
                .orElse(null);
    }

    public boolean isAvailable() {
        AiProvider provider = provider();
        return provider != null && provider.isConfigured();
    }

    public int dailyLimit(AiUser user) {
        return user.isStudent() ? properties.getStudentDailyQuota() : properties.getStaffDailyQuota();
    }

    public int usedToday(AiUser user) {
        LocalDateTime startOfDay = LocalDate.now(ZONE).atStartOfDay();
        return (int) usageRepository.countBilledSince(user.userId(), startOfDay);
    }

    public int remainingToday(AiUser user) {
        return Math.max(0, dailyLimit(user) - usedToday(user));
    }

    /** Throws when the AI cannot serve this user right now. Call before any expensive preparation. */
    public void ensureCanServe(AiUser user) {
        if (!isAvailable()) {
            throw new AiException(NOT_CONFIGURED);
        }
        if (remainingToday(user) <= 0) {
            throw new AiException(QUOTA_EXCEEDED);
        }
    }

    /**
     * One-shot completion. With {@code cacheable}, an identical request (same model, messages and
     * settings) within the cache TTL is answered from {@value #PROMPT_CACHE} at no cost.
     */
    public String complete(AiUser user, String feature, AiRequest request, boolean cacheable) {
        AiRequest resolved = resolveModel(request);
        Cache cache = cacheable ? cacheManager.getCache(PROMPT_CACHE) : null;
        String key = cache != null ? cacheKey(resolved) : null;
        if (cache != null) {
            String hit = cache.get(key, String.class);
            if (hit != null) {
                record(user, feature, new AiResult(hit, 0, 0, resolved.model()), true, 0);
                return hit;
            }
        }
        ensureCanServe(user);
        try {
            AiResult result = provider().complete(resolved);
            record(user, feature, result, false, cost(result));
            if (cache != null && !result.text().isBlank()) {
                cache.put(key, result.text());
            }
            return result.text();
        } catch (AiProviderException e) {
            log.warn("AI {} failed for {}: {}", feature, user.userId(), e.getMessage());
            throw new AiException(PROVIDER_FAILED);
        }
    }

    /** Streams the answer through {@code onDelta}; returns the whole text. Never cached. */
    public String stream(AiUser user, String feature, AiRequest request, Consumer<String> onDelta) {
        ensureCanServe(user);
        AiRequest resolved = resolveModel(request);
        try {
            AiResult result = provider().stream(resolved, onDelta);
            record(user, feature, result, false, cost(result));
            return result.text();
        } catch (AiProviderException e) {
            log.warn("AI stream {} failed for {}: {}", feature, user.userId(), e.getMessage());
            throw new AiException(PROVIDER_FAILED);
        }
    }

    public String transcribe(AiUser user, byte[] audio, String fileName) {
        ensureCanServe(user);
        try {
            String text = provider().transcribe(audio, fileName, "he");
            record(user, "TRANSCRIBE", new AiResult(text, 0, 0, properties.getTranscriptionModel()), false,
                    properties.getPriceTranscriptionPerRequest());
            return text;
        } catch (AiProviderException e) {
            log.warn("AI transcription failed for {}: {}", user.userId(), e.getMessage());
            throw new AiException("לא הצלחנו להמיר את ההקלטה לטקסט. נסו להקליד את השאלה.");
        }
    }

    // [s05] Text to speech for the study podcast.
    public static final String SPEECH_FAILED = "לא הצלחנו להפיק הקראה קולית כרגע. אפשר להשתמש בהקראה של הדפדפן.";

    /** Text to speech (MP3). Counts against the daily quota like any other request. */
    public byte[] speak(AiUser user, String feature, String text) {
        ensureCanServe(user);
        try {
            byte[] audio = provider().speak(text, properties.getTtsVoice(), "he");
            record(user, feature, new AiResult("", 0, 0, properties.getTtsModel()), false,
                    text.length() / 1000.0 * properties.getPriceTtsPer1kChars());
            return audio;
        } catch (AiProviderException e) {
            log.warn("AI speech failed for {}: {}", user.userId(), e.getMessage());
            throw new AiException(SPEECH_FAILED);
        }
    }

    /** The provider's moderation verdict; false when the provider has none or is unavailable. */
    public boolean flaggedByProviderModeration(String text) {
        if (!properties.isProviderModeration() || !isAvailable()) {
            return false;
        }
        return provider().isFlaggedByModeration(text);
    }

    private AiRequest resolveModel(AiRequest request) {
        if (request.model() != null) {
            return request;
        }
        return request.withModel(request.hasImage() ? properties.getVisionModel() : properties.getModel());
    }

    double cost(AiResult result) {
        return result.promptTokens() / 1000.0 * properties.getPriceInputPer1k()
                + result.completionTokens() / 1000.0 * properties.getPriceOutputPer1k();
    }

    private void record(AiUser user, String feature, AiResult result, boolean cached, double cost) {
        AiUsageEntity usage = new AiUsageEntity();
        usage.setUserId(user.userId());
        usage.setSchoolCode(user.schoolCode());
        usage.setFeature(feature);
        usage.setProvider(properties.getProvider());
        usage.setModel(result.model());
        usage.setPromptTokens(result.promptTokens());
        usage.setCompletionTokens(result.completionTokens());
        usage.setCostUsd(cost);
        usage.setCached(cached);
        usage.setCreatedAt(now());
        try {
            usageRepository.save(usage);
        } catch (RuntimeException e) {
            // Losing a usage row must never cost the user their answer.
            log.warn("Could not record AI usage: {}", e.getMessage());
        }
    }

    String cacheKey(AiRequest request) {
        try {
            String json = properties.getProvider() + "|" + objectMapper.writeValueAsString(request);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
