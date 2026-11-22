package org.example.learningprojectserver.service.ai.tutor;

import jakarta.annotation.PreDestroy;
import org.example.learningprojectserver.dto.ai.AiChatMessageDTO;
import org.example.learningprojectserver.dto.ai.AiStatusDTO;
import org.example.learningprojectserver.dto.ai.TutorRequestDTO;
import org.example.learningprojectserver.entities.AiConversationMessageEntity;
import org.example.learningprojectserver.entities.QuestionEntity;
import org.example.learningprojectserver.mappers.AiConversationMessageEntityToDTOMapper;
import org.example.learningprojectserver.repository.AiConversationMessageRepository;
import org.example.learningprojectserver.repository.QuestionRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.ai.AiException;
import org.example.learningprojectserver.service.ai.AiGateway;
import org.example.learningprojectserver.service.ai.AiProperties;
import org.example.learningprojectserver.service.ai.AiUser;
import org.example.learningprojectserver.service.ai.provider.AiMessage;
import org.example.learningprojectserver.service.ai.provider.AiRequest;
import org.example.learningprojectserver.service.ai.safety.ContentSafetyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Pattern;

/**
 * The context-aware Socratic tutor. One call = one student message → a streamed answer.
 * Order of checks: exam lock → input safety (local, then provider moderation) → quota → model,
 * with the answer-leak guard and output safety applied while/after streaming.
 */
@Service
public class TutorService {

    public static final String FEATURE = "TUTOR";
    public static final String FEATURE_PHOTO = "TUTOR_PHOTO";

    private static final Logger log = LoggerFactory.getLogger(TutorService.class);
    private static final Pattern IMAGE_DATA_URL = Pattern.compile("^data:image/(png|jpeg|jpg|webp);base64,[A-Za-z0-9+/=]+$");
    /** About 2 MB of image after base64; the client resizes photos well below this. */
    private static final int MAX_IMAGE_CHARS = 2_800_000;

    /** Receives the tutor's answer as it is produced. */
    public interface TutorSink {
        void delta(String text);

        /** Replace everything shown so far with this text (a refusal, a safety answer, a withheld answer). */
        void replace(String text);

        void done(int remainingToday);

        void error(String hebrewMessage);
    }

