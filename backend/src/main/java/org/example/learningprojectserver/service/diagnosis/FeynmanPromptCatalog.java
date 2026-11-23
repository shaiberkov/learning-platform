package org.example.learningprojectserver.service.diagnosis;

import org.example.learningprojectserver.service.adaptive.SkillCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * [s06] The "explain it to me" prompts, one or more per math topic. Each prompt lists the key ideas a
 * real explanation mentions; they guide the AI and, when the AI is not available, drive a simple
 * key-concept check (a rough score and the ideas that are missing).
 */
public final class FeynmanPromptCatalog {

    /** One idea a good explanation contains: the words that show it, and the gap shown when it is missing. */
    public record Concept(List<String> keywords, String gap) {}

    public record Prompt(String key, String subject, String topic, String text, List<Concept> concepts) {}

    public record Evaluation(int score, List<String> gaps, String feedback) {}

    static final int MIN_WORDS = 15;

    private static final List<Prompt> PROMPTS = List.of(
            new Prompt("equation-divide-both-sides", SkillCatalog.MATH, "משוואות",
                    "הסבר במילים שלך למה מחלקים בשני הצדדים של המשוואה",
                    List.of(
                            new Concept(List.of("שוויון", "שווה", "שווים", "מאזניים", "איזון", "מאוזן"),
                                    "לא הוסבר שמשוואה היא שוויון (כמו מאזניים) שצריך לשמור עליו"),
                            new Concept(List.of("שני הצדדים", "שני האגפים", "בשני צדדי", "שני צדדי", "לשני הצדדים", "אגף"),
                                    "לא הוסבר למה הפעולה נעשית בשני הצדדים יחד"),
                            new Concept(List.of("אותו מספר", "באותו", "אותה פעולה", "אותו דבר"),
                                    "לא נאמר שמחלקים את שני הצדדים באותו מספר"),
                            new Concept(List.of("לבודד", "בידוד", "נעלם", "x", "איקס", "המקדם"),
                                    "לא הוסבר שהמטרה היא לבודד את הנעלם (להיפטר מהמקדם)"),
                            new Concept(List.of("אפס", "0"),
                                    "לא הוזכר שאסור לחלק באפס"))),
            new Prompt("quadratic-two-solutions", SkillCatalog.MATH, "משוואות",
                    "הסבר במילים שלך למה למשוואה ריבועית יכולים להיות שני פתרונות, אחד או אף אחד",
                    List.of(
                            new Concept(List.of("ריבוע", "בריבוע", "חזקה"),
                                    "לא הוסבר שהנעלם מופיע בריבוע"),
                            new Concept(List.of("מינוס", "שלילי", "חיובי", "סימן"),
                                    "לא הוסבר שגם מספר שלילי וגם חיובי נותנים אותו ריבוע"),
                            new Concept(List.of("דיסקרימיננטה", "שורש", "מתחת לשורש", "b²-4ac", "b^2-4ac"),
                                    "לא הוזכר מה קובע את מספר הפתרונות (הביטוי שמתחת לשורש)"),
                            new Concept(List.of("פרבולה", "גרף", "חותך", "ציר"),
                                    "לא נקשר מספר הפתרונות לנקודות החיתוך של הפרבולה עם ציר x"))),
            new Prompt("negative-times-negative", SkillCatalog.MATH, "מספרים שלמים",
                    "הסבר במילים שלך למה מינוס כפול מינוס נותן פלוס",
                    List.of(
                            new Concept(List.of("הפוך", "היפוך", "כיוון", "מתהפך", "נגדי"),
                                    "לא הוסבר שכפל במינוס הופך את הכיוון (הסימן)"),
                            new Concept(List.of("ציר", "ישר המספרים", "שמאלה", "ימינה"),
                                    "לא נעשה שימוש בציר המספרים להסבר"),
                            new Concept(List.of("דפוס", "סדרה", "פעמיים", "חוזר", "חוק הפילוג", "פילוג"),
                                    "חסרה דוגמה או דפוס שמראים למה זה חייב להיות כך"))),
            new Prompt("fractions-common-denominator", SkillCatalog.MATH, "שברים",
                    "הסבר במילים שלך למה צריך מכנה משותף כדי לחבר שברים",
                    List.of(
                            new Concept(List.of("מכנה"),
                                    "לא הוסבר מה המכנה מייצג (לכמה חלקים חילקנו את השלם)"),
                            new Concept(List.of("גודל", "חלקים שווים", "אותו גודל", "חלקים"),
                                    "לא הוסבר שאפשר לחבר רק חלקים באותו גודל"),
                            new Concept(List.of("הרחבה", "להרחיב", "מרחיבים", "כופלים", "כפל"),
                                    "לא הוסבר איך מגיעים למכנה המשותף (הרחבה) בלי לשנות את ערך השבר"),
                            new Concept(List.of("מונה", "מחברים את המונים"),
                                    "לא נאמר שאחרי שהמכנים שווים מחברים רק את המונים"))),
            new Prompt("slope-meaning", SkillCatalog.MATH, "הנדסה אנליטית",
                    "הסבר במילים שלך מה השיפוע של ישר אומר",
                    List.of(
                            new Concept(List.of("y", "וואי", "עולה", "יורד", "גובה"),
                                    "לא הוסבר שהשיפוע מתאר בכמה y משתנה"),
                            new Concept(List.of("x", "איקס", "צעד", "יחידה"),
                                    "לא הוסבר שהשינוי נמדד לכל צעד של יחידה אחת ב-x"),
                            new Concept(List.of("שלילי", "חיובי", "אפס", "אופקי"),
                                    "לא הוסבר מה המשמעות של שיפוע חיובי, שלילי או אפס"),
                            new Concept(List.of("תלול", "תלילות", "מהירות", "קצב"),
                                    "לא נקשר גודל השיפוע לתלילות (לקצב השינוי)"))),
            new Prompt("zero-power", SkillCatalog.MATH, "מספרים שלמים",
                    "הסבר במילים שלך למה כל מספר (חוץ מאפס) בחזקת 0 שווה 1",
                    List.of(
                            new Concept(List.of("חילוק", "מחלקים", "חלקי"),
                                    "לא נעשה שימוש בחילוק חזקות להסבר"),
                            new Concept(List.of("מחסרים", "חיסור", "מעריך", "מעריכים"),
                                    "לא הוסבר שבחילוק חזקות מחסרים מעריכים"),
                            new Concept(List.of("עצמו", "אותו מספר", "שווה 1", "1"),
                                    "לא הוסבר שמספר חלקי עצמו שווה 1"))),
            new Prompt("derivative-meaning", SkillCatalog.MATH, "חשבון אינטגרלי ודפרנציאלי",
                    "הסבר במילים שלך מה הנגזרת של פונקציה מתארת",
                    List.of(
                            new Concept(List.of("שיפוע", "משיק"),
                                    "לא נקשרה הנגזרת לשיפוע המשיק"),
                            new Concept(List.of("קצב", "מהירות", "שינוי"),
                                    "לא הוסבר שהנגזרת היא קצב השינוי של הפונקציה"),
                            new Concept(List.of("נקודה", "רגע", "בכל נקודה"),
                                    "לא הוסבר שהנגזרת מתארת את השינוי בנקודה מסוימת"),
                            new Concept(List.of("עולה", "יורדת", "קיצון", "מקסימום", "מינימום"),
                                    "לא הוזכר מה הנגזרת מלמדת על עלייה, ירידה ונקודות קיצון")))
    );

