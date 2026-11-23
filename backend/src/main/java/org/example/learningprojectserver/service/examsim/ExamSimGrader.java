package org.example.learningprojectserver.service.examsim;

import org.example.learningprojectserver.service.exam.LatexAnswerParser;

import java.util.*;

/**
 * Rubric grading of a simulation answer, free of persistence.
 * An expected answer is split into parts at top-level commas ("x = 3, y = -2" has two parts,
 * "(3, -2)" has one). Each expected part matched by a distinct given part (same text after
 * LatexAnswerParser.normalize, or the same number) earns points / parts; order does not matter.
 * A section with questionsToCount keeps only the best N questions, as in an exam with a choice.
 */
public final class ExamSimGrader {

    private static final double EPSILON = 1e-6;

    private ExamSimGrader() {}

    /** Fraction of the question's points earned, 0..1. */
    public static double fraction(String expected, String given) {
        if (expected == null || given == null || given.isBlank()) return 0;
        List<String> want = parts(expected);
        List<String> have = new ArrayList<>(parts(given));
        if (want.isEmpty()) return 0;
        int matched = 0;
        for (String w : want) {
            for (Iterator<String> it = have.iterator(); it.hasNext(); ) {
                if (same(w, it.next())) {
                    matched++;
                    it.remove();
                    break;
                }
            }
        }
        return (double) matched / want.size();
    }

    /** Points earned, rounded to one decimal. */
    public static double points(String expected, String given, int points) {
        return Math.round(fraction(expected, given) * points * 10.0) / 10.0;
    }

    public static int partCount(String expected) {
        return expected == null ? 1 : Math.max(1, parts(expected).size());
    }

    /** The answer's shape with every number replaced by a box, e.g. "x = □, y = □"; null for a single number. */
    public static String answerHint(String expected) {
        if (expected == null) return null;
        String hint = expected.replaceAll("-?\\d+(\\.\\d+)?(/\\d+)?", "□");
        return hint.trim().equals("□") ? null : hint;
    }

    /**
     * Indexes of the questions that count in a section: all of them, or the best {@code toCount}
     * by earned points (ties keep the earlier question).
     */
    public static Set<Integer> countedIndexes(List<Double> earned, Integer toCount) {
        Set<Integer> all = new LinkedHashSet<>();
        for (int i = 0; i < earned.size(); i++) all.add(i);
        if (toCount == null || toCount <= 0 || toCount >= earned.size()) return all;
        List<Integer> order = new ArrayList<>(all);
        order.sort((a, b) -> Double.compare(earned.get(b), earned.get(a)));
        return new LinkedHashSet<>(order.subList(0, toCount));
    }

    /** The most points a section can give: the sum of its {@code toCount} largest question values. */
    public static int possiblePoints(List<Integer> points, Integer toCount) {
        List<Integer> sorted = new ArrayList<>(points);
        sorted.sort(Comparator.reverseOrder());
        int n = toCount == null || toCount <= 0 ? sorted.size() : Math.min(toCount, sorted.size());
        int sum = 0;
        for (int i = 0; i < n; i++) sum += sorted.get(i);
        return sum;
    }

    static List<String> parts(String answer) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : answer.toCharArray()) {
            if (c == '(' || c == '[' || c == '{') depth++;
            if (c == ')' || c == ']' || c == '}') depth = Math.max(0, depth - 1);
            if ((c == ',' || c == ';') && depth == 0) {
                add(out, current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        add(out, current.toString());
        return out;
    }

    private static void add(List<String> out, String part) {
        String n = LatexAnswerParser.normalize(part);
        if (!n.isEmpty()) out.add(n);
    }

    private static boolean same(String a, String b) {
        if (a.equals(b)) return true;
        Double x = LatexAnswerParser.parseNumber(a);
        Double y = LatexAnswerParser.parseNumber(b);
        return x != null && y != null && Math.abs(x - y) < EPSILON;
    }
}
