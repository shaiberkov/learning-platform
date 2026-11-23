package org.example.learningprojectserver.service.adaptive;

/** Practice points and Elo outcome, reduced by each hint the student opened. */
public final class PracticeScoring {

    public static final int FULL_SOLUTION_STEP = 3;
    /** Points left after opening 0, 1, 2 hints or the full solution (3). */
    private static final int[] POINTS_BY_HINTS = {10, 7, 4, 0};
    private static final double[] OUTCOME_BY_HINTS = {1.0, 0.75, 0.5, 0.0};

    private PracticeScoring() {}

    private static int clamp(int hintsUsed) {
        return Math.max(0, Math.min(FULL_SOLUTION_STEP, hintsUsed));
    }

    public static int points(boolean correct, int hintsUsed) {
        return correct ? POINTS_BY_HINTS[clamp(hintsUsed)] : 0;
    }

    /** Points still available before answering. */
    public static int availablePoints(int hintsUsed) {
        return POINTS_BY_HINTS[clamp(hintsUsed)];
    }

    public static double eloOutcome(boolean correct, int hintsUsed) {
        return correct ? OUTCOME_BY_HINTS[clamp(hintsUsed)] : 0.0;
    }
}
