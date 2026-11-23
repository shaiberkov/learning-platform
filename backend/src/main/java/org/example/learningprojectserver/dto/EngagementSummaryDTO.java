package org.example.learningprojectserver.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A student's own progress: XP, streak, badges, personal week-over-week change and the shared
 * challenges of their class. Deliberately holds nothing about other students.
 */
public record EngagementSummaryDTO(int totalXp,
                                   int level,
                                   int xpIntoLevel,
                                   int xpPerLevel,
                                   int currentStreak,
                                   int longestStreak,
                                   boolean activeToday,
                                   int xpThisWeek,
                                   int xpLastWeek,
                                   String progressMessage,
                                   List<DailyXp> lastDays,
                                   List<Badge> badges,
                                   List<ClassChallengeDTO> classChallenges) {

    public record DailyXp(LocalDate date, int xp) {
    }

    public record Badge(String code, String title, String description, LocalDateTime earnedAt) {
    }
}
