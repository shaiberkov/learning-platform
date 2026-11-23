package org.example.learningprojectserver.service.examsim;

import org.example.learningprojectserver.service.adaptive.EloRating;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Expected score on a simulation, free of persistence.
 *
 * Model:
 * 1. Mastery estimate. For each question, p = EloRating.expected(rating of its sub-topic, rating of its
 *    level); an unpractised sub-topic uses the initial rating. Each question is a Bernoulli trial worth
 *    its points, so the mastery score is 100 * sum(p * points) / possible, with variance
 *    100^2 * sum(points^2 * p * (1 - p)) / possible^2. A "best N" section uses its N best expected questions.
 * 2. Past simulations. Mean and standard deviation of the student's earlier simulation scores
 *    (standard deviation 12 with fewer than two).
 * 3. Blend. w = n / (n + 2) for n past simulations: expected = (1 - w) * mastery + w * past mean, and the
 *    variances blend the same way. The margin is one standard deviation, widened by up to 50% when little
 *    of the exam's material was ever practised, and kept between 3 and 25 points.
 */
public final class ScorePredictor {

    static final double PAST_SD_DEFAULT = 12;
    static final int MIN_MARGIN = 3;
    static final int MAX_MARGIN = 25;

    public record Question(int points, int level, Double rating) {}

    public record Section(List<Question> questions, Integer questionsToCount) {}

    /** coverage: percent of the counted points whose sub-topic was practised. */
    public record Prediction(int expected, int margin, int coverage) {}

    private ScorePredictor() {}

    public static Prediction predict(List<Section> sections, List<Integer> pastScores) {
        double expectedPoints = 0;
        double variance = 0;
        double possible = 0;
        double practisedPoints = 0;
        double countedPoints = 0;
        for (Section section : sections) {
            List<double[]> items = new ArrayList<>(); // [points, p, practised]
            for (Question q : section.questions()) {
                double rating = q.rating() == null ? EloRating.INITIAL_RATING : q.rating();
                double p = EloRating.expected(rating, EloRating.levelRating(q.level()));
                items.add(new double[]{q.points(), p, q.rating() == null ? 0 : 1});
            }
            Integer k = section.questionsToCount();
            if (k != null && k > 0 && k < items.size()) {
                items.sort(Comparator.comparingDouble((double[] it) -> it[0] * it[1]).reversed());
                items = items.subList(0, k);
            }
            possible += ExamSimGrader.possiblePoints(section.questions().stream().map(Question::points).toList(), k);
            for (double[] it : items) {
                expectedPoints += it[0] * it[1];
                variance += it[0] * it[0] * it[1] * (1 - it[1]);
                countedPoints += it[0];
                practisedPoints += it[0] * it[2];
            }
        }
        if (possible <= 0) return new Prediction(0, MAX_MARGIN, 0);

        double masteryScore = 100 * expectedPoints / possible;
        double masteryVar = 100 * 100 * variance / (possible * possible);
        double coverage = countedPoints == 0 ? 0 : practisedPoints / countedPoints;

        int n = pastScores == null ? 0 : pastScores.size();
        double w = n / (n + 2.0);
        double pastMean = 0;
        double pastSd = PAST_SD_DEFAULT;
        if (n > 0) {
            pastMean = pastScores.stream().mapToInt(Integer::intValue).average().orElse(0);
            if (n >= 2) {
                double m = pastMean;
                double ss = pastScores.stream().mapToDouble(s -> (s - m) * (s - m)).sum();
                pastSd = Math.sqrt(ss / (n - 1));
            }
        }
        double expected = (1 - w) * masteryScore + w * pastMean;
        double sd = Math.sqrt((1 - w) * masteryVar + w * pastSd * pastSd);
        sd *= 1 + 0.5 * (1 - coverage) * (1 - w);

        int exp = (int) Math.round(Math.max(0, Math.min(100, expected)));
        int margin = (int) Math.round(Math.max(MIN_MARGIN, Math.min(MAX_MARGIN, sd)));
        return new Prediction(exp, margin, (int) Math.round(coverage * 100));
    }
}
