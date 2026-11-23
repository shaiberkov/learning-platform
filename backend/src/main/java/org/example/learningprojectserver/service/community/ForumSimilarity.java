package org.example.learningprojectserver.service.community;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Word-overlap similarity between what a student is typing and an existing forum thread, so a student
 * who is stuck sees the discussion that already exists before asking again. No AI: a thread scores by the
 * share of the typed words it contains (title, exercise and body), with a bonus for the same topic.
 */
public final class ForumSimilarity {

    private ForumSimilarity() {}

    /** Threads scoring below this are not shown as similar. */
    public static final int MIN_SCORE = 34;

    private static final Set<String> STOP_WORDS = Set.of(
            "של", "את", "על", "עם", "זה", "זו", "מה", "איך", "למה", "לא", "כן", "יש", "אין", "אני", "גם",
            "או", "אם", "כי", "הוא", "היא", "אבל", "כל", "מי", "לי", "שלי", "תרגיל", "שאלה", "עזרה",
            "the", "and", "how", "what", "why", "is", "to", "of", "in", "a");

    /** Lower-cased words of two letters or more, without stop words. */
    public static Set<String> words(String text) {
        Set<String> out = new LinkedHashSet<>();
        if (text == null || text.isBlank()) return out;
        for (String raw : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (raw.length() >= 2 && !STOP_WORDS.contains(raw)) out.add(raw);
        }
        return out;
    }

    /** The words plus each word without a leading Hebrew prefix letter (ה/ו/ב/ל/מ/ש/כ). */
    public static Set<String> tokens(String text) {
        Set<String> out = new LinkedHashSet<>();
        for (String word : words(text)) {
            out.add(word);
            out.add(stripPrefix(word));
        }
        return out;
    }

    /**
     * 0-100: the percentage of the query's words that appear in the thread, plus 20 when the topics
     * match (capped at 100). An empty query scores 0.
     */
    public static int score(String query, String queryTopic,
                            String title, String exerciseText, String body, String topic) {
        Set<String> wanted = words(query);
        if (wanted.isEmpty()) return 0;
        Set<String> have = tokens(String.join(" ", nz(title), nz(exerciseText), nz(body), nz(topic)));
        long hits = wanted.stream().filter(w -> have.contains(w) || have.contains(stripPrefix(w))).count();
        int score = (int) Math.round(100.0 * hits / wanted.size());
        if (hits > 0 && queryTopic != null && !queryTopic.isBlank() && topic != null
                && queryTopic.trim().equalsIgnoreCase(topic.trim())) {
            score += 20;
        }
        return Math.min(100, score);
    }

    private static String stripPrefix(String word) {
        // A single common Hebrew prefix letter (the, and, in, to, from, that, as).
        if (word.length() > 3 && "הובלמשכ".indexOf(word.charAt(0)) >= 0) {
            return word.substring(1);
        }
        return word;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
