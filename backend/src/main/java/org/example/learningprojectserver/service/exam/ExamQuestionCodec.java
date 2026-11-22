package org.example.learningprojectserver.service.exam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.learningprojectserver.dto.exam.ExamQuestionInput;
import org.example.learningprojectserver.dto.exam.ExamQuestionView;
import org.example.learningprojectserver.entities.TestQuestionEntity;
import org.example.learningprojectserver.enums.QuestionKind;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Converts authored questions to stored rows and stored rows to the student's view.
 *
 * Stored shapes (TestQuestionEntity):
 * MULTI_SELECT  optionsJson ["a","b",...]                 answerKeyJson [0,2]
 * NUMERIC       -                                         answerKeyJson 3.5, tolerance column
 * MATCHING      optionsJson {"left":[...],"right":[...]}  answerKeyJson [right index per left]
 * ORDERING      optionsJson [items in the correct order]  -
 * SHORT_OPEN    -                                         answerKeyJson ["accepted", ...]
 * TEXT          legacy generated question, key in the answer column.
 *
 * The answer key never goes into an {@link ExamQuestionView}.
 */
@Component
public class ExamQuestionCodec {

    static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_TEXT = 4000;
    private static final int LEGACY_TEXT_LIMIT = 250;

    /** Hebrew error for an invalid question, or null when it is valid. */
    public String validate(ExamQuestionInput in, int number) {
        String prefix = "שאלה " + number + ": ";
        QuestionKind kind = QuestionKind.of(in.getKind());
        if (kind == null || kind == QuestionKind.TEXT) {
            return prefix + "סוג שאלה לא נתמך";
        }
        if (isBlank(in.getText())) {
            return prefix + "חסר נוסח שאלה";
        }
        if (in.getText().length() > MAX_TEXT) {
            return prefix + "נוסח השאלה ארוך מדי";
        }
        if (in.getPoints() != null && (in.getPoints() < 1 || in.getPoints() > 100)) {
            return prefix + "ניקוד חייב להיות בין 1 ל-100";
        }
        switch (kind) {
            case MULTI_SELECT -> {
                List<String> options = clean(in.getOptions());
                if (hasBlank(in.getOptions())) {
                    return prefix + "אין להשאיר אפשרות ריקה";
                }
                if (options.size() < 2) {
                    return prefix + "יש להזין לפחות שתי אפשרויות";
                }
                if (in.getCorrectOptions() == null || in.getCorrectOptions().isEmpty()) {
                    return prefix + "יש לסמן לפחות תשובה נכונה אחת";
                }
                for (Integer i : in.getCorrectOptions()) {
                    if (i == null || i < 0 || i >= options.size()) {
                        return prefix + "תשובה נכונה לא תקינה";
                    }
                }
            }
            case NUMERIC -> {
                if (in.getNumericAnswer() == null || !Double.isFinite(in.getNumericAnswer())) {
                    return prefix + "יש להזין תשובה מספרית";
                }
                if (in.getTolerance() != null && (in.getTolerance() < 0 || !Double.isFinite(in.getTolerance()))) {
                    return prefix + "סטייה מותרת חייבת להיות אפס או יותר";
                }
            }
            case MATCHING -> {
                List<String> left = clean(in.getMatchLeft());
                List<String> right = clean(in.getMatchRight());
                if (hasBlank(in.getMatchLeft()) || hasBlank(in.getMatchRight())) {
                    return prefix + "אין להשאיר פריט התאמה ריק";
                }
                if (left.size() < 2 || right.size() < 2) {
                    return prefix + "יש להזין לפחות שני זוגות להתאמה";
                }
                if (in.getMatchKey() == null || in.getMatchKey().size() != left.size()) {
                    return prefix + "יש לבחור התאמה לכל פריט";
                }
                for (Integer i : in.getMatchKey()) {
                    if (i == null || i < 0 || i >= right.size()) {
                        return prefix + "התאמה לא תקינה";
                    }
                }
            }
            case ORDERING -> {
                if (clean(in.getOptions()).size() < 2) {
                    return prefix + "יש להזין לפחות שני פריטים לסידור";
                }
            }
            case SHORT_OPEN -> {
                if (clean(in.getAcceptedAnswers()).isEmpty()) {
                    return prefix + "יש להזין לפחות תשובה מקובלת אחת";
                }
            }
            default -> {
                return prefix + "סוג שאלה לא נתמך";
            }
        }
        return null;
    }

