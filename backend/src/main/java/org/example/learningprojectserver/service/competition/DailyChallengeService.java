package org.example.learningprojectserver.service.competition;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.DailyChallengeDTOs.*;
import org.example.learningprojectserver.entities.DailyChallengeAttemptEntity;
import org.example.learningprojectserver.entities.DailyChallengeEntity;
import org.example.learningprojectserver.entities.QuestionEntity;
import org.example.learningprojectserver.mappers.DailyChallengeEntityToQuestionDTOMapper;
import org.example.learningprojectserver.repository.DailyChallengeAttemptRepository;
import org.example.learningprojectserver.repository.DailyChallengeRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.adaptive.SkillCatalog;
import org.example.learningprojectserver.utils.GradeLevels;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

/**
 * One question a day per grade level of a school, the same for every student of that grade. The sub-topic is
 * picked deterministically from (school, grade, date); the question itself is generated once, by the first
 * student who opens the challenge that day, and stored (unique per school, grade and date), so everyone sees
 * the same question (the generator is random, so deleting the stored row would change that day's question).
 * A student has TIME_LIMIT_SECONDS from "start" to answer; the solution is sent only from
 * the next day on. The streak counts consecutive days answered correctly.
 *
 * Days follow Israel time. Not transactional on purpose: two students creating the same day's challenge at once
 * hit the unique key, and the loser reads the winner's row.
 */
@Service
@RequiredArgsConstructor
public class DailyChallengeService {

    public static final int TIME_LIMIT_SECONDS = 120;
    /** Network slack added to the limit when an answer arrives. */
    static final int GRACE_SECONDS = 5;
    static final String NO_GRADE = "כללי";
    static final int STREAK_LOOKBACK_DAYS = 400;

    private final DailyChallengeRepository challengeRepository;
    private final DailyChallengeAttemptRepository attemptRepository;
    private final DailyChallengeEntityToQuestionDTOMapper questionMapper;

    /** Replaced in tests. */
    Clock clock = Clock.system(ZoneId.of("Asia/Jerusalem"));

    public BasicResponse<DailyChallengeTodayDTO> getToday(String userId) {
        String[] scope = scope(userId);
        if (scope == null) return fail("האתגר היומי זמין לתלמידים המשויכים לבית ספר");
        LocalDate today = LocalDate.now(clock);
        DailyChallengeEntity challenge = challengeFor(scope[0], scope[1], today);
        DailyChallengeAttemptEntity attempt = attemptRepository
                .findByChallengeIdAndStudentUserId(challenge.getId(), userId).orElse(null);
        return ok(null, today(userId, scope, challenge, attempt));
    }

    /** Starts the 2-minute clock and shows the question. Calling it again does not restart the clock. */
    public BasicResponse<DailyChallengeTodayDTO> start(String userId) {
        String[] scope = scope(userId);
        if (scope == null) return fail("האתגר היומי זמין לתלמידים המשויכים לבית ספר");
        LocalDate today = LocalDate.now(clock);
        DailyChallengeEntity challenge = challengeFor(scope[0], scope[1], today);
        DailyChallengeAttemptEntity attempt = attemptRepository
                .findByChallengeIdAndStudentUserId(challenge.getId(), userId).orElse(null);
        if (attempt == null) {
            attempt = new DailyChallengeAttemptEntity();
            attempt.setChallengeId(challenge.getId());
            attempt.setStudentUserId(userId);
            attempt.setChallengeDate(today);
            attempt.setStartedAt(LocalDateTime.now(clock));
            try {
                attempt = attemptRepository.save(attempt);
            } catch (DataIntegrityViolationException e) {
                // A double click: the first request already started the clock.
                attempt = attemptRepository.findByChallengeIdAndStudentUserId(challenge.getId(), userId).orElseThrow();
            }
        }
        return ok(null, today(userId, scope, challenge, attempt));
    }

