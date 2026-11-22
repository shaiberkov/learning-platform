package org.example.learningprojectserver.service.ai.provider;

/** A provider call failed. The message is technical (for logs), never shown to users. */
public class AiProviderException extends RuntimeException {
    public AiProviderException(String message) {
        super(message);
    }

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
