package org.example.learningprojectserver.service.gradebook;

import org.example.learningprojectserver.enums.GradeCategory;

import java.util.*;

/**
 * Weighted grade arithmetic, kept free of persistence so it can be tested alone.
 * A category's average is the mean of the student's scores in it, each as a percent of its column's
 * maximum. The final grade is the weighted mean of the category averages the student has, over the
 * categories with a positive weight; weights of categories with no grade are spread over the rest.
 */
public final class GradebookCalculator {

    public record ColumnSpec(String key, GradeCategory category, double maxScore) {}

    public record StudentSummary(Map<String, Double> categoryAverages, Double finalGrade) {}

    private GradebookCalculator() {}

    public static Map<String, StudentSummary> compute(List<String> studentIds,
                                                      List<ColumnSpec> columns,
                                                      Map<String, Map<String, Double>> grades,
                                                      Map<GradeCategory, Integer> weights) {
        Map<String, StudentSummary> result = new LinkedHashMap<>();
        for (String studentId : studentIds) {
            Map<GradeCategory, double[]> sums = new EnumMap<>(GradeCategory.class); // [sum of percents, count]
            for (ColumnSpec column : columns) {
                Double score = grades.getOrDefault(column.key(), Map.of()).get(studentId);
                if (score == null || column.maxScore() <= 0) continue;
                double[] acc = sums.computeIfAbsent(column.category(), c -> new double[2]);
                acc[0] += 100.0 * score / column.maxScore();
                acc[1]++;
            }

            Map<String, Double> averages = new LinkedHashMap<>();
            double weighted = 0;
            double weightTotal = 0;
            for (Map.Entry<GradeCategory, double[]> e : sums.entrySet()) {
                double average = e.getValue()[0] / e.getValue()[1];
                averages.put(e.getKey().name(), round(average));
                int weight = weights.getOrDefault(e.getKey(), 0);
                if (weight > 0) {
                    weighted += weight * average;
                    weightTotal += weight;
                }
            }
            result.put(studentId, new StudentSummary(averages, weightTotal > 0 ? round(weighted / weightTotal) : null));
        }
        return result;
    }

    public static Double average(Collection<Double> values) {
        List<Double> present = values.stream().filter(Objects::nonNull).toList();
        if (present.isEmpty()) return null;
        return round(present.stream().mapToDouble(Double::doubleValue).sum() / present.size());
    }

    static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
