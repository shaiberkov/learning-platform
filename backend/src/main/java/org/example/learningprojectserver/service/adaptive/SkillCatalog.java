package org.example.learningprojectserver.service.adaptive;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every practisable topic and sub-topic, using the sub-topic names {@code MathQuestionFactory}
 * accepts. Some generators label their questions with a shorter sub-topic name; those are
 * mapped back here so mastery and the skill map use one name per sub-topic.
 */
public final class SkillCatalog {

    public static final String MATH = "מתמטיקה";

    private static final Map<String, List<String>> MATH_TOPICS = new LinkedHashMap<>();
    private static final Map<String, String> ALIASES = Map.of(
            "חישוב מרחק", "חישוב מרחק בין שתי נקודות בגרף",
            "נקודת חיתוך", "נקודת חיתוך בין שני ישרים",
            "חיתוך פרבולות", "נקודות חיתוך בין פרבולות"
    );

    static {
        MATH_TOPICS.put("מספרים שלמים", List.of("חיבור מספרים שלמים", "חיסור מספרים שלמים", "כפל מספרים שלמים",
                "חילוק מספרים שלמים", "חזקות", "שורש ריבועי", "סדרות דינמיות"));
        MATH_TOPICS.put("שברים", List.of("חיבור שברים", "חיסור שברים", "כפל שברים", "חיבור וחיסור שברים"));
        MATH_TOPICS.put("משוואות", List.of("מישוואה עם נעלם אחד", "מערכת משוואות עם שני נעלמים", "משוואה ריבועית"));
        MATH_TOPICS.put("מספרים מרוכבים", List.of("חיבור מספרים מרוכבים", "חיסור מספרים מרוכבים",
                "כפל מספרים מרוכבים", "חילוק מספרים מרוכבים"));
        MATH_TOPICS.put("וקטורים", List.of("חיבור וקטורים", "חיסור וקטורים", "מכפלה סקלרית של וקטורים"));
        MATH_TOPICS.put("הנדסה אנליטית", List.of("חישוב מרחק בין שתי נקודות בגרף", "חישוב שיפוע",
                "נקודת חיתוך בין שני ישרים", "נקודות חיתוך בין פרבולות"));
        MATH_TOPICS.put("מטריצות", List.of("חיבור מטריצות"));
        MATH_TOPICS.put("שאלות מילוליות", List.of("שאלה מילולית", "חישוב זמן", "חישוב הנחות"));
        MATH_TOPICS.put("חשבון אינטגרלי ודפרנציאלי", List.of("ניגזרות", "אינטגרלים"));
    }

    private SkillCatalog() {}

    /** Topic to sub-topics, in display order. */
    public static Map<String, List<String>> mathTopics() {
        return MATH_TOPICS;
    }

    public static String canonicalSubTopic(String subTopic) {
        if (subTopic == null) return null;
        String trimmed = subTopic.trim();
        return ALIASES.getOrDefault(trimmed, trimmed);
    }

    /** The catalog topic of a sub-topic, or {@code fallback} when the sub-topic is not listed. */
    public static String topicOf(String subTopic, String fallback) {
        String canonical = canonicalSubTopic(subTopic);
        for (Map.Entry<String, List<String>> e : MATH_TOPICS.entrySet()) {
            if (e.getValue().contains(canonical)) return e.getKey();
        }
        return fallback;
    }

    public static boolean contains(String subTopic) {
        return topicOf(subTopic, null) != null;
    }
}
