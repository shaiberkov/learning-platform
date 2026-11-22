package org.example.learningprojectserver.service.ai;

/** An AI request that cannot be served. The message is Hebrew and is shown to the user as is. */
public class AiException extends RuntimeException {
    public AiException(String hebrewMessage) {
        super(hebrewMessage);
    }
}
