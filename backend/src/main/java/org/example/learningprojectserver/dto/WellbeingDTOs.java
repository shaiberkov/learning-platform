package org.example.learningprojectserver.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.learningprojectserver.service.wellbeing.WellbeingPolicy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Request and response shapes of the reflection journal and the pre-exam check-in ([s10]). */
public final class WellbeingDTOs {
    private WellbeingDTOs() {}

    /** status: WellbeingPolicy.GoalStatus; progress: practice answers, XP or active days so far. */
    public record Reflection(Long id,
                             LocalDate weekStart,
                             String wentWell,
                             String stuckOn,
                             String nextGoal,
                             String goalType,
                             Integer goalTarget,
                             int progress,
                             Boolean achievedMark,
                             String status) {
        public Reflection withProgress(int newProgress, String newStatus) {
            return new Reflection(id, weekStart, wentWell, stuckOn, nextGoal, goalType, goalTarget,
                    newProgress, achievedMark, newStatus);
        }
    }

    /** current: this week's reflection (goal in progress); previous: last week's, with its final result. */
    public record ReflectionOverview(LocalDate weekStart, boolean sunday, Reflection current, Reflection previous) {}

    public record ReflectionRequest(
            @Size(max = 2000, message = "התשובה ארוכה מדי (עד 2000 תווים)") String wentWell,
            @Size(max = 2000, message = "התשובה ארוכה מדי (עד 2000 תווים)") String stuckOn,
            @NotBlank(message = "יש לכתוב יעד לשבוע הבא")
            @Size(max = 500, message = "היעד ארוך מדי (עד 500 תווים)") String nextGoal,
            @NotBlank(message = "יש לבחור סוג יעד") String goalType,
            @Min(value = 1, message = "היעד צריך להיות לפחות 1")
            @Max(value = 5000, message = "היעד גדול מדי") Integer goalTarget) {}

    public record UpcomingExam(Long testId, String subject, String topic, String startTime, int daysLeft) {}

    /** What the dashboard card should offer right now. */
    public record Prompt(boolean reflectionDue, boolean sunday, UpcomingExam upcomingExam, boolean checkedInToday) {}

    public record Checkin(LocalDate date, String mood, String examLabel) {}

    public record Consent(boolean consented, LocalDateTime consentedAt, LocalDateTime revokedAt, String policyVersion) {}

    public record CheckinStatus(Consent consent, List<Checkin> recent, UpcomingExam upcomingExam,
                                int distressCount, int distressWindow) {}

    public record CheckinRequest(@NotBlank(message = "יש לבחור איך את/ה מרגיש/ה") String mood) {}

    /** alertSent: this check-in sent the (consented) alert to the school staff. */
    public record CheckinResult(Checkin checkin, WellbeingPolicy.Suggestion suggestion,
                                boolean distressPattern, boolean alertSent, boolean consented) {}
}