    public BasicResponse<DailyChallengeTodayDTO> answer(String userId, String answer) {
        String[] scope = scope(userId);
        if (scope == null) return fail("האתגר היומי זמין לתלמידים המשויכים לבית ספר");
        String given = answer == null ? "" : answer.trim();
        if (given.isEmpty()) return fail("יש להקליד תשובה");
        if (given.length() > 200) return fail("התשובה ארוכה מדי");
        LocalDate today = LocalDate.now(clock);
        DailyChallengeEntity challenge = challengeFor(scope[0], scope[1], today);
        DailyChallengeAttemptEntity attempt = attemptRepository
                .findByChallengeIdAndStudentUserId(challenge.getId(), userId).orElse(null);
        LocalDateTime now = LocalDateTime.now(clock);
        DailyChallengeEntity answered = challenge;
        if (attempt == null) {
            // Started just before midnight: the answer belongs to yesterday's challenge while its clock runs.
            DailyChallengeEntity prev = challengeRepository
                    .findBySchoolCodeAndGradeNameAndChallengeDate(scope[0], scope[1], today.minusDays(1)).orElse(null);
            DailyChallengeAttemptEntity open = prev == null ? null : attemptRepository
                    .findByChallengeIdAndStudentUserId(prev.getId(), userId).orElse(null);
            if (open == null || open.getAnsweredAt() != null
                    || now.isAfter(open.getStartedAt().plusSeconds(TIME_LIMIT_SECONDS + GRACE_SECONDS))) {
                return fail("יש להתחיל את האתגר לפני שעונים");
            }
            answered = prev;
            attempt = open;
        }
        if (attempt.getAnsweredAt() != null) return fail("כבר ענית על האתגר של היום");

        boolean inTime = !now.isAfter(attempt.getStartedAt().plusSeconds(TIME_LIMIT_SECONDS + GRACE_SECONDS));
        attempt.setAnsweredAt(now);
        attempt.setAnswer(given);
        attempt.setCorrect(inTime && CompetitionCatalog.answersMatch(given, answered.getAnswer()));
        attemptRepository.save(attempt);

        String message = !inTime ? "הזמן נגמר. התשובה לא נספרה"
                : attempt.isCorrect() ? "תשובה נכונה! הפתרון ייפתח מחר" : "התשובה לא נכונה. הפתרון ייפתח מחר";
        // After a midnight answer the screen moves on to today's challenge, which is not started yet.
        return ok(message, today(userId, scope, challenge, answered == challenge ? attempt : null));
    }

    // ---- rules ----

    /** The sub-topic of a (school, grade, date): the same on every call, different from day to day. */
    static String pickSubTopic(String schoolCode, String gradeName, LocalDate date) {
        List<String> options = CompetitionCatalog.forGrade(GradeLevels.level(gradeName));
        int hash = (schoolCode + "|" + gradeName + "|" + date).hashCode();
        return options.get(Math.floorMod(hash, options.size()));
    }

