package org.example.learningprojectserver.service.studysummary;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.StudySummaryDTOs.GenerateRequest;
import org.example.learningprojectserver.dto.StudySummaryDTOs.Mistake;
import org.example.learningprojectserver.dto.StudySummaryDTOs.Options;
import org.example.learningprojectserver.dto.StudySummaryDTOs.Podcast;
import org.example.learningprojectserver.dto.StudySummaryDTOs.PodcastSummary;
import org.example.learningprojectserver.dto.StudySummaryDTOs.Sheet;
import org.example.learningprojectserver.dto.StudySummaryDTOs.SheetContent;
import org.example.learningprojectserver.dto.StudySummaryDTOs.SheetSummary;
import org.example.learningprojectserver.dto.StudySummaryDTOs.TestOption;
import org.example.learningprojectserver.entities.ReviewItemEntity;
import org.example.learningprojectserver.entities.StudyPodcastAudioEntity;
import org.example.learningprojectserver.entities.StudyPodcastEntity;
import org.example.learningprojectserver.entities.StudySheetEntity;
import org.example.learningprojectserver.mappers.StudyPodcastEntityToDTOMapper;
import org.example.learningprojectserver.mappers.StudySheetEntityToDTOMapper;
import org.example.learningprojectserver.repository.StudyPodcastAudioRepository;
import org.example.learningprojectserver.repository.StudyPodcastRepository;
import org.example.learningprojectserver.repository.StudySheetRepository;
import org.example.learningprojectserver.repository.StudySummarySourceRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.ai.AiException;
import org.example.learningprojectserver.service.ai.AiGateway;
import org.example.learningprojectserver.service.ai.AiUser;
import org.example.learningprojectserver.service.ai.AiUserResolver;
import org.example.learningprojectserver.service.ai.provider.AiMessage;
import org.example.learningprojectserver.service.ai.provider.AiRequest;
import org.example.learningprojectserver.service.ai.safety.ContentSafetyFilter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Personal study material before a test, built from the test's topics and the student's own
 * mistakes (the mistakes notebook, {@code review_item}):
 * <ul>
 *   <li>a summary sheet: topics, formulas and each mistake with a short explanation (printable);</li>
 *   <li>a study podcast: a ~5 minute spoken script, voiced by the AI (MP3, stored) or by the browser.</li>
 * </ul>
 * Both are stored so the student can reopen them. Without the AI, a basic version (no explanations,
 * a template script) can be built on request.
 */
@Service
@RequiredArgsConstructor
public class StudySummaryService {

    public static final String FEATURE_SHEET = "STUDY_SHEET";
    public static final String FEATURE_PODCAST = "STUDY_PODCAST";
    public static final String FEATURE_SPEECH = "STUDY_PODCAST_TTS";

    static final int MAX_TOPICS = 8;
    static final int MAX_TOPIC_CHARS = 80;
    static final int MAX_MISTAKES = 8;
    /** OpenAI's speech endpoint takes up to 4,096 characters. */
    static final int MAX_SPEECH_CHARS = 4096;
    /** Kept to what the speech endpoint takes, so the AI voice and the browser voice read the same text. */
    static final int MAX_SCRIPT_CHARS = MAX_SPEECH_CHARS;
    static final String AUDIO_TYPE = "audio/mpeg";
    private static final DateTimeFormatter START_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static final String NOTHING_TO_SUMMARIZE = "בחרו מבחן או לפחות נושא אחד לסיכום";
    static final String AUDIO_IN_PROGRESS = "ההקראה כבר בהפקה, נסו שוב בעוד רגע";
    static final String TEST_NOT_FOUND = "המבחן לא נמצא או שאינו משויך אליך";
    static final String SHEET_NOT_FOUND = "דף הסיכום לא נמצא";
    static final String PODCAST_NOT_FOUND = "הסיכום הקולי לא נמצא";
    static final String BAD_AI_OUTPUT = "ה-AI לא החזיר סיכום תקין. נסו שוב בעוד רגע.";

