package org.example.learningprojectserver.dto;

import java.util.List;

/** API shapes of the story quests and their cosmetic rewards. The pending answer is never included. */
public final class QuestDTOs {

    private QuestDTOs() {}

    public static final String NOT_STARTED = "NOT_STARTED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";

    public record CosmeticItem(String code, String kind, String name, String emoji, String tone, String decor,
                               String questTitle, boolean unlocked, boolean equipped) {}

    public record QuestSummary(String code, String title, String emoji, String tagline, String gradeBand, String topic,
                               int chapterCount, int chaptersDone, String status, List<CosmeticItem> rewards) {}

    public record ChapterView(int number, String title, String story, String skill, int questionsToPass,
                              boolean done, boolean current) {}

    public record QuestState(String code, String title, String emoji, String intro, String outro, String status,
                             int chapterIndex, int chapterCount, List<ChapterView> chapters,
                             int chapterCorrect, int questionsToPass, int triesLeft, String question,
                             List<CosmeticItem> rewards) {}

    /** revealedAnswer is set only after the last wrong try, together with a fresh question. */
    public record AnswerResult(boolean correct, String message, String revealedAnswer, boolean chapterCompleted,
                               boolean questCompleted, List<CosmeticItem> unlocked, QuestState state) {}

    public record Cosmetics(CosmeticItem avatar, CosmeticItem room, List<CosmeticItem> items) {}
}
