package org.example.learningprojectserver.service.gradebook;

import org.example.learningprojectserver.enums.GradeCategory;

import java.util.*;

/**
 * "What do I need?" arithmetic, free of persistence, using the same rule as GradebookCalculator:
 * a category's average is the mean of its column percents, and the final grade is the weighted mean
 * of the categories that have columns, over categories with a positive weight.
 *
 * If the student scores the same percent x in every remaining column, each category average is
 * (graded sum + remaining count * x) / (graded count + remaining count), so the final grade is
 * linear in x: F(x) = F(0) + (F(100) - F(0)) * x / 100. The needed x is solved from F(x) = target.
 */
public final class GradeTargetCalculator {

    public enum Status { POSSIBLE, ALREADY_SECURED, IMPOSSIBLE, NO_REMAINING_MET, NO_REMAINING_MISSED, NO_DATA }

    /** percent is null for a column the student has no grade in yet. */
    public record Column(GradeCategory category, Double percent) {}

    /** current is the grade from graded columns only (null when nothing is graded). */
    public record Plan(Status status, Double current, Double min, Double max, Double requiredPercent) {}

    private GradeTargetCalculator() {}

    public static Plan plan(List<Column> columns, Map<GradeCategory, Integer> weights, double target) {
        Double current = finalGrade(columns, weights, null);
        boolean hasRemaining = columns.stream()
                .anyMatch(c -> c.percent() == null && weights.getOrDefault(c.category(), 0) > 0);
        if (!hasRemaining) {
            if (current == null) return new Plan(Status.NO_DATA, null, null, null, null);
            Status s = current >= target - 1e-9 ? Status.NO_REMAINING_MET : Status.NO_REMAINING_MISSED;
            return new Plan(s, round(current), round(current), round(current), null);
        }
        double min = finalGrade(columns, weights, 0.0);
        double max = finalGrade(columns, weights, 100.0);
        return solve(current, min, max, target);
    }

    /** Solves F(x) = target for a grade that is linear between min (x = 0) and max (x = 100). */
    public static Plan solve(Double current, double min, double max, double target) {
        Double c = current == null ? null : round(current);
        if (min >= target - 1e-9) return new Plan(Status.ALREADY_SECURED, c, round(min), round(max), 0.0);
        if (max < target - 1e-9) return new Plan(Status.IMPOSSIBLE, c, round(min), round(max), null);
        double x = 100 * (target - min) / (max - min);
        double required = Math.ceil(x * 10 - 1e-9) / 10.0; // round up: rounding down could miss the target
        return new Plan(Status.POSSIBLE, c, round(min), round(max), Math.min(100, required));
    }

    /**
     * The weighted final grade with every remaining column at {@code fill} percent; with fill null,
     * remaining columns are left out (the current grade). Null when no weighted category has a value.
     */
    static Double finalGrade(List<Column> columns, Map<GradeCategory, Integer> weights, Double fill) {
        Map<GradeCategory, double[]> sums = new EnumMap<>(GradeCategory.class);
        for (Column col : columns) {
            Double value = col.percent() != null ? col.percent() : fill;
            if (value == null) continue;
            double[] acc = sums.computeIfAbsent(col.category(), k -> new double[2]);
            acc[0] += value;
            acc[1]++;
        }
        double weighted = 0;
        double total = 0;
        for (Map.Entry<GradeCategory, double[]> e : sums.entrySet()) {
            int w = weights.getOrDefault(e.getKey(), 0);
            if (w <= 0) continue;
            weighted += w * e.getValue()[0] / e.getValue()[1];
            total += w;
        }
        return total > 0 ? weighted / total : null;
    }

    static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
