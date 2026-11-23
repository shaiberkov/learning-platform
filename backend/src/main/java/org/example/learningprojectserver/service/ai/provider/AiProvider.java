package org.example.learningprojectserver.service.ai.provider;

import java.util.function.Consumer;

/**
 * A language-model provider. Pick one with {@code ai.provider} (its {@link #name()}) and a model
 * with {@code ai.model}; nothing else in the code names a vendor. Business code never calls a
 * provider directly: it goes through {@link org.example.learningprojectserver.service.ai.AiGateway},
 * which adds quota, safety, caching and cost tracking.
 */
public interface AiProvider {

    String name();

    /** False when the provider has no key/configuration; the gateway then answers with a Hebrew error. */
    boolean isConfigured();

    AiResult complete(AiRequest request);

    /** Streams the answer, calling {@code onDelta} with each new piece of text, and returns the whole result. */
    AiResult stream(AiRequest request, Consumer<String> onDelta);

    /** Speech to text. Returns the transcript. */
    default String transcribe(byte[] audio, String fileName, String language) {
        throw new AiProviderException(name() + " does not support transcription");
    }

    // [s05] Text to speech for the study podcast.
    /** Text to speech. Returns the audio bytes (MP3). */
    default byte[] speak(String text, String voice, String language) {
        throw new AiProviderException(name() + " does not support text to speech");
    }

    /** True when the provider's own moderation flags the text. Providers without one return false. */
    default boolean isFlaggedByModeration(String text) {
        return false;
    }
}