    private final StudySummarySourceRepository sources;
    private final StudySheetRepository sheetRepository;
    private final StudyPodcastRepository podcastRepository;
    private final StudyPodcastAudioRepository audioRepository;
    private final AiGateway gateway;
    private final AiUserResolver userResolver;
    private final ContentSafetyFilter safety;
    private final StudySheetEntityToDTOMapper sheetMapper;
    private final StudyPodcastEntityToDTOMapper podcastMapper;
    /** Podcasts whose AI recording is being produced right now (one instance). */
    private final Set<Long> audioInProgress = ConcurrentHashMap.newKeySet();

    /** What the summary is about: a title, the topics and the student's matching mistakes. */
    record Context(String title, Long testId, List<String> topics, List<ReviewItemEntity> mistakes) {
    }

    /** A request that cannot be summarized; the message is Hebrew. */
    static final class SummaryException extends RuntimeException {
        SummaryException(String hebrewMessage) {
            super(hebrewMessage);
        }
    }

    /** The audio of a podcast, or the Hebrew reason there is none. */
    public record AudioResult(byte[] audio, String contentType, String error) {
        static AudioResult failed(String error) {
            return new AudioResult(null, null, error);
        }
    }

    // ---------------------------------------------------------------- options

    public BasicResponse<Options> options(String userId) {
        Set<String> topics = new TreeSet<>(sources.findMistakeTopics(userId));
        topics.addAll(sources.findMistakeSubTopics(userId));
        topics.removeIf(t -> t == null || t.isBlank());
        BasicResponse<Options> response = new BasicResponse<>(true, null);
        response.setData(new Options(upcomingTests(userId), List.copyOf(topics),
                sources.countMistakes(userId), gateway.isAvailable()));
        return response;
    }

    /** Assigned tests that start today or later, soonest first. */
    List<TestOption> upcomingTests(String userId) {
        LocalDateTime from = AiGateway.now().toLocalDate().atStartOfDay();
        return sources.findAssignedTests(userId).stream()
                .filter(t -> {
                    LocalDateTime start = parseStart(t.startTime());
                    return start != null && !start.isBefore(from);
                })
                .sorted(Comparator.comparing(t -> parseStart(t.startTime())))
                .limit(20)
                .toList();
    }

