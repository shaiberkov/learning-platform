package org.example.learningprojectserver.service.competition;

import org.example.learningprojectserver.entities.QuestionEntity;
import org.example.learningprojectserver.service.MathQuestion.MathQuestionFactory;
import org.example.learningprojectserver.service.adaptive.SkillCatalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The sub-topics duels and the daily challenge draw from: the MathQuestionFactory generators whose answer is a
 * single exact string a student can type (the factory's TODO-marked generators are left out), and the
 * answer comparison both features share.
 */
public final class CompetitionCatalog {

    public static final int DUEL_QUESTION_COUNT = 5;

    private static final Map<String, List<String>> TOPICS = new LinkedHashMap<>();

    static {
        TOPICS.put("מספרים שלמים", List.of("חיבור מספרים שלמים", "חיסור מספרים שלמים", "כפל מספרים שלמים",
                "חילוק מספרים שלמים", "חזקות", "שורש ריבועי"));
        TOPICS.put("שברים", List.of("חיבור שברים", "חיסור שברים", "כפל שברים"));
        // "משוואה ריבועית" is left out: its generator rounds non-integer roots and answers "x1 = .., x2 = .." in a fixed order.
        TOPICS.put("משוואות", List.of("מישוואה עם נעלם אחד", "מערכת משוואות עם שני נעלמים"));
        TOPICS.put("מספרים מרוכבים", List.of("חיבור מספרים מרוכבים", "חיסור מספרים מרוכבים", "כפל מספרים מרוכבים"));
        TOPICS.put("וקטורים", List.of("חיבור וקטורים", "חיסור וקטורים", "מכפלה סקלרית של וקטורים"));
        TOPICS.put("הנדסה אנליטית", List.of("חישוב מרחק בין שתי נקודות בגרף", "חישוב שיפוע",
                "נקודת חיתוך בין שני ישרים"));
        TOPICS.put("שאלות מילוליות", List.of("שאלה מילולית"));
        TOPICS.put("חשבון אינטגרלי ודפרנציאלי", List.of("ניגזרות"));
    }

    private CompetitionCatalog() {}

    /** Topic to sub-topics, in display order. */
    public static Map<String, List<String>> topics() {
        return TOPICS;
    }

    public static boolean contains(String subTopic) {
        return subTopic != null && TOPICS.values().stream().anyMatch(l -> l.contains(subTopic.trim()));
    }

    public static String topicOf(String subTopic) {
        return SkillCatalog.topicOf(subTopic, null);
    }

    /**
     * Sub-topics that fit a grade level (1-12, or -1 when the grade name is not recognised), easiest first.
     * Lower grades get arithmetic only; each band adds the topics taught from it on.
     */
    public static List<String> forGrade(int gradeLevel) {
        List<String> list = new ArrayList<>(List.of("חיבור מספרים שלמים", "חיסור מספרים שלמים",
                "כפל מספרים שלמים", "חילוק מספרים שלמים"));
        if (gradeLevel < 0 || gradeLevel >= 5) {
            list.addAll(List.of("חזקות", "שורש ריבועי", "חיבור שברים", "חיסור שברים", "כפל שברים"));
        }
        if (gradeLevel >= 7) {
            list.addAll(List.of("מישוואה עם נעלם אחד", "חישוב שיפוע", "חישוב מרחק בין שתי נקודות בגרף",
                    "שאלה מילולית", "מערכת משוואות עם שני נעלמים"));
        }
        if (gradeLevel >= 10) {
            list.addAll(List.of("ניגזרות", "חיבור וקטורים", "מכפלה סקלרית של וקטורים",
                    "כפל מספרים מרוכבים"));
        }
        return list;
    }

    /** Generator level (1-5) for a grade level: two grades per level, unknown grades get level 2. */
    public static int levelForGrade(int gradeLevel) {
        if (gradeLevel < 1) return 2;
        return Math.max(1, Math.min(5, (gradeLevel + 1) / 2 - 1));
    }

    public static QuestionEntity generate(String subTopic, int level) {
        return new MathQuestionFactory().getQuestionGenerator(topicOf(subTopic), subTopic).generateQuestion(level);
    }

    /** Case-insensitive, ignoring every whitespace character ("x = 3" equals "x=3"). */
    public static boolean answersMatch(String given, String expected) {
        if (given == null || expected == null) return false;
        return normalize(given).equals(normalize(expected));
    }

    static String normalize(String s) {
        return s.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
