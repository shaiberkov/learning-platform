package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.FocusDTOs.Bucket;
import org.example.learningprojectserver.dto.FocusDTOs.FocusStats;
import org.example.learningprojectserver.dto.FocusDTOs.Insights;
import org.example.learningprojectserver.dto.FocusDTOs.SessionRequest;
import org.example.learningprojectserver.entities.FocusSessionEntity;
import org.example.learningprojectserver.entities.StudentDailyActivityEntity;
import org.example.learningprojectserver.mappers.FocusSessionEntityToItemMapper;
import org.example.learningprojectserver.repository.FocusSessionRepository;
import org.example.learningprojectserver.repository.StudentDailyActivityRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Focus mode (a distraction-free timer whose sessions are recorded) and study-habit insights:
 * accuracy by hour of the day and by minutes into a test, computed from the student's exam answers
 * (test_answer_record), plus focus-session and daily-XP patterns. Read from MySQL on every request.
 */
@Service
@RequiredArgsConstructor
public class FocusService {

    /** Minimum answers in a bucket before it can back an insight. */
    static final int MIN_ANSWERS = 8;
    /** Hour-of-day lift (percent) below which no insight is shown. */
    static final int MIN_LIFT_PERCENT = 5;
    /** Accuracy drop (percentage points) that counts as "accuracy goes down". */
    static final int MIN_DROP_POINTS = 10;
    static final int HISTORY_DAYS = 120;
    static final int[] BREAK_THRESHOLDS = {20, 30, 40, 50, 60};
    private static final String[] HEBREW_DAYS = {"שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת", "ראשון"};

    private final FocusSessionRepository focusSessionRepository;
    private final StudentDailyActivityRepository dailyActivityRepository;
    private final FocusSessionEntityToItemMapper sessionMapper;

    /** One answered exam question: when, how far into its test (minutes), and whether it was right. */
    record AnswerRow(LocalDateTime at, Integer minutesIntoTest, boolean correct) {}

    public BasicResponse<Void> recordSession(String userId, SessionRequest request) {
        if (request.actualMinutes() < 1) {
            return new BasicResponse<>(false, "מפגש קצר מדקה לא נשמר");
        }
        LocalDateTime now = LocalDateTime.now(EngagementService.ZONE);
        FocusSessionEntity session = new FocusSessionEntity();
        session.setStudentUserId(userId);
        session.setTask(request.task() == null || request.task().isBlank() ? null : request.task().trim());
        session.setPlannedMinutes(request.plannedMinutes());
        session.setActualMinutes(Math.min(request.actualMinutes(), request.plannedMinutes()));
        session.setCompleted(request.completed());
        session.setEndedAt(now);
        session.setStartedAt(now.minusMinutes(session.getActualMinutes()));
        focusSessionRepository.save(session);
        return new BasicResponse<>(true, request.completed() ? "כל הכבוד! סיימת מפגש ריכוז" : "המפגש נשמר");
    }

    public BasicResponse<Insights> getInsights(String userId) {
        LocalDateTime now = LocalDateTime.now(EngagementService.ZONE);
        List<AnswerRow> answers = toAnswerRows(focusSessionRepository.answerTimings(userId, now.minusDays(HISTORY_DAYS)));
        List<FocusSessionEntity> sessions =
                focusSessionRepository.findByStudentUserIdAndStartedAtAfterOrderByStartedAtDesc(userId, now.minusDays(HISTORY_DAYS));
        LocalDate today = now.toLocalDate();
        List<StudentDailyActivityEntity> activity =
                dailyActivityRepository.findByStudentUserIdAndActivityDateBetweenOrderByActivityDate(userId, today.minusWeeks(8), today);
        long totalSessions = focusSessionRepository.countByStudentUserId(userId);

        BasicResponse<Insights> response = new BasicResponse<>(true, null);
        response.setData(buildInsights(answers, sessions, activity, totalSessions, now));
        return response;
    }

