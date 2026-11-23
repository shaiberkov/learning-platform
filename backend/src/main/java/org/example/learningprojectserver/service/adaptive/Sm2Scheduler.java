package org.example.learningprojectserver.service.adaptive;

import java.time.LocalDate;

/** SuperMemo-2 scheduling for the mistakes notebook and the flashcards. */
public final class Sm2Scheduler {

    public static final double INITIAL_EASINESS = 2.5;
    public static final double MIN_EASINESS = 1.3;
    /** Review quality given for a correct answer in the notebook (0-5 scale). */
    public static final int QUALITY_CORRECT = 4;
    /** Review quality given for a wrong answer in the notebook (0-5 scale). */
    public static final int QUALITY_WRONG = 1;

    private Sm2Scheduler() {}

    /** Puts a new mistake, or a known one answered wrong again, at the start of the schedule, due today. */
    public static void resetAsNewMistake(Sm2Schedulable item, LocalDate today) {
        if (item.getId() != null) {
            item.setLapses(item.getLapses() + 1);
        } else {
            item.setEasiness(INITIAL_EASINESS);
        }
        item.setRepetitions(0);
        item.setIntervalDays(0);
        item.setDueDate(today);
    }

    /** Applies one review with SM-2 quality 0-5 and moves the due date. */
    public static void review(Sm2Schedulable item, int quality, LocalDate today) {
        int q = Math.max(0, Math.min(5, quality));
        if (q >= 3) {
            int interval;
            if (item.getRepetitions() == 0) {
                interval = 1;
            } else if (item.getRepetitions() == 1) {
                interval = 6;
            } else {
                interval = (int) Math.round(Math.max(1, item.getIntervalDays()) * item.getEasiness());
            }
            item.setRepetitions(item.getRepetitions() + 1);
            item.setIntervalDays(interval);
        } else {
            item.setRepetitions(0);
            item.setIntervalDays(1);
            item.setLapses(item.getLapses() + 1);
        }
        double ef = item.getEasiness() + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
        item.setEasiness(Math.max(MIN_EASINESS, ef));
        item.setReviewCount(item.getReviewCount() + 1);
        item.setDueDate(today.plusDays(item.getIntervalDays()));
    }
}
