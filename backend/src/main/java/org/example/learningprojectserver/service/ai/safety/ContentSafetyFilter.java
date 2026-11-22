package org.example.learningprojectserver.service.ai.safety;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * First line of defence for an AI used by minors. Runs locally, before and after every model call:
 * <ul>
 *     <li>distress signals (self-harm) are never sent to the model; the student gets a caring answer
 *     with the ERAN hotline and the message is flagged for school staff;</li>
 *     <li>explicit or violent content is refused and flagged;</li>
 *     <li>prompt-injection attempts ("ignore your instructions", "show the system prompt") are refused;</li>
 *     <li>personal details (ID numbers, phone numbers, e-mails) are redacted before the text leaves the server;</li>
 *     <li>student text is wrapped in delimiters the system prompt tells the model to treat as data.</li>
 * </ul>
 * The provider's moderation endpoint, when available, is a second check (see {@code TutorService}).
 */
@Component
public class ContentSafetyFilter {

    public static final int MAX_INPUT_CHARS = 1200;

    public static final String DISTRESS_REPLY = """
            נשמע שעובר עליך משהו קשה, ואני רוצה שתדע/י שאת/ה לא לבד. \
            כדאי מאוד לדבר עכשיו עם מבוגר שסומכים עליו: הורה, מחנך/ת או יועצ/ת בית הספר. \
            אפשר גם להתקשר לער"ן בטלפון 1201 (24 שעות ביממה, בחינם) או לכתוב בצ'אט של סה"ר באתר sahar.org.il. \
            במצב חירום יש להתקשר ל-101.""";

    public static final String INAPPROPRIATE_REPLY = "אני עוזר רק בלימודים, ואני לא יכול לעזור עם זה. בוא/י נחזור לתרגיל 🙂";

    public static final String INJECTION_REPLY = "אני כאן כדי לעזור בתרגיל בלבד, ואני לא יכול לשנות את הכללים שלי. מה הצעד הבא שניסית בפתרון?";

    public static final String UNSAFE_OUTPUT_REPLY = "סליחה, לא הצלחתי לנסח תשובה מתאימה. נסה/י לשאול שוב במילים אחרות.";

    private static final List<String> DISTRESS_TERMS = List.of(
            "להתאבד", "התאבדות", "רוצה למות", "לפגוע בעצמי", "לחתוך את עצמי", "אין טעם לחיות", "לא רוצה לחיות",
            "suicide", "kill myself", "hurt myself", "want to die", "self harm", "self-harm");

    private static final List<String> INAPPROPRIATE_TERMS = List.of(
            "פורנו", "סקס", "עירום", "זונה", "שרמוטה", "מזדיין", "לזיין",
            "porn", "nude", "nudes", "sex", "fuck", "bitch",
            "איך לבנות פצצה", "איך להכין פצצה", "לקנות נשק", "how to make a bomb", "buy a gun", "drugs", "סמים");

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore (all |any |the )?(previous|prior|above|earlier) (instructions|rules|prompts?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(system|developer) (prompt|message|instructions)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you are now|act as (a|an) |pretend (to be|you are)|jailbreak|DAN mode", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(reveal|show|print|tell me) (me )?(the |your )?(answer key|correct answer|hidden|secret)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("התעלם מ?(כל )?ה?(הוראות|כללים|הנחיות)"),
            Pattern.compile("(שכח|תשכח) (את )?(כל )?ה?(הוראות|כללים|הנחיות)"),
            Pattern.compile("(הוראות|הנחיות) (ה)?מערכת"),
            Pattern.compile("(מעכשיו|מהיום) אתה"),
            Pattern.compile("(תגלה|תראה|תכתוב) (לי )?(את )?ה?(תשובה הנכונה|פתרון הסופי|מפתח התשובות)"),
            Pattern.compile("</?\\s*(student_input|system|assistant)\\s*>", Pattern.CASE_INSENSITIVE));

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern IL_PHONE = Pattern.compile("(?<!\\d)(\\+?972[- ]?|0)(5\\d|[2-489]|7\\d)[- ]?\\d{3}[- ]?\\d{4}(?!\\d)");
    private static final Pattern ID_NUMBER = Pattern.compile("(?<!\\d)\\d{9}(?!\\d)");
    private static final String REDACTED = "[פרט אישי הוסר]";

