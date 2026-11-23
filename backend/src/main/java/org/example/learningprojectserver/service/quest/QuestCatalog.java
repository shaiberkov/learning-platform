package org.example.learningprojectserver.service.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The story quests and the cosmetic rewards they unlock. Defined in code (like the question generators):
 * every chapter names a sub-topic that MathQuestionFactory knows, so its questions and exact answers come
 * from the existing generators. Aimed at the younger grades: low difficulty, short chapters.
 */
public final class QuestCatalog {

    private QuestCatalog() {}

    public static final String AVATAR = "AVATAR";
    public static final String ROOM = "ROOM";

    /** One step of the story; the student advances after questionsToPass correct answers. */
    public record Chapter(String title, String story, String subTopic, int difficulty, int questionsToPass) {}

    public record Quest(String code, String title, String emoji, String tagline, String intro, String outro,
                        String gradeBand, String topic, List<Chapter> chapters, List<String> rewardItems) {}

    /** tone is a theme token name (accent, mint, amber) the UI uses for the room background. */
    public record Item(String code, String kind, String name, String emoji, String tone, String decor) {}

    public static final Item DEFAULT_AVATAR = new Item("AVATAR_DEFAULT", AVATAR, "חיוך", "🙂", "accent", "");
    public static final Item DEFAULT_ROOM = new Item("ROOM_DEFAULT", ROOM, "החדר שלי", "🏠", "accent", "📚 ✏️ 🪴");

    private static final List<Item> REWARD_ITEMS = List.of(
            new Item("AVATAR_ASTRONAUT", AVATAR, "אסטרונאוט/ית", "🧑‍🚀", "accent", ""),
            new Item("ROOM_SPACE", ROOM, "תחנת חלל", "🪐", "accent", "🚀 ⭐ 🌌 🛰️"),
            new Item("AVATAR_PIRATE", AVATAR, "תוכי של פיראטים", "🦜", "amber", ""),
            new Item("ROOM_ISLAND", ROOM, "האי הנעלם", "🏝️", "amber", "🏴‍☠️ 💰 🗺️ 🐚"),
            new Item("AVATAR_WIZARD", AVATAR, "קוסם/ת", "🧙", "mint", ""),
            new Item("ROOM_FOREST", ROOM, "בקתה ביער", "🌳", "mint", "🍄 🦉 ✨ 🌙"),
            new Item("AVATAR_DIVER", AVATAR, "צולל/ת", "🤿", "accent", ""),
            new Item("ROOM_OCEAN", ROOM, "מעמקי הים", "🐠", "accent", "🐙 🐚 🫧 🐳")
    );