    /**
     * Consecutive days answered correctly, ending today, or yesterday while today's challenge is still open
     * (the caller passes 0 instead once today's answer is wrong or late).
     * {@code correctDates} is newest first.
     */
    static int streak(List<LocalDate> correctDates, LocalDate today) {
        if (correctDates.isEmpty()) return 0;
        LocalDate expected = correctDates.get(0).equals(today) ? today : today.minusDays(1);
        int streak = 0;
        for (LocalDate d : correctDates) {
            if (d.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (d.isBefore(expected)) {
                break;
            }
        }
        return streak;
    }

    // ---- internals ----

    private DailyChallengeTodayDTO today(String userId, String[] scope, DailyChallengeEntity challenge,
                                        DailyChallengeAttemptEntity attempt) {
        LocalDate today = challenge.getChallengeDate();
        LocalDateTime now = LocalDateTime.now(clock);
        String status;
        Integer secondsLeft = null;
        if (attempt == null) {
            status = "NOT_STARTED";
        } else if (attempt.getAnsweredAt() != null) {
            boolean late = attempt.getAnsweredAt()
                    .isAfter(attempt.getStartedAt().plusSeconds(TIME_LIMIT_SECONDS + GRACE_SECONDS));
            status = late ? "TIMED_OUT" : "ANSWERED";
        } else {
            long left = TIME_LIMIT_SECONDS - Duration.between(attempt.getStartedAt(), now).getSeconds();
            status = left > 0 ? "IN_PROGRESS" : "TIMED_OUT";
            secondsLeft = (int) Math.max(0, left);
        }
        boolean showQuestion = attempt != null;
        // A wrong or late answer today breaks the streak now, not tomorrow.
        boolean failedToday = status.equals("TIMED_OUT") || (attempt != null && attempt.getAnsweredAt() != null
                && !attempt.isCorrect());
        int streak = failedToday ? 0
                : streak(attemptRepository.findCorrectDates(userId, today.minusDays(STREAK_LOOKBACK_DAYS)), today);
        long solved = attemptRepository.countByChallengeIdAndCorrectTrue(challenge.getId());

        return new DailyChallengeTodayDTO(today, challenge.getGradeName(), challenge.getTopic(),
                challenge.getSubTopic(), status, TIME_LIMIT_SECONDS,
                showQuestion ? challenge.getQuestionText() : null, secondsLeft,
                attempt == null ? null : attempt.getAnswer(),
                attempt == null || attempt.getAnsweredAt() == null ? null : attempt.isCorrect(),
                streak, solved, yesterday(userId, scope, today.minusDays(1)));
    }

    private DailyChallengeRevealDTO yesterday(String userId, String[] scope, LocalDate date) {
        DailyChallengeEntity challenge = challengeRepository
                .findBySchoolCodeAndGradeNameAndChallengeDate(scope[0], scope[1], date).orElse(null);
        if (challenge == null) return null;
        DailyChallengeAttemptEntity attempt = attemptRepository
                .findByChallengeIdAndStudentUserId(challenge.getId(), userId).orElse(null);
        DailyChallengeQuestionDTO question = questionMapper.apply(challenge);
        // Started just before midnight and still answerable: keep the solution hidden until the clock runs out.
        if (attempt != null && attempt.getAnsweredAt() == null && !LocalDateTime.now(clock)
                .isAfter(attempt.getStartedAt().plusSeconds(TIME_LIMIT_SECONDS + GRACE_SECONDS))) {
            question = new DailyChallengeQuestionDTO(question.date(), question.gradeName(), question.topic(),
                    question.subTopic(), question.questionText(), null);
        }
        return new DailyChallengeRevealDTO(question, attempt != null,
                attempt == null ? null : attempt.getAnswer(), attempt != null && attempt.isCorrect());
    }

    /** Today's challenge of the scope, created on first access. */
    private DailyChallengeEntity challengeFor(String schoolCode, String gradeName, LocalDate date) {
        DailyChallengeEntity existing = challengeRepository
                .findBySchoolCodeAndGradeNameAndChallengeDate(schoolCode, gradeName, date).orElse(null);
        if (existing != null) return existing;

        String subTopic = pickSubTopic(schoolCode, gradeName, date);
        int level = CompetitionCatalog.levelForGrade(GradeLevels.level(gradeName));
        QuestionEntity generated = CompetitionCatalog.generate(subTopic, level);
        DailyChallengeEntity challenge = new DailyChallengeEntity();
        challenge.setSchoolCode(schoolCode);
        challenge.setGradeName(gradeName);
        challenge.setChallengeDate(date);
        challenge.setSubject(SkillCatalog.MATH);
        challenge.setTopic(CompetitionCatalog.topicOf(subTopic));
        challenge.setSubTopic(subTopic);
        challenge.setLevel(level);
        challenge.setQuestionText(generated.getQuestionText());
        challenge.setAnswer(generated.getAnswer());
        challenge.setCreatedAt(LocalDateTime.now(clock));
        try {
            return challengeRepository.save(challenge);
        } catch (DataIntegrityViolationException e) {
            return challengeRepository.findBySchoolCodeAndGradeNameAndChallengeDate(schoolCode, gradeName, date)
                    .orElseThrow();
        }
    }

    /** [schoolCode, gradeName], or null when the user is not a student of a school. */
    private String[] scope(String userId) {
        if (userId == null) return null;
        List<Object[]> rows = challengeRepository.findStudentScope(userId);
        if (rows.isEmpty() || rows.get(0)[0] == null) return null;
        String grade = rows.get(0)[1] == null ? NO_GRADE : ((String) rows.get(0)[1]).trim();
        return new String[]{(String) rows.get(0)[0], grade.isEmpty() ? NO_GRADE : grade};
    }

    private static <T> BasicResponse<T> ok(String message, T data) {
        BasicResponse<T> response = new BasicResponse<>(true, message);
        response.setData(data);
        return response;
    }

    private static <T> BasicResponse<T> fail(String message) {
        return new BasicResponse<>(false, message);
    }
}
