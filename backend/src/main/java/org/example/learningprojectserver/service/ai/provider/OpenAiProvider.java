package org.example.learningprojectserver.service.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.learningprojectserver.service.ai.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * OpenAI (or any OpenAI-compatible endpoint, via {@code ai.openai.base-url}). The key is read from
 * the {@code GPT_TOKEN} environment variable on every call, so adding it later needs no code change.
 * Without it {@link #isConfigured()} is false and the gateway answers with a Hebrew error.
 */
@Component
public class OpenAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);
    private static final String KEY_ENV = "GPT_TOKEN";

    private final Environment env;
    private final AiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public OpenAiProvider(Environment env, AiProperties properties) {
        this.env = env;
        this.properties = properties;
    }

    @Override
    public String name() {
        return "openai";
    }

    @Override
    public boolean isConfigured() {
        String key = env.getProperty(KEY_ENV);
        return key != null && !key.isBlank();
    }

    private String authorization() {
        String key = env.getProperty(KEY_ENV, "").trim();
        // The legacy GptMassenger sends GPT_TOKEN as-is, so accept a value that already has the prefix.
        return key.startsWith("Bearer ") ? key : "Bearer " + key;
    }

    private String modelFor(AiRequest request) {
        if (request.model() != null) {
            return request.model();
        }
        return request.hasImage() ? properties.getVisionModel() : properties.getModel();
    }

    String buildChatBody(AiRequest request, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", modelFor(request));
        body.put("temperature", request.temperature());
        body.put("max_tokens", request.maxTokens());
        if (request.jsonOutput()) {
            body.putObject("response_format").put("type", "json_object");
        }
        if (stream) {
            body.put("stream", true);
            body.putObject("stream_options").put("include_usage", true);
        }
        ArrayNode messages = body.putArray("messages");
        for (AiMessage m : request.messages()) {
            ObjectNode node = messages.addObject();
            node.put("role", m.role());
            if (m.imageDataUrl() == null) {
                node.put("content", m.content());
            } else {
                ArrayNode parts = node.putArray("content");
                parts.addObject().put("type", "text").put("text", m.content());
                ObjectNode image = parts.addObject().put("type", "image_url");
                image.putObject("image_url").put("url", m.imageDataUrl()).put("detail", "low");
            }
        }
        return body.toString();
    }

    private HttpRequest.Builder post(String path) {
        return HttpRequest.newBuilder(URI.create(properties.getOpenAiBaseUrl() + path))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Authorization", authorization());
    }

    @Override
    public AiResult complete(AiRequest request) {
        HttpRequest httpRequest = post("/chat/completions")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildChatBody(request, false), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new AiProviderException("OpenAI returned " + response.statusCode() + ": " + abbreviate(response.body()));
            }
            JsonNode json = objectMapper.readTree(response.body());
            String text = json.path("choices").path(0).path("message").path("content").asText("");
            JsonNode usage = json.path("usage");
            return new AiResult(text, usage.path("prompt_tokens").asInt(0), usage.path("completion_tokens").asInt(0),
                    json.path("model").asText(modelFor(request)));
        } catch (IOException e) {
            throw new AiProviderException("OpenAI request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("OpenAI request interrupted", e);
        }
    }

    @Override
    public AiResult stream(AiRequest request, Consumer<String> onDelta) {
        HttpRequest httpRequest = post("/chat/completions")
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(buildChatBody(request, true), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                try (InputStream in = response.body()) {
                    throw new AiProviderException("OpenAI returned " + response.statusCode() + ": "
                            + abbreviate(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                }
            }
            StringBuilder full = new StringBuilder();
            int promptTokens = 0;
            int completionTokens = 0;
            String model = modelFor(request);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (data.equals("[DONE]")) {
                        break;
                    }
                    JsonNode chunk = objectMapper.readTree(data);
                    String delta = chunk.path("choices").path(0).path("delta").path("content").asText("");
                    if (!delta.isEmpty()) {
                        full.append(delta);
                        onDelta.accept(delta);
                    }
                    JsonNode usage = chunk.path("usage");
                    if (usage.isObject()) {
                        promptTokens = usage.path("prompt_tokens").asInt(promptTokens);
                        completionTokens = usage.path("completion_tokens").asInt(completionTokens);
                    }
                    if (chunk.hasNonNull("model")) {
                        model = chunk.get("model").asText();
                    }
                }
            }
            return new AiResult(full.toString(), promptTokens, completionTokens, model);
        } catch (IOException e) {
            throw new AiProviderException("OpenAI stream failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("OpenAI stream interrupted", e);
        }
    }

    @Override
    public String transcribe(byte[] audio, String fileName, String language) {
        String boundary = "----lp" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        try {
            writeField(body, boundary, "model", properties.getTranscriptionModel());
            writeField(body, boundary, "language", language);
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                    + sanitizeFileName(fileName) + "\"\r\nContent-Type: application/octet-stream\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            body.write(audio);
            body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpRequest httpRequest = post("/audio/transcriptions")
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new AiProviderException("OpenAI transcription returned " + response.statusCode() + ": " + abbreviate(response.body()));
            }
            return objectMapper.readTree(response.body()).path("text").asText("");
        } catch (IOException e) {
            throw new AiProviderException("OpenAI transcription failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("OpenAI transcription interrupted", e);
        }
    }

    // [s05] Text to speech for the study podcast.
    @Override
    public byte[] speak(String text, String voice, String language) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.getTtsModel());
        body.put("input", text);
        body.put("voice", voice);
        body.put("response_format", "mp3");
        if (properties.getTtsModel().startsWith("gpt-")) {
            // Only the gpt-*-tts models take instructions; tts-1 rejects the field.
            body.put("instructions", "Speak in fluent, warm Israeli Hebrew (" + language + "), like a friendly teacher hosting a short study podcast.");
        }
        HttpRequest httpRequest = post("/audio/speech")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new AiProviderException("OpenAI speech returned " + response.statusCode() + ": "
                        + abbreviate(new String(response.body(), StandardCharsets.UTF_8)));
            }
            return response.body();
        } catch (IOException e) {
            throw new AiProviderException("OpenAI speech failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("OpenAI speech interrupted", e);
        }
    }

    @Override
    public boolean isFlaggedByModeration(String text) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", "omni-moderation-latest");
        body.put("input", text);
        HttpRequest httpRequest = post("/moderations")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.warn("Moderation call returned {}", response.statusCode());
                return false;
            }
            return objectMapper.readTree(response.body()).path("results").path(0).path("flagged").asBoolean(false);
        } catch (IOException e) {
            log.warn("Moderation call failed: {}", e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void writeField(ByteArrayOutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n")
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "audio.webm";
        }
        return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }
}