    public enum Category { NONE, DISTRESS, INAPPROPRIATE, INJECTION, TOO_LONG }

    /**
     * Result of a check. When {@code allowed} is false, {@code reply} is what the student sees instead
     * of a model answer. {@code sanitized} is the text that may be sent to the model.
     */
    public record Verdict(boolean allowed, Category category, String sanitized, String reply) {
        public boolean shouldFlag() {
            return category == Category.DISTRESS || category == Category.INAPPROPRIATE || category == Category.INJECTION;
        }
    }

    public Verdict checkInput(String text) {
        if (text == null || text.isBlank()) {
            return new Verdict(true, Category.NONE, "", null);
        }
        if (text.length() > MAX_INPUT_CHARS) {
            return new Verdict(false, Category.TOO_LONG, null, "ההודעה ארוכה מדי. נסה/י לקצר ל-" + MAX_INPUT_CHARS + " תווים.");
        }
        String normalized = normalize(text);
        if (containsAny(normalized, DISTRESS_TERMS)) {
            return new Verdict(false, Category.DISTRESS, null, DISTRESS_REPLY);
        }
        if (containsAny(normalized, INAPPROPRIATE_TERMS)) {
            return new Verdict(false, Category.INAPPROPRIATE, null, INAPPROPRIATE_REPLY);
        }
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return new Verdict(false, Category.INJECTION, null, INJECTION_REPLY);
            }
        }
        return new Verdict(true, Category.NONE, redactPersonalDetails(text), null);
    }

    /** Checks a model answer before it is stored or (for non-streamed calls) shown. */
    public Verdict checkOutput(String text) {
        if (text == null) {
            return new Verdict(true, Category.NONE, "", null);
        }
        if (containsAny(normalize(text), INAPPROPRIATE_TERMS)) {
            return new Verdict(false, Category.INAPPROPRIATE, null, UNSAFE_OUTPUT_REPLY);
        }
        return new Verdict(true, Category.NONE, text, null);
    }

    public String redactPersonalDetails(String text) {
        String result = EMAIL.matcher(text).replaceAll(REDACTED);
        result = IL_PHONE.matcher(result).replaceAll(REDACTED);
        return ID_NUMBER.matcher(result).replaceAll(REDACTED);
    }

    /**
     * Wraps untrusted text so the model can tell it apart from instructions. Any delimiter the text
     * itself contains is removed first, so it cannot close the block early.
     */
    public String wrapUntrusted(String text) {
        String cleaned = text == null ? "" : text.replaceAll("(?i)</?\\s*student_input\\s*>", "");
        return "<student_input>\n" + cleaned + "\n</student_input>";
    }

    /**
     * True when {@code answer} appears in {@code text} as a standalone token. Used to keep the tutor
     * from handing out the answer. One-character answers are skipped: they appear in every explanation.
     */
    public boolean revealsAnswer(String text, String answer) {
        if (text == null || answer == null) {
            return false;
        }
        String normalizedAnswer = compactOperators(answer.trim().toLowerCase(Locale.ROOT));
        if (normalizedAnswer.length() < 2) {
            return false;
        }
        String compact = compactOperators(text.toLowerCase(Locale.ROOT));
        Pattern token = Pattern.compile("(?<![\\p{L}\\p{N}.])" + Pattern.quote(normalizedAnswer) + "(?![\\p{L}\\p{N}]|\\.\\d)");
        return token.matcher(compact).find();
    }

    /** "x = 3 / 4" and "x=3/4" compare equal: spaces around operators are dropped. */
    private static String compactOperators(String text) {
        return text.replaceAll("\\s*([=,/+\\-*()^])\\s*", "$1");
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\u0591-\\u05C7]", "")   // Hebrew niqqud
                .replaceAll("[\\u200B-\\u200F]", "")   // zero-width and direction marks
                .replaceAll("\\s+", " ");
    }

    private static boolean containsAny(String normalized, List<String> terms) {
        for (String term : terms) {
            if (isAscii(term)) {
                if (Pattern.compile("\\b" + Pattern.quote(term) + "\\b").matcher(normalized).find()) {
                    return true;
                }
            } else if (normalized.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAscii(String s) {
        return s.chars().allMatch(c -> c < 128);
    }
}
