package org.example.learningprojectserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * API shapes of the personal portfolio. The public shapes (Public*) are what a share link shows:
 * no user id, username, school, class or promotion outcome.
 */
public final class PortfolioDTOs {

    private PortfolioDTOs() {}

    public record ProjectRequest(
            @NotBlank(message = "נא לכתוב שם לפרויקט")
            @Size(max = 120, message = "שם הפרויקט ארוך מדי (עד 120 תווים)") String title,
            @Size(max = 1000, message = "התיאור ארוך מדי (עד 1000 תווים)") String description) {}

    public record Project(Long id, String title, String description, String gradeName, LocalDateTime createdAt) {}

    /** One school year; current marks the year in progress (not archived yet). */
    public record YearEntry(String yearName, String gradeName, String className, Integer testsCount,
                            Double averageScore, String outcome, boolean current) {}

    public record QuestTrophy(String code, String title, String emoji, LocalDateTime completedAt) {}

    public record Share(String token, String displayName, LocalDateTime createdAt) {}

    public record Portfolio(String studentName, String schoolName, String gradeName, String className,
                            int totalXp, int level, int longestStreak, String avatarEmoji, String avatarName,
                            List<EngagementSummaryDTO.Badge> badges, List<YearEntry> years,
                            List<Project> projects, List<QuestTrophy> quests, Share share) {}

    public record PublicYear(String yearName, String gradeName, Integer testsCount, Double averageScore,
                             boolean current) {}

    public record PublicProject(String title, String description, String gradeName, LocalDate createdOn) {}

    public record PublicBadge(String title, String description, LocalDate earnedOn) {}

    public record PublicPortfolio(String displayName, String gradeName, int level, int totalXp, String avatarEmoji,
                                  List<PublicBadge> badges, List<PublicYear> years, List<PublicProject> projects,
                                  List<QuestTrophy> quests, LocalDate sharedOn) {}
}