    Insights buildInsights(List<AnswerRow> answers, List<FocusSessionEntity> sessions,
                           List<StudentDailyActivityEntity> activity, long totalSessions, LocalDateTime now) {
        List<String> insights = new ArrayList<>();

        String hourInsight = bestHoursInsight(answers);
        if (hourInsight != null) insights.add(hourInsight);

        Integer breakAfter = accuracyDropMinute(answers);
        if (breakAfter != null) {
            insights.add("אחרי " + breakAfter + " דקות הדיוק שלך יורד, כדאי לעשות הפסקה");
        }

        FocusStats stats = focusStats(sessions, totalSessions, now);
        if (stats.sessionsThisWeek() > 0) {
            insights.add("השבוע למדת " + stats.minutesThisWeek() + " דקות במצב ריכוז ב-" + stats.sessionsThisWeek() + " מפגשים");
        }
        if (sessions.size() >= 4 && stats.completionPercent() < 50) {
            insights.add("רוב מפגשי הריכוז שלך נעצרים לפני הסוף. נסה מפגשים קצרים יותר, למשל 25 דקות");
        }

        String dayInsight = bestWeekdayInsight(activity);
        if (dayInsight != null) insights.add(dayInsight);

        return new Insights(insights, answers.size(), hourBuckets(answers), minuteBuckets(answers), breakAfter, stats,
                sessions.stream().limit(5).map(sessionMapper).toList());
    }

    /** Rows: testId, createdAt, secondsSpent, correct, ordered by test and answer order. */
    static List<AnswerRow> toAnswerRows(List<Object[]> rows) {
        List<AnswerRow> result = new ArrayList<>();
        Object currentTest = null;
        int secondsBefore = 0;
        boolean timingKnown = true;
        for (Object[] row : rows) {
            if (!Objects.equals(row[0], currentTest)) {
                currentTest = row[0];
                secondsBefore = 0;
                timingKnown = true;
            }
            Integer seconds = row[2] == null ? null : ((Number) row[2]).intValue();
            Integer minutesInto = timingKnown && seconds != null ? secondsBefore / 60 : null;
            if (seconds == null) {
                timingKnown = false;
            } else {
                secondsBefore += seconds;
            }
            result.add(new AnswerRow((LocalDateTime) row[1], minutesInto, Boolean.TRUE.equals(row[3])));
        }
        return result;
    }

    /** "You do X% better between HH:00 and HH+2:00", over sliding two-hour windows. */
    static String bestHoursInsight(List<AnswerRow> answers) {
        int bestStart = -1;
        double bestAccuracy = -1;
        int bestCount = 0;
        for (int start = 0; start < 24; start++) {
            int[] in = count(answers, start, true);
            if (in[1] < MIN_ANSWERS) continue;
            // On equal accuracy, the window with more answers is the better-supported one.
            if (accuracy(in) > bestAccuracy || (accuracy(in) == bestAccuracy && in[1] > bestCount)) {
                bestAccuracy = accuracy(in);
                bestCount = in[1];
                bestStart = start;
            }
        }
        if (bestStart < 0) return null;
        int[] rest = count(answers, bestStart, false);
        if (rest[1] < MIN_ANSWERS || rest[0] == 0) return null;
        long lift = Math.round((bestAccuracy - accuracy(rest)) / accuracy(rest) * 100);
        if (lift < MIN_LIFT_PERCENT) return null;
        return String.format("אתה מצליח %d%% יותר כשאתה לומד בין %02d:00 ל-%02d:00", lift, bestStart, (bestStart + 2) % 24);
    }

