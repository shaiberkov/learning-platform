package org.example.learningprojectserver.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/** API shapes of the daily challenge. */
public final class DailyChallengeDTOs {

    private DailyChallengeDTOs() {}

    /** A challenge question; answer is null unless the challenge's day is over. */
    public record DailyChallengeQuestionDTO(LocalDate date, String gradeName, String topic, String subTopic,
                                            String questionText, String answer) {}

    /** Yesterday's challenge with its unlocked solution and the caller's own answer. */
    public record DailyChallengeRevealDTO(DailyChallengeQuestionDTO challenge, boolean attempted, String myAnswer,
                                          boolean correct) {}

    /**
     * Today's challenge from the caller's side. status: NOT_STARTED, IN_PROGRESS, ANSWERED or TIMED_OUT.
     * questionText is sent only once started; the solution never (it unlocks tomorrow).
     */
    public record DailyChallengeTodayDTO(LocalDate date, String gradeName, String topic, String subTopic,
                                         String status, int timeLimitSeconds, String questionText,
                                         Integer secondsLeft, String myAnswer, Boolean correct,
                                         int streak, long solvedInGrade, DailyChallengeRevealDTO yesterday) {}

    public record DailyChallengeAnswerRequest(@NotBlank(message = "יש להקליד תשובה") String answer) {}
}
