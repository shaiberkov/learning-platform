package org.example.learningprojectserver.service.realworld;

import org.example.learningprojectserver.service.adaptive.SkillCatalog;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Built-in "why does it matter to me?" texts: an everyday example and a link to a profession for
 * every topic of {@link SkillCatalog#mathTopics()}. A teacher can replace them per school
 * (real_world_override); RealWorldCatalogTest checks that no catalog topic is missing here.
 */
public final class RealWorldCatalog {

    public record Entry(String everydayExample, String careerLink) {}

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        ENTRIES.put("מספרים שלמים", new Entry(
                "כשמתכננים טיול שנתי מחברים את מחיר האוטובוס והכניסות, ומחלקים את הסכום בין התלמידים כדי לדעת כמה כל אחד מביא. "
                        + "חזקות מופיעות כשבודקים כמה תמונות נכנסות בטלפון: ג'יגה-בייט הוא בערך 2 בחזקת 30 בתים.",
                "רואי חשבון משתמשים בפעולות החשבון בכל דוח כספי, ומהנדסי תוכנה משתמשים בחזקות של 2 כשהם מחשבים זיכרון ונפח אחסון."));
        ENTRIES.put("שברים", new Entry(
                "במתכון כתוב 3/4 כוס סוכר ואתם רוצים להכין חצי כמות? זה כפל שברים. "
                        + "גם \"חצי מחיר\", \"שליש מהפיצה\" ו\"רבע שעה\" הם שברים שפוגשים כל יום.",
                "שפים ואופים מגדילים ומקטינים מתכונים בעזרת שברים, ומוזיקאים קוראים תווים שאורכם הוא שבר של תיבה (רבע, שמינית)."));
        ENTRIES.put("משוואות", new Entry(
                "חבילת סלולר עולה 30 ₪ בחודש ועוד חצי שקל לכל דקה, ויש לכם 50 ₪: משוואה עם נעלם אחד אומרת כמה דקות אפשר לדבר. "
                        + "משוואה ריבועית מתארת את המסלול של כדור שנזרק לסל.",
                "כלכלנים פותרים מערכות משוואות כדי למצוא את המחיר שבו ההיצע שווה לביקוש, ומהנדסי בניין פותרים משוואות כדי לחשב עומסים על גשרים."));
        ENTRIES.put("מספרים מרוכבים", new Entry(
                "החשמל בשקע בבית הוא זרם חילופין שמשנה כיוון 50 פעמים בשנייה, והדרך הנוחה לתאר אותו היא במספרים מרוכבים. "
                        + "גם אפליקציה שמנקה רעשי רקע מהקלטה מחשבת איתם.",
                "מהנדסי חשמל משתמשים במספרים מרוכבים כשהם מתכננים רשתות חשמל, ומהנדסי תקשורת משתמשים בהם כשהם מעבדים אותות של Wi-Fi וסלולר."));
        ENTRIES.put("וקטורים", new Entry(
                "באפליקציית ניווט כל תזוזה היא וקטור: כיוון ואורך. כשרוח דוחפת מטוס הצידה, "
                        + "מחברים את וקטור המהירות של המטוס עם וקטור הרוח כדי לדעת לאן הוא באמת טס.",
                "מפתחי משחקי וידאו משתמשים בוקטורים כדי להזיז דמויות ולזהות התנגשויות, וטייסים מחברים וקטורים של מהירות ורוח כדי לתכנן מסלול."));
        ENTRIES.put("הנדסה אנליטית", new Entry(
                "השיפוע של רמפה לכיסא גלגלים או של כביש בעלייה הוא בדיוק השיפוע של ישר. "
                        + "אפליקציית מפות מחשבת מרחק בין שתי נקודות כדי להגיד לכם כמה רחוקה המסעדה.",
                "אדריכלים משתמשים בשיפועים כדי לתכנן גגות ורמפות לפי התקן, ומפתחי מערכות GPS מחשבים מרחקים ונקודות חיתוך בין מסלולים."));
        ENTRIES.put("מטריצות", new Entry(
                "כל תמונה בטלפון היא מטריצה של פיקסלים, ופילטר באפליקציית צילום הוא חישוב על המטריצה הזו. "
                        + "גם טבלת ליגה עם ניצחונות, תיקו והפסדים היא מטריצה.",
                "מהנדסי בינה מלאכותית משתמשים במטריצות בכל רשת נוירונים, ומפתחי גרפיקה ממוחשבת מסובבים ומגדילים עצמים בתלת-ממד בעזרת מטריצות."));
        ENTRIES.put("שאלות מילוליות", new Entry(
                "\"40% הנחה ועוד 10% לחברי מועדון\": האם זה 50% הנחה? לא, זה 46%. "
                        + "חישובי זמן עוזרים לדעת מתי לצאת מהבית כדי להגיע בזמן לאימון.",
                "אנשי שיווק מחשבים הנחות ורווחים לפני כל מבצע, ומתכנני תחבורה ציבורית מחשבים זמני נסיעה כדי לבנות לוחות זמנים לאוטובוסים."));
        ENTRIES.put("חשבון אינטגרלי ודפרנציאלי", new Entry(
                "מד המהירות ברכב מראה נגזרת: כמה מהר משתנה המיקום. האינטגרל עושה את ההפך: "
                        + "מהמהירות לאורך הדרך הוא מחשב כמה קילומטרים נסעתם.",
                "מהנדסי רכב וחלל משתמשים בנגזרות ובאינטגרלים כדי לחשב מהירות, תאוצה וצריכת דלק, וכלכלנים משתמשים בנגזרת כדי למצוא את המחיר שמביא רווח מרבי."));
    }

    private RealWorldCatalog() {}

    /** Topic to built-in entry, in the order of the practice catalog. */
    public static Map<String, Entry> entries() {
        return ENTRIES;
    }

    /** The catalog topic named by a topic or sub-topic, or null when it is not a practice topic. */
    public static String resolveTopic(String topicOrSubTopic) {
        if (topicOrSubTopic == null || topicOrSubTopic.isBlank()) return null;
        String trimmed = topicOrSubTopic.trim();
        if (SkillCatalog.mathTopics().containsKey(trimmed)) return trimmed;
        return SkillCatalog.topicOf(trimmed, null);
    }
}