    private FeynmanPromptCatalog() {}

    public static List<Prompt> all() {
        return PROMPTS;
    }

    public static Optional<Prompt> find(String key) {
        return PROMPTS.stream().filter(p -> p.key().equals(key)).findFirst();
    }

    /**
     * The check used when the AI is not available: the share of key ideas the explanation mentions,
     * capped for very short explanations. The gaps are the ideas that are missing.
     */
    public static Evaluation evaluateByKeywords(Prompt prompt, String explanation) {
        String text = explanation == null ? "" : explanation.toLowerCase(Locale.ROOT);
        List<String> gaps = new ArrayList<>();
        int covered = 0;
        for (Concept concept : prompt.concepts()) {
            boolean found = concept.keywords().stream().anyMatch(k -> text.contains(k.toLowerCase(Locale.ROOT)));
            if (found) {
                covered++;
            } else {
                gaps.add(concept.gap());
            }
        }
        int score = prompt.concepts().isEmpty() ? 0 : Math.round(100f * covered / prompt.concepts().size());
        int words = text.isBlank() ? 0 : text.trim().split("\\s+").length;
        if (words < MIN_WORDS) {
            score = Math.min(score, 40);
            gaps.add("ההסבר קצר מדי – נסו להסביר כאילו אתם מלמדים חבר שלא מכיר את הנושא");
        }
        String feedback = score >= 80 ? "הסבר טוב! הוא מכסה את רוב הרעיונות החשובים."
                : score >= 50 ? "התחלה טובה. כדאי להשלים את הרעיונות החסרים."
                : "ההסבר עדיין לא מראה הבנה מלאה. חזרו לשיעור ונסו שוב.";
        return new Evaluation(score, gaps, feedback);
    }
}
