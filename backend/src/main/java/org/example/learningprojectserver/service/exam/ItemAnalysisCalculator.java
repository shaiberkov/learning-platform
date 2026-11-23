package org.example.learningprojectserver.service.exam;

import org.example.learningprojectserver.dto.exam.ItemAnalysisView;

import java.util.*;

/**
 * Classical test theory item statistics.
 * Difficulty index p: share of examinees who answered the item correctly.
 * Discrimination index D: p in the top 27% by total score minus p in the bottom 27%.
 */
public final class ItemAnalysisCalculator {

    public static final double GROUP_SHARE = 0.27;
    public static final double TOO_HARD = 0.25;
    public static final double TOO_EASY = 0.90;
    public static final double WEAK_DISCRIMINATION = 0.20;

    public static final String FLAG_TOO_HARD = "קשה מאוד: רוב הנבחנים טעו";
    public static final String FLAG_TOO_EASY = "קלה מאוד: כמעט כולם צדקו";
    public static final String FLAG_NEGATIVE = "מבחינה שלילית: החזקים טעו יותר מהחלשים, כדאי לבדוק את מפתח התשובה";
    public static final String FLAG_WEAK = "מבחינה חלשה: השאלה כמעט לא מבדילה בין חזקים לחלשים";

    /** One graded answer. */
    public record Row(Long attemptId, Integer attemptScore, Long questionId, boolean correct) {}

    /** A question of the exam, in display order. */
    public record Question(Long id, int position, String text, String kind) {}

    private ItemAnalysisCalculator() {}

    public static int groupSize(int examinees) {
        if (examinees < 2) {
            return 0;
        }
        return Math.max(1, (int) Math.round(GROUP_SHARE * examinees));
    }

    public static List<ItemAnalysisView.Item> analyse(List<Question> questions, List<Row> rows) {
        Map<Long, Integer> scoreByAttempt = new HashMap<>();
        for (Row r : rows) {
            scoreByAttempt.put(r.attemptId(), r.attemptScore() == null ? 0 : r.attemptScore());
        }
        List<Long> ranked = new ArrayList<>(scoreByAttempt.keySet());
        ranked.sort(Comparator.<Long, Integer>comparing(scoreByAttempt::get).reversed().thenComparing(id -> id));
        int g = groupSize(ranked.size());
        Set<Long> upper = new HashSet<>(ranked.subList(0, g));
        Set<Long> lower = new HashSet<>(ranked.subList(ranked.size() - g, ranked.size()));

        Map<Long, int[]> counts = new HashMap<>(); // answered, correct, upperCorrect, lowerCorrect
        for (Row r : rows) {
            int[] c = counts.computeIfAbsent(r.questionId(), k -> new int[4]);
            c[0]++;
            if (r.correct()) {
                c[1]++;
                if (upper.contains(r.attemptId())) {
                    c[2]++;
                }
                if (lower.contains(r.attemptId())) {
                    c[3]++;
                }
            }
        }

        List<ItemAnalysisView.Item> items = new ArrayList<>();
        for (Question q : questions) {
            int[] c = counts.getOrDefault(q.id(), new int[4]);
            ItemAnalysisView.Item item = new ItemAnalysisView.Item();
            item.setQuestionId(q.id());
            item.setPosition(q.position());
            item.setText(q.text());
            item.setKind(q.kind());
            item.setAnsweredCount(c[0]);
            item.setCorrectCount(c[1]);
            if (c[0] > 0) {
                double p = (double) c[1] / c[0];
                item.setDifficulty(round(p));
                if (p < TOO_HARD) {
                    item.getFlags().add(FLAG_TOO_HARD);
                } else if (p > TOO_EASY) {
                    item.getFlags().add(FLAG_TOO_EASY);
                }
            }
            if (g > 0 && c[0] > 0) {
                double d = ((double) c[2] - c[3]) / g;
                item.setDiscrimination(round(d));
                if (d < 0) {
                    item.getFlags().add(FLAG_NEGATIVE);
                } else if (d < WEAK_DISCRIMINATION) {
                    item.getFlags().add(FLAG_WEAK);
                }
            }
            items.add(item);
        }
        return items;
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
