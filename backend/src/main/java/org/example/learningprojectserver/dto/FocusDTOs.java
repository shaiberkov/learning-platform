package org.example.learningprojectserver.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/** API shapes of focus mode and the study-habit insights. */
public final class FocusDTOs {
    private FocusDTOs() {}

    /** Body of "record a focus session", sent when the timer ends or the student stops. */
    public record SessionRequest(
            @Min(value = 1, message = "משך מתוכנן לא תקין") @Max(value = 240, message = "משך מתוכנן לא תקין") int plannedMinutes,
            @Min(value = 0, message = "משך לא תקין") @Max(value = 240, message = "משך לא תקין") int actualMinutes,
            @Size(max = 80, message = "תיאור המשימה ארוך מדי") String task,
            boolean completed) {}

    public record SessionItem(Long id, String task, int plannedMinutes, int actualMinutes, boolean completed, String startedAt) {}

    /** Accuracy in one time bucket (hours of the day, or minutes into a test). */
    public record Bucket(String label, int answers, int accuracyPercent) {}

    public record FocusStats(int sessionsThisWeek, int minutesThisWeek, long totalSessions, int completionPercent) {}

    public record Insights(List<String> insights,
                           int answersAnalyzed,
                           List<Bucket> byHour,
                           List<Bucket> bySessionMinute,
                           Integer suggestedBreakAfterMinutes,
                           FocusStats focus,
                           List<SessionItem> recentSessions) {}
}