    private static LocalDateTime parseStart(String startTime) {
        if (startTime == null || startTime.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(startTime.trim(), START_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- context

    Context buildContext(String userId, GenerateRequest request) {
        Set<String> topics = new LinkedHashSet<>();
        String title = null;
        Long testId = request == null ? null : request.testId();
        if (testId != null) {
            TestOption test = sources.findAssignedTests(userId).stream()
                    .filter(t -> testId.equals(t.testId()))
                    .findFirst()
                    .orElseThrow(() -> new SummaryException(TEST_NOT_FOUND));
            title = "לקראת " + firstNonBlank(test.title(), test.topic(), "המבחן");
            addTopic(topics, test.topic());
            sources.findTestSubTopics(testId).forEach(t -> addTopic(topics, t));
        }
        if (request != null && request.topics() != null) {
            for (String raw : request.topics()) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String trimmed = raw.trim();
                if (trimmed.length() > MAX_TOPIC_CHARS) {
                    trimmed = trimmed.substring(0, MAX_TOPIC_CHARS);
                }
                ContentSafetyFilter.Verdict verdict = safety.checkInput(trimmed);
                if (!verdict.allowed()) {
                    throw new SummaryException("אחד הנושאים לא מתאים לסיכום. בחרו נושא מהרשימה.");
                }
                addTopic(topics, verdict.sanitized().replaceAll("[<>]", ""));
            }
        }

        List<ReviewItemEntity> allMistakes = sources.findMistakes(userId, PageRequest.of(0, 60));
        if (topics.isEmpty()) {
            // Nothing picked: summarize the topics the student struggles with most.
            allMistakes.forEach(m -> addTopic(topics, firstNonBlank(m.getTopic(), m.getSubTopic(), null)));
        }
        if (topics.isEmpty()) {
            throw new SummaryException(NOTHING_TO_SUMMARIZE);
        }
        List<String> topicList = topics.stream().limit(MAX_TOPICS).toList();
        if (title == null) {
            title = "סיכום: " + String.join(", ", topicList.stream().limit(3).toList());
        }
        if (title.length() > 200) {
            title = title.substring(0, 200);
        }
        return new Context(title, testId, topicList, pickMistakes(allMistakes, topicList));
    }

    /** Mistakes in the chosen topics; when there are none, the student's most missed questions overall. */
    static List<ReviewItemEntity> pickMistakes(List<ReviewItemEntity> all, List<String> topics) {
        Set<String> wanted = new LinkedHashSet<>();
        topics.forEach(t -> wanted.add(t.toLowerCase(Locale.ROOT)));
        List<ReviewItemEntity> matching = all.stream()
                .filter(m -> matches(m.getTopic(), wanted) || matches(m.getSubTopic(), wanted))
                .limit(MAX_MISTAKES)
                .toList();
        return matching.isEmpty() ? all.stream().limit(MAX_MISTAKES).toList() : matching;
    }

    private static boolean matches(String value, Set<String> wanted) {
        return value != null && wanted.contains(value.trim().toLowerCase(Locale.ROOT));
    }

    private static void addTopic(Set<String> topics, String topic) {
        if (topic != null && !topic.isBlank()) {
            topics.add(topic.trim());
        }
    }

    // ---------------------------------------------------------------- sheets

    public BasicResponse<Sheet> generateSheet(String userId, GenerateRequest request) {
        Context ctx;
        try {
            ctx = buildContext(userId, request);
        } catch (SummaryException e) {
            return new BasicResponse<>(false, e.getMessage());
        }
        SheetContent content;
        boolean ai = request == null || !request.isBasic();
        if (!ai) {
            content = StudySummaryPrompts.basicSheet(ctx.topics(), mistakesOf(ctx));
        } else {
            String json;
            try {
                AiUser user = userResolver.resolve(userId);
                json = gateway.complete(user, FEATURE_SHEET, AiRequest.json(List.of(
                        AiMessage.system(StudySummaryPrompts.SHEET_SYSTEM),
                        AiMessage.user(StudySummaryPrompts.sheetUserMessage(ctx.title(), ctx.topics(), mistakesOf(ctx)))),
                        3000), false);
            } catch (AiException e) {
                return new BasicResponse<>(false, e.getMessage());
            }
            if (!safety.checkOutput(json).allowed()) {
                return new BasicResponse<>(false, BAD_AI_OUTPUT);
            }
            content = StudySummaryPrompts.parseSheet(json, mistakesOf(ctx));
            if (content == null) {
                return new BasicResponse<>(false, BAD_AI_OUTPUT);
            }
        }

        StudySheetEntity sheet = new StudySheetEntity();
        sheet.setStudentUserId(userId);
        sheet.setTitle(ctx.title());
        sheet.setTestId(ctx.testId());
        sheet.setTopics(joinTopics(ctx.topics()));
        sheet.setContent(StudySheetEntityToDTOMapper.writeContent(content));
        sheet.setAiGenerated(ai);
        sheet.setCreatedAt(AiGateway.now());
        sheetRepository.save(sheet);

        BasicResponse<Sheet> response = new BasicResponse<>(true, ai ? "דף הסיכום מוכן" : "נוצר דף בסיסי ללא הסברי AI");
        response.setData(sheetMapper.apply(sheet));
        return response;
    }

    public BasicResponse<List<SheetSummary>> listSheets(String userId) {
        BasicResponse<List<SheetSummary>> response = new BasicResponse<>(true, null);
        response.setData(sheetRepository.findTop30ByStudentUserIdOrderByCreatedAtDesc(userId).stream()
                .map(sheetMapper::toSummary).toList());
        return response;
    }

    public BasicResponse<Sheet> getSheet(String userId, Long sheetId) {
        return sheetRepository.findByIdAndStudentUserId(sheetId, userId)
                .map(s -> {
                    BasicResponse<Sheet> r = new BasicResponse<Sheet>(true, null);
                    r.setData(sheetMapper.apply(s));
                    return r;
                })
                .orElseGet(() -> new BasicResponse<>(false, SHEET_NOT_FOUND));
    }

    public BasicResponse<Void> deleteSheet(String userId, Long sheetId) {
        Optional<StudySheetEntity> sheet = sheetRepository.findByIdAndStudentUserId(sheetId, userId);
        if (sheet.isEmpty()) {
            return new BasicResponse<>(false, SHEET_NOT_FOUND);
        }
        sheetRepository.delete(sheet.get());
        return new BasicResponse<>(true, "דף הסיכום נמחק");
    }

    // ---------------------------------------------------------------- podcasts

    public BasicResponse<Podcast> generatePodcast(String userId, GenerateRequest request) {
        Context ctx;
        try {
            ctx = buildContext(userId, request);
        } catch (SummaryException e) {
            return new BasicResponse<>(false, e.getMessage());
        }
        String script;
        boolean ai = request == null || !request.isBasic();
        if (!ai) {
            script = StudySummaryPrompts.basicScript(ctx.title(), ctx.topics(), mistakesOf(ctx));
        } else {
            try {
                AiUser user = userResolver.resolve(userId);
                AiRequest aiRequest = new AiRequest(List.of(
                        AiMessage.system(StudySummaryPrompts.PODCAST_SYSTEM),
                        AiMessage.user(StudySummaryPrompts.podcastUserMessage(ctx.title(), ctx.topics(), mistakesOf(ctx)))),
                        null, 0.6, false, 3500);
                script = StudySummaryPrompts.cleanScript(gateway.complete(user, FEATURE_PODCAST, aiRequest, false));
            } catch (AiException e) {
                return new BasicResponse<>(false, e.getMessage());
            }
            if (script.isBlank() || !safety.checkOutput(script).allowed()) {
                return new BasicResponse<>(false, BAD_AI_OUTPUT);
            }
        }
        script = speechText(script);

        StudyPodcastEntity podcast = new StudyPodcastEntity();
        podcast.setStudentUserId(userId);
        podcast.setTitle(ctx.title());
        podcast.setTestId(ctx.testId());
        podcast.setTopics(joinTopics(ctx.topics()));
        podcast.setScript(script);
        podcast.setAiGenerated(ai);
        podcast.setHasAudio(false);
        podcast.setCreatedAt(AiGateway.now());
        podcastRepository.save(podcast);

        BasicResponse<Podcast> response = new BasicResponse<>(true, ai ? "הסיכום הקולי מוכן" : "נוצר סיכום קולי בסיסי");
        response.setData(podcastMapper.apply(podcast));
        return response;
    }

    public BasicResponse<List<PodcastSummary>> listPodcasts(String userId) {
        BasicResponse<List<PodcastSummary>> response = new BasicResponse<>(true, null);
        response.setData(podcastRepository.findTop30ByStudentUserIdOrderByCreatedAtDesc(userId).stream()
                .map(podcastMapper::toSummary).toList());
        return response;
    }

    public BasicResponse<Podcast> getPodcast(String userId, Long podcastId) {
        return podcastRepository.findByIdAndStudentUserId(podcastId, userId)
                .map(p -> {
                    BasicResponse<Podcast> r = new BasicResponse<Podcast>(true, null);
                    r.setData(podcastMapper.apply(p));
                    return r;
                })
                .orElseGet(() -> new BasicResponse<>(false, PODCAST_NOT_FOUND));
    }

    @Transactional
    public BasicResponse<Void> deletePodcast(String userId, Long podcastId) {
        Optional<StudyPodcastEntity> podcast = podcastRepository.findByIdAndStudentUserId(podcastId, userId);
        if (podcast.isEmpty()) {
            return new BasicResponse<>(false, PODCAST_NOT_FOUND);
        }
        if (audioRepository.existsById(podcastId)) {
            audioRepository.deleteById(podcastId);
        }
        podcastRepository.delete(podcast.get());
        return new BasicResponse<>(true, "הסיכום הקולי נמחק");
    }

    /**
     * Produces the AI voice recording of a podcast once and stores it. Not transactional: the
     * provider call is slow, and a failure leaves the podcast as it was (browser voice still works).
     */
    public BasicResponse<Podcast> createAudio(String userId, Long podcastId) {
        Optional<StudyPodcastEntity> found = podcastRepository.findByIdAndStudentUserId(podcastId, userId);
        if (found.isEmpty()) {
            return new BasicResponse<>(false, PODCAST_NOT_FOUND);
        }
        StudyPodcastEntity podcast = found.get();
        if (!podcast.isHasAudio() || !audioRepository.existsById(podcastId)) {
            // One paid speech call per podcast at a time (double click, two tabs).
            if (!audioInProgress.add(podcastId)) {
                return new BasicResponse<>(false, AUDIO_IN_PROGRESS);
            }
            byte[] audio;
            try {
                audio = gateway.speak(userResolver.resolve(userId), FEATURE_SPEECH, speechText(podcast.getScript()));
            } catch (AiException e) {
                return new BasicResponse<>(false, e.getMessage());
            } finally {
                audioInProgress.remove(podcastId);
            }
            if (audio == null || audio.length == 0) {
                return new BasicResponse<>(false, AiGateway.SPEECH_FAILED);
            }
            StudyPodcastAudioEntity row = new StudyPodcastAudioEntity();
            row.setPodcastId(podcastId);
            row.setAudio(audio);
            row.setContentType(AUDIO_TYPE);
            row.setCreatedAt(AiGateway.now());
            audioRepository.save(row);
            podcast.setHasAudio(true);
            podcastRepository.save(podcast);
        }
        BasicResponse<Podcast> response = new BasicResponse<>(true, "ההקראה מוכנה");
        response.setData(podcastMapper.apply(podcast));
        return response;
    }

    public AudioResult audio(String userId, Long podcastId) {
        if (podcastRepository.findByIdAndStudentUserId(podcastId, userId).isEmpty()) {
            return AudioResult.failed(PODCAST_NOT_FOUND);
        }
        return audioRepository.findById(podcastId)
                .map(a -> new AudioResult(a.getAudio(), a.getContentType(), null))
                .orElseGet(() -> AudioResult.failed("עדיין לא הופקה הקראה לסיכום הזה"));
    }

    /** The part of the script the speech endpoint accepts, cut at the end of a sentence. */
    static String speechText(String script) {
        if (script.length() <= MAX_SPEECH_CHARS) {
            return script;
        }
        String head = script.substring(0, MAX_SPEECH_CHARS);
        int end = Math.max(head.lastIndexOf('.'), Math.max(head.lastIndexOf('!'), head.lastIndexOf('?')));
        return end > MAX_SPEECH_CHARS / 2 ? head.substring(0, end + 1) : head;
    }

    // ---------------------------------------------------------------- helpers

    private static List<Mistake> mistakesOf(Context ctx) {
        List<Mistake> mistakes = new ArrayList<>();
        for (ReviewItemEntity m : ctx.mistakes()) {
            mistakes.add(new Mistake(firstNonBlank(m.getSubTopic(), m.getTopic(), ""),
                    nullToEmpty(m.getQuestionText()), nullToEmpty(m.getAnswer()), ""));
        }
        return mistakes;
    }

    private static String joinTopics(List<String> topics) {
        String joined = String.join(", ", topics);
        return joined.length() > 1000 ? joined.substring(0, 1000) : joined;
    }

    private static String firstNonBlank(String a, String b, String fallback) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return fallback;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
