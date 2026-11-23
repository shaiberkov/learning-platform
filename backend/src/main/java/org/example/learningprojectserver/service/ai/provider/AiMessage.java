package org.example.learningprojectserver.service.ai.provider;

/**
 * One chat message. {@code imageDataUrl} is an optional {@code data:image/...;base64,...} URL
 * for vision requests; providers without vision support ignore it.
 */
public record AiMessage(String role, String content, String imageDataUrl) {

    public static AiMessage system(String content) {
        return new AiMessage("system", content, null);
    }

    public static AiMessage user(String content) {
        return new AiMessage("user", content, null);
    }

    public static AiMessage assistant(String content) {
        return new AiMessage("assistant", content, null);
    }

    public static AiMessage userWithImage(String content, String imageDataUrl) {
        return new AiMessage("user", content, imageDataUrl);
    }
}