    private final AiGateway gateway;
    private final AiProperties properties;
    private final ContentSafetyFilter safety;
    private final ExamGuard examGuard;
    private final QuestionRepository questionRepository;
    private final AiConversationMessageRepository conversationRepository;
    private final AiConversationMessageEntityToDTOMapper messageMapper;
    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    public TutorService(AiGateway gateway, AiProperties properties, ContentSafetyFilter safety, ExamGuard examGuard,
                        QuestionRepository questionRepository, AiConversationMessageRepository conversationRepository,
                        AiConversationMessageEntityToDTOMapper messageMapper) {
        this.gateway = gateway;
        this.properties = properties;
        this.safety = safety;
        this.examGuard = examGuard;
        this.questionRepository = questionRepository;
        this.conversationRepository = conversationRepository;
        this.messageMapper = messageMapper;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    /** Runs {@link #reply} on the tutor pool and writes it to an SSE emitter. */
    public SseEmitter streamReply(AiUser user, TutorRequestDTO request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        TutorSink sink = new SseSink(emitter);
        try {
            executor.submit(() -> {
                try {
                    reply(user, request, sink);
                } finally {
                    emitter.complete();
                }
            });
        } catch (RejectedExecutionException e) {
            sink.error("השרת עמוס כרגע. נסו שוב בעוד רגע.");
            emitter.complete();
        }
        return emitter;
    }

    /** An already-finished stream that carries one {@code error} event. */
    public static SseEmitter errorEmitter(String hebrewMessage) {
        SseEmitter emitter = new SseEmitter(5_000L);
        new SseSink(emitter).error(hebrewMessage);
        emitter.complete();
        return emitter;
    }

    public void reply(AiUser user, TutorRequestDTO request, TutorSink sink) {
        try {
            String message = request.getMessage() == null ? "" : request.getMessage().trim();
            String image = request.getImageDataUrl();
            if (message.isEmpty() && image == null) {
                sink.error("כתבו הודעה או צרפו תמונה של התרגיל.");
                return;
            }
            if (image != null && (image.length() > MAX_IMAGE_CHARS || !IMAGE_DATA_URL.matcher(image).matches())) {
                sink.error("התמונה גדולה מדי או בפורמט לא נתמך (JPG, PNG או WEBP).");
                return;
            }

            if (user.isStudent() && examGuard.isInActiveExam(user.userId())) {
                sink.error(ExamGuard.EXAM_ACTIVE_MESSAGE);
                return;
            }
            if (examGuard.isExamQuestion(request.getQuestionId())) {
                sink.error(ExamGuard.EXAM_QUESTION_MESSAGE);
                return;
            }

            ContentSafetyFilter.Verdict verdict = safety.checkInput(message);
            if (verdict.allowed() && !message.isEmpty() && gateway.flaggedByProviderModeration(message)) {
                verdict = new ContentSafetyFilter.Verdict(false, ContentSafetyFilter.Category.INAPPROPRIATE, null,
                        ContentSafetyFilter.INAPPROPRIATE_REPLY);
            }
            if (!verdict.allowed()) {
                if (verdict.shouldFlag()) {
                    save(user, request.getQuestionId(), "user", message, verdict.category().name());
                }
                sink.replace(verdict.reply());
                sink.done(gateway.remainingToday(user));
                return;
            }

            gateway.ensureCanServe(user);

            QuestionEntity question = request.getQuestionId() == null ? null
                    : questionRepository.findQuestionById(request.getQuestionId());
            String studentAnswer = request.getStudentAnswer() == null ? null : request.getStudentAnswer().trim();
            String secretAnswer = question == null ? null : question.getAnswer();
            boolean alreadySolved = secretAnswer != null && studentAnswer != null
                    && secretAnswer.trim().equalsIgnoreCase(studentAnswer);

            List<AiMessage> messages = new ArrayList<>();
            messages.add(AiMessage.system(question == null
                    ? TutorPrompts.freeChat()
                    : TutorPrompts.forQuestion(question.getSubject(), question.getTopic(), question.getSubTopic(),
                    question.getQuestionText(), secretAnswer,
                    studentAnswer == null ? null : safety.redactPersonalDetails(studentAnswer), alreadySolved)));
            messages.addAll(history(user.userId(), request.getQuestionId()));
            String userText = message.isEmpty() ? "צירפתי תמונה של תרגיל. תעזור לי להתחיל?" : verdict.sanitized();
            String wrapped = safety.wrapUntrusted(userText);
            messages.add(image == null ? AiMessage.user(wrapped) : AiMessage.userWithImage(wrapped, image));

            boolean guardAnswer = secretAnswer != null && !alreadySolved;
            StringBuilder shown = new StringBuilder();
            boolean[] withheld = {false};
            String full = gateway.stream(user, image == null ? FEATURE : FEATURE_PHOTO, AiRequest.chat(messages), delta -> {
                if (withheld[0]) {
                    return;
                }
                if (guardAnswer && safety.revealsAnswer(shown + delta, secretAnswer)) {
                    withheld[0] = true;
                    return;
                }
                shown.append(delta);
                sink.delta(delta);
            });

            String finalText = full;
            if (withheld[0] || (guardAnswer && safety.revealsAnswer(full, secretAnswer))) {
                finalText = TutorPrompts.ANSWER_WITHHELD;
                sink.replace(finalText);
            } else {
                ContentSafetyFilter.Verdict out = safety.checkOutput(full);
                if (!out.allowed()) {
                    finalText = out.reply();
                    sink.replace(finalText);
                    save(user, request.getQuestionId(), "assistant", full, "OUTPUT_" + out.category().name());
                }
            }

            save(user, request.getQuestionId(), "user", image == null ? userText : "📷 " + userText, null);
            save(user, request.getQuestionId(), "assistant", finalText, null);
            sink.done(gateway.remainingToday(user));
        } catch (AiException e) {
            sink.error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Tutor failed for {}", user.userId(), e);
            sink.error(AiGateway.PROVIDER_FAILED);
        }
    }

    private List<AiMessage> history(String userId, Long questionId) {
        List<AiConversationMessageEntity> recent = conversationRepository.findRecent(userId, questionId,
                PageRequest.of(0, properties.getTutorHistoryMessages()));
        List<AiMessage> result = new ArrayList<>();
        for (AiConversationMessageEntity m : recent) {
            result.add("assistant".equals(m.getRole())
                    ? AiMessage.assistant(m.getContent())
                    : AiMessage.user(safety.wrapUntrusted(m.getContent())));
        }
        Collections.reverse(result);
        return result;
    }

    private void save(AiUser user, Long questionId, String role, String content, String flagReason) {
        AiConversationMessageEntity entity = new AiConversationMessageEntity();
        entity.setUserId(user.userId());
        entity.setSchoolCode(user.schoolCode());
        entity.setQuestionId(questionId);
        entity.setRole(role);
        entity.setContent(content);
        entity.setFlagged(flagReason != null);
        entity.setFlagReason(flagReason);
        entity.setCreatedAt(AiGateway.now());
        conversationRepository.save(entity);
    }

    public BasicResponse<List<AiChatMessageDTO>> getHistory(AiUser user, Long questionId) {
        List<AiChatMessageDTO> messages = new ArrayList<>(conversationRepository
                .findRecent(user.userId(), questionId, PageRequest.of(0, 50))
                .stream().map(messageMapper).toList());
        Collections.reverse(messages);
        BasicResponse<List<AiChatMessageDTO>> response = new BasicResponse<>(true, null);
        response.setData(messages);
        return response;
    }

    /** The student deletes their own conversations. Flagged messages stay until retention removes them. */
    public BasicResponse<Integer> clearHistory(AiUser user) {
        int deleted = conversationRepository.deleteUnflaggedByUser(user.userId());
        BasicResponse<Integer> response = new BasicResponse<>(true, "השיחות נמחקו");
        response.setData(deleted);
        return response;
    }

    public BasicResponse<String> transcribe(AiUser user, byte[] audio, String fileName) {
        if (audio == null || audio.length == 0) {
            return new BasicResponse<>(false, "לא התקבלה הקלטה");
        }
        if (user.isStudent() && examGuard.isInActiveExam(user.userId())) {
            return new BasicResponse<>(false, ExamGuard.EXAM_ACTIVE_MESSAGE);
        }
        try {
            BasicResponse<String> response = new BasicResponse<>(true, null);
            response.setData(gateway.transcribe(user, audio, fileName));
            return response;
        } catch (AiException e) {
            return new BasicResponse<>(false, e.getMessage());
        }
    }

    public BasicResponse<AiStatusDTO> status(AiUser user) {
        boolean examLocked = user.isStudent() && examGuard.isInActiveExam(user.userId());
        BasicResponse<AiStatusDTO> response = new BasicResponse<>(true, null);
        response.setData(new AiStatusDTO(gateway.isAvailable(), gateway.dailyLimit(user), gateway.usedToday(user),
                properties.getRetentionDays(), examLocked));
        return response;
    }

    /** Writes sink events as SSE: {@code delta}, {@code replace}, {@code done}, {@code error}, each with a JSON body. */
    private static final class SseSink implements TutorSink {
        private final SseEmitter emitter;

        SseSink(SseEmitter emitter) {
            this.emitter = emitter;
        }

        private void send(String event, Map<String, Object> data) {
            try {
                emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                // The student closed the chat; the answer is still saved.
            }
        }

        @Override
        public void delta(String text) {
            send("delta", Map.of("text", text));
        }

        @Override
        public void replace(String text) {
            send("replace", Map.of("text", text));
        }

        @Override
        public void done(int remainingToday) {
            send("done", Map.of("remaining", remainingToday));
        }

        @Override
        public void error(String hebrewMessage) {
            send("error", Map.of("message", hebrewMessage));
        }
    }
}
