package org.example.learningprojectserver.dto;

import java.util.List;

/** API shapes of the shared study rooms. */
public final class StudyRoomDTOs {
    private StudyRoomDTOs() {}

    /** A room in the school's list. phase is FOCUS or BREAK. */
    public record RoomSummary(Long id, String name, int memberCount, String phase) {}

    /** Who is in the room and what they are working on. */
    public record Member(String name, String task, boolean you) {}

    public record ChatMessage(String name, String text, long sentAtMs, boolean you) {}

    /**
     * The room as one member sees it. Clients run the countdown from timerStartedAtMs and their
     * clock offset to serverNowMs, so every member sees the same phase.
     */
    public record RoomState(Long id, String name, long serverNowMs, long timerStartedAtMs,
                            int focusMinutes, int breakMinutes, String phase, long secondsLeft,
                            boolean chatOpen, List<Member> members, List<ChatMessage> chat) {}

    /** Phase of the shared Pomodoro timer at a moment. */
    public record Phase(String phase, long secondsLeft, long phaseStartMs) {}
}