    /** Builds an unsaved question row from a validated input. */
    public TestQuestionEntity toEntity(ExamQuestionInput in, String subject, String topic) {
        QuestionKind kind = QuestionKind.of(in.getKind());
        TestQuestionEntity q = new TestQuestionEntity();
        q.setKind(kind.name());
        q.setSubject(subject);
        q.setTopic(topic);
        q.setSubTopic(kind.name());
        String text = in.getText().trim();
        q.setPrompt(text);
        q.setQuestionText(text.length() > LEGACY_TEXT_LIMIT ? text.substring(0, LEGACY_TEXT_LIMIT) : text);
        q.setPoints(in.getPoints() == null ? 1 : in.getPoints());
        switch (kind) {
            case MULTI_SELECT -> {
                q.setOptionsJson(write(clean(in.getOptions())));
                q.setAnswerKeyJson(write(new TreeSet<>(in.getCorrectOptions())));
            }
            case NUMERIC -> {
                q.setAnswerKeyJson(write(in.getNumericAnswer()));
                q.setTolerance(in.getTolerance() == null ? 0.0 : in.getTolerance());
            }
            case MATCHING -> {
                Map<String, List<String>> options = new LinkedHashMap<>();
                options.put("left", clean(in.getMatchLeft()));
                options.put("right", clean(in.getMatchRight()));
                q.setOptionsJson(write(options));
                q.setAnswerKeyJson(write(in.getMatchKey()));
            }
            case ORDERING -> q.setOptionsJson(write(clean(in.getOptions())));
            case SHORT_OPEN -> q.setAnswerKeyJson(write(clean(in.getAcceptedAnswers())));
            default -> { }
        }
        return q;
    }

    /** A detached copy of a stored question (for cloning and versions). */
    public TestQuestionEntity copy(TestQuestionEntity src) {
        TestQuestionEntity q = new TestQuestionEntity();
        q.setSubject(src.getSubject());
        q.setTopic(src.getTopic());
        q.setSubTopic(src.getSubTopic());
        q.setQuestionText(src.getQuestionText());
        q.setAnswer(src.getAnswer());
        q.setKind(src.getKind());
        q.setPrompt(src.getPrompt());
        q.setOptionsJson(src.getOptionsJson());
        q.setAnswerKeyJson(src.getAnswerKeyJson());
        q.setTolerance(src.getTolerance());
        q.setPoints(src.getPoints());
        q.setPosition(src.getPosition());
        return q;
    }

    /**
     * The student's view of a question. Option order depends only on the attempt seed and the
     * question id, so reloading the page shows the same order.
     */
    public ExamQuestionView toView(TestQuestionEntity q, long attemptSeed, boolean shuffleOptions) {
        QuestionKind kind = QuestionKind.of(q.getKind());
        if (kind == null) {
            kind = QuestionKind.TEXT;
        }
        Random random = new Random(attemptSeed ^ (q.getId() * 0x9E3779B97F4A7C15L));
        ExamQuestionView view = new ExamQuestionView();
        view.setId(q.getId());
        view.setKind(kind.name());
        view.setText(q.getDisplayText());
        view.setPoints(q.getPointsOrDefault());
        switch (kind) {
            case MULTI_SELECT -> view.setOptions(options(readStrings(q.getOptionsJson()), random, shuffleOptions));
            case MATCHING -> {
                JsonNode node = readTree(q.getOptionsJson());
                view.setMatchLeft(options(strings(node.path("left")), random, false));
                view.setMatchRight(options(strings(node.path("right")), random, shuffleOptions));
            }
            case ORDERING -> {
                List<ExamQuestionView.Option> items = options(readStrings(q.getOptionsJson()), random, true);
                if (items.size() > 1 && isIdentity(items)) {
                    Collections.rotate(items, 1);
                }
                view.setOptions(items);
            }
            default -> { }
        }
        return view;
    }

    private static List<ExamQuestionView.Option> options(List<String> texts, Random random, boolean shuffle) {
        List<ExamQuestionView.Option> list = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            list.add(new ExamQuestionView.Option(i, texts.get(i)));
        }
        if (shuffle) {
            Collections.shuffle(list, random);
        }
        return list;
    }

    private static boolean isIdentity(List<ExamQuestionView.Option> items) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() != i) {
                return false;
            }
        }
        return true;
    }

    static List<String> readStrings(String json) {
        return strings(readTree(json));
    }

    static List<String> strings(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> out.add(n.asText()));
        }
        return out;
    }

    static JsonNode readTree(String json) {
        if (json == null || json.isBlank()) {
            return JSON.missingNode();
        }
        try {
            return JSON.readTree(json);
        } catch (JsonProcessingException e) {
            return JSON.missingNode();
        }
    }

    /** Integer list from a JSON array; null when the text is not one. */
    static List<Integer> readInts(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSON.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return null;
        }
    }

    public static Map<Long, String> readAnswers(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return JSON.readValue(json, new TypeReference<Map<Long, String>>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    public static String write(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> clean(List<String> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        return list.stream().filter(s -> !isBlank(s)).map(String::trim).toList();
    }

    private static boolean hasBlank(List<String> list) {
        return list != null && list.stream().anyMatch(ExamQuestionCodec::isBlank);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