    public static final List<Quest> QUESTS = List.of(
            new Quest("MARS", "משלחת למאדים", "🚀",
                    "פותרים תרגילים כדי להביא את החללית בשלום עד מאדים",
                    "צוות החלל צריך אותך! החללית מוכנה, אבל בלי חישובים מדויקים היא לא תגיע ליעד. כל תרגיל נכון מקרב אותנו למאדים.",
                    "נחתנו! הבסיס על מאדים פועל בזכותך. קיבלת את חליפת האסטרונאוט ואת תחנת החלל לחדר שלך.",
                    "ג'–ו'", "מספרים שלמים",
                    List.of(
                            new Chapter("השיגור", "כדי לשגר את החללית צריך לחשב כמה דלק נטען. חברו את המספרים!",
                                    "חיבור מספרים שלמים", 1, 3),
                            new Chapter("חגורת האסטרואידים", "אסטרואידים בדרך! חשבו כמה נשאר אחרי כל התחמקות.",
                                    "חיסור מספרים שלמים", 1, 3),
                            new Chapter("הנחיתה", "כדי לנחות בבטחה צריך לחשב את כמות המצנחים לכל תא.",
                                    "כפל מספרים שלמים", 1, 3),
                            new Chapter("הבסיס על מאדים", "מחלקים את האספקה בין חברי הצוות בצורה שווה.",
                                    "חילוק מספרים שלמים", 1, 3)),
                    List.of("AVATAR_ASTRONAUT", "ROOM_SPACE")),
            new Quest("PIRATES", "אוצר האי הנעלם", "🏴‍☠️",
                    "מפת אוצר קרועה לחתיכות: רק שברים יחברו אותה",
                    "מצאנו מפת אוצר, אבל היא קרועה לחתיכות. כל שבר שתחשבו נכון מחזיר חלק מהמפה.",
                    "תיבת האוצר נפתחה! קיבלת תוכי של פיראטים ואת האי הנעלם לחדר שלך.",
                    "ד'–ו'", "שברים",
                    List.of(
                            new Chapter("חתיכות המפה", "מחברים את חתיכות המפה: חברו את השברים.",
                                    "חיבור שברים", 1, 3),
                            new Chapter("הסערה", "הסערה לקחה חלק מהמטען. כמה נשאר?",
                                    "חיסור שברים", 1, 3),
                            new Chapter("חלוקת האוצר", "כל פיראט מקבל חלק מהחלק. כפלו את השברים.",
                                    "כפל שברים", 1, 3)),
                    List.of("AVATAR_PIRATE", "ROOM_ISLAND")),
            new Quest("FOREST", "יער הקסמים", "🧙",
                    "לחשים שעובדים רק עם חזקות, שורשים ומשוואות",
                    "שער יער הקסמים נעול בלחשים מספריים. רק מי שפותר/ת אותם יכול/ה לעבור.",
                    "השער נפתח! הפכת לקוסם/ת של היער וקיבלת בקתה ביער לחדר שלך.",
                    "ה'–ו'", "חזקות, שורשים ומשוואות",
                    List.of(
                            new Chapter("לחש הכפל הקסום", "הלחש מכפיל מספר בעצמו שוב ושוב. חשבו את החזקה.",
                                    "חזקות", 1, 3),
                            new Chapter("שורשי העץ העתיק", "העץ העתיק שואל: איזה מספר כפול עצמו נותן את זה?",
                                    "שורש ריבועי", 1, 3),
                            new Chapter("השער הנעול", "מצאו את המספר החסר כדי לפתוח את השער.",
                                    "מישוואה עם נעלם אחד", 1, 3)),
                    List.of("AVATAR_WIZARD", "ROOM_FOREST")),
            new Quest("OCEAN", "מסע למעמקי הים", "🐙",
                    "צוללים לעומק ופותרים בעיות של יצורי הים",
                    "הצוללת מוכנה לצלילה. יצורי הים צריכים עזרה, וכל בעיה שתפתרו מורידה אותנו עוד קצת לעומק.",
                    "הגענו לקרקעית! יצורי הים מודים לך. קיבלת מסכת צלילה ואת מעמקי הים לחדר שלך.",
                    "ג'–ו'", "בעיות מילוליות",
                    List.of(
                            new Chapter("הצלילה", "התמנון מבקש עזרה בספירה. קראו בעיון ופתרו.",
                                    "שאלה מילולית", 1, 3),
                            new Chapter("להקת הדגים", "כמה דגים יש בכל הלהקות יחד?",
                                    "כפל מספרים שלמים", 2, 3),
                            new Chapter("הקרקעית", "מחלקים את הפנינים בין הצדפות באופן שווה.",
                                    "חילוק מספרים שלמים", 2, 3)),
                    List.of("AVATAR_DIVER", "ROOM_OCEAN"))
    );

    private static final Map<String, Quest> QUESTS_BY_CODE =
            QUESTS.stream().collect(Collectors.toMap(Quest::code, Function.identity()));

    private static final Map<String, Item> ITEMS_BY_CODE;

    static {
        List<Item> all = new ArrayList<>();
        all.add(DEFAULT_AVATAR);
        all.add(DEFAULT_ROOM);
        all.addAll(REWARD_ITEMS);
        ITEMS_BY_CODE = all.stream().collect(Collectors.toMap(Item::code, Function.identity()));
    }

    public static Quest quest(String code) {
        return code == null ? null : QUESTS_BY_CODE.get(code);
    }

    public static Item item(String code) {
        return code == null ? null : ITEMS_BY_CODE.get(code);
    }

    /** Defaults first, then rewards in quest order. */
    public static List<Item> allItems() {
        List<Item> all = new ArrayList<>();
        all.add(DEFAULT_AVATAR);
        all.add(DEFAULT_ROOM);
        all.addAll(REWARD_ITEMS);
        return all;
    }

    public static boolean isDefault(String itemCode) {
        return DEFAULT_AVATAR.code().equals(itemCode) || DEFAULT_ROOM.code().equals(itemCode);
    }

    /** The quest whose rewards include this item, or null for the defaults. */
    public static Quest questOfItem(String itemCode) {
        return QUESTS.stream().filter(q -> q.rewardItems().contains(itemCode)).findFirst().orElse(null);
    }
}