    /**
     * The threshold (minutes into a test) that splits the answers into a clearly more accurate "before"
     * and a less accurate "after", choosing the sharpest drop; null when no split drops enough.
     */
    static Integer accuracyDropMinute(List<AnswerRow> answers) {
        Integer best = null;
        double bestDrop = MIN_DROP_POINTS / 100.0;
        for (int threshold : BREAK_THRESHOLDS) {
            int[] before = {0, 0};
            int[] after = {0, 0};
            for (AnswerRow a : answers) {
                if (a.minutesIntoTest() == null) continue;
                int[] target = a.minutesIntoTest() < threshold ? before : after;
                target[1]++;
                if (a.correct()) target[0]++;
            }
            if (before[1] < MIN_ANSWERS || after[1] < MIN_ANSWERS) continue;
            double drop = accuracy(before) - accuracy(after);
            if (drop >= bestDrop + (best == null ? 0 : 1e-9)) {
                bestDrop = drop;
                best = threshold;
            }
        }
        return best;
    }

    static List<Bucket> hourBuckets(List<AnswerRow> answers) {
        List<Bucket> buckets = new ArrayList<>();
        for (int start = 6; start < 24; start += 2) {
            int[] c = count(answers, start, true);
            buckets.add(new Bucket(String.format("%02d-%02d", start, (start + 2) % 24), c[1], percent(c)));
        }
        return buckets;
    }

    static List<Bucket> minuteBuckets(List<AnswerRow> answers) {
        List<Bucket> buckets = new ArrayList<>();
        for (int from = 0; from <= 60; from += 10) {
            int[] c = {0, 0};
            for (AnswerRow a : answers) {
                Integer m = a.minutesIntoTest();
                if (m == null) continue;
                boolean inBucket = from == 60 ? m >= 60 : m >= from && m < from + 10;
                if (inBucket) {
                    c[1]++;
                    if (a.correct()) c[0]++;
                }
            }
            buckets.add(new Bucket(from == 60 ? "60+" : from + "-" + (from + 10), c[1], percent(c)));
        }
        return buckets;
    }

    static FocusStats focusStats(List<FocusSessionEntity> sessions, long totalSessions, LocalDateTime now) {
        LocalDateTime weekAgo = now.minusDays(7);
        int weekSessions = 0;
        int weekMinutes = 0;
        int completed = 0;
        for (FocusSessionEntity s : sessions) {
            if (s.isCompleted()) completed++;
            if (s.getStartedAt().isAfter(weekAgo)) {
                weekSessions++;
                weekMinutes += s.getActualMinutes();
            }
        }
        int completion = sessions.isEmpty() ? 0 : Math.round(completed * 100f / sessions.size());
        return new FocusStats(weekSessions, weekMinutes, totalSessions, completion);
    }

    static String bestWeekdayInsight(List<StudentDailyActivityEntity> activity) {
        Map<DayOfWeek, int[]> byDay = new EnumMap<>(DayOfWeek.class);
        int activeDays = 0;
        for (StudentDailyActivityEntity day : activity) {
            if (day.getXp() <= 0) continue;
            activeDays++;
            int[] sum = byDay.computeIfAbsent(day.getActivityDate().getDayOfWeek(), d -> new int[2]);
            sum[0] += day.getXp();
            sum[1]++;
        }
        if (activeDays < 4 || byDay.size() < 2) return null;
        DayOfWeek best = null;
        double bestAverage = -1;
        for (Map.Entry<DayOfWeek, int[]> e : byDay.entrySet()) {
            double average = (double) e.getValue()[0] / e.getValue()[1];
            if (average > bestAverage) {
                bestAverage = average;
                best = e.getKey();
            }
        }
        return "היום הכי פורה שלך הוא יום " + HEBREW_DAYS[best.getValue() - 1];
    }

    /** {correct, total} of the answers inside (inside=true) or outside the window [start, start+2). */
    private static int[] count(List<AnswerRow> answers, int start, boolean inside) {
        int[] c = {0, 0};
        for (AnswerRow a : answers) {
            int hour = a.at().getHour();
            boolean in = hour == start || hour == (start + 1) % 24;
            if (in == inside) {
                c[1]++;
                if (a.correct()) c[0]++;
            }
        }
        return c;
    }

    private static double accuracy(int[] c) {
        return c[1] == 0 ? 0 : (double) c[0] / c[1];
    }

    private static int percent(int[] c) {
        return (int) Math.round(accuracy(c) * 100);
    }
}
