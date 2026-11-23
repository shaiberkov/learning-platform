package org.example.learningprojectserver.service.adaptive;

/**
 * Elo mastery per student per sub-topic. Each generator difficulty level (1-5) has a fixed
 * rating; the student's rating moves after every answer, and practice picks the level
 * slightly above the student's current rating.
 */
public final class EloRating {

    public static final double INITIAL_RATING = 1000;
    public static final double MIN_RATING = 600;
    public static final double MAX_RATING = 1700;
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 5;
    /** How far above the student's rating the next practice question aims. */
    public static final double STRETCH = 50;

    /** Below this the sub-topic shows red on the skill map. */
    public static final double YELLOW_FROM = 1000;
    /** From this up the sub-topic shows green on the skill map. */
    public static final double GREEN_FROM = 1200;

    private EloRating() {}

    public static double levelRating(int level) {
        int l = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
        return 700 + 150 * l;
    }

    public static double expected(double studentRating, double questionRating) {
        return 1.0 / (1.0 + Math.pow(10, (questionRating - studentRating) / 400.0));
    }

    /** Larger steps while the rating is still uncertain. */
    public static double kFactor(int attemptsSoFar) {
        return attemptsSoFar < 10 ? 48 : 32;
    }

    /**
     * New rating after one answer.
     *
     * @param outcome 1 for a clean correct answer, 0 for wrong, in between when hints were used
     */
    public static double update(double studentRating, int attemptsSoFar, int level, double outcome) {
        double e = expected(studentRating, levelRating(level));
        double next = studentRating + kFactor(attemptsSoFar) * (outcome - e);
        return Math.max(MIN_RATING, Math.min(MAX_RATING, next));
    }

    /** The level whose rating is closest to a little above the student's rating. */
    public static int pickLevel(double studentRating) {
        double target = studentRating + STRETCH;
        int best = MIN_LEVEL;
        for (int l = MIN_LEVEL; l <= MAX_LEVEL; l++) {
            if (Math.abs(levelRating(l) - target) < Math.abs(levelRating(best) - target)) {
                best = l;
            }
        }
        return best;
    }

    /** RED, YELLOW or GREEN for the skill map. */
    public static String color(double rating) {
        if (rating >= GREEN_FROM) return "GREEN";
        if (rating >= YELLOW_FROM) return "YELLOW";
        return "RED";
    }

    /** Rating as 0-100 between the minimum and maximum rating, for progress bars. */
    public static int percent(double rating) {
        double p = (rating - MIN_RATING) / (MAX_RATING - MIN_RATING) * 100;
        return (int) Math.round(Math.max(0, Math.min(100, p)));
    }
}
