package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.StudyRoomDTOs.ChatMessage;
import org.example.learningprojectserver.dto.StudyRoomDTOs.Member;
import org.example.learningprojectserver.dto.StudyRoomDTOs.Phase;
import org.example.learningprojectserver.dto.StudyRoomDTOs.RoomState;
import org.example.learningprojectserver.dto.StudyRoomDTOs.RoomSummary;
import org.example.learningprojectserver.entities.StudentEntity;
import org.example.learningprojectserver.entities.StudyRoomEntity;
import org.example.learningprojectserver.repository.StudentRepository;
import org.example.learningprojectserver.repository.StudyRoomRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Shared study rooms: students of one school study together on a shared Pomodoro timer
 * (25 minutes focus, 5 minutes break), see who is in the room and what each one works on,
 * and chat only during the break.
 *
 * The rooms are rows in study_room. Presence (heartbeats) and the break chat live in memory in this
 * bean: they are seconds-to-minutes state that nobody expects to keep, lost on restart and per process
 * (the server runs as a single instance, see docs/CACHING.md).
 */
@Service
@RequiredArgsConstructor
public class StudyRoomService {

    public static final int FOCUS_MINUTES = 25;
    public static final int BREAK_MINUTES = 5;
    public static final String FOCUS = "FOCUS";
    public static final String BREAK = "BREAK";

    /** A member who has not sent a heartbeat for this long has left. */
    static final long PRESENCE_TIMEOUT_MS = 15_000;
    /** A room nobody has been in for this long is closed. */
    static final long IDLE_ROOM_MINUTES = 120;
    static final int MAX_MEMBERS = 12;
    static final int MAX_ROOMS_PER_SCHOOL = 30;
    static final int MAX_CHAT_MESSAGES = 100;
    static final int MAX_CHAT_LENGTH = 300;
    private static final long LAST_ACTIVITY_WRITE_EVERY_MINUTES = 5;

    private final StudyRoomRepository roomRepository;
    private final StudentRepository studentRepository;

    private record Presence(String name, String task, long lastSeenMs) {}

    private record StoredChat(String userId, String name, String text, long sentAtMs) {}

    private final Map<Long, Map<String, Presence>> presence = new ConcurrentHashMap<>();
    private final Map<Long, Deque<StoredChat>> chats = new ConcurrentHashMap<>();

    /** Phase of a timer that started at startMs, at nowMs. Pure, shared by the server and the tests. */
    public static Phase phaseAt(long startMs, long nowMs) {
        long cycleMs = (FOCUS_MINUTES + BREAK_MINUTES) * 60_000L;
        long focusMs = FOCUS_MINUTES * 60_000L;
        long elapsed = Math.max(0, nowMs - startMs);
        long inCycle = elapsed % cycleMs;
        long cycleStart = nowMs - inCycle;
        if (inCycle < focusMs) {
            return new Phase(FOCUS, ceilSeconds(focusMs - inCycle), cycleStart);
        }
        return new Phase(BREAK, ceilSeconds(cycleMs - inCycle), cycleStart + focusMs);
    }

    private static long ceilSeconds(long ms) {
        return (ms + 999) / 1000;
    }

    public BasicResponse<List<RoomSummary>> listRooms(String userId) {
        String schoolCode = studentRepository.findSchoolCodeByUserId(userId);
        if (schoolCode == null) {
            return fail("חדרי הלמידה זמינים לתלמידים המשויכים לבית ספר");
        }
        long now = nowMs();
        List<RoomSummary> result = new ArrayList<>();
        for (StudyRoomEntity room : roomRepository.findBySchoolCodeAndActiveTrueOrderByCreatedAtDesc(schoolCode)) {
            int members = activeMembers(room.getId(), now).size();
            if (members == 0 && room.getLastActivityAt().isBefore(now().minusMinutes(IDLE_ROOM_MINUTES))) {
                room.setActive(false);
                roomRepository.save(room);
                forget(room.getId());
                continue;
            }
            result.add(new RoomSummary(room.getId(), room.getName(), members,
                    phaseAt(toMs(room.getTimerStartedAt()), now).phase()));
        }
        return ok(null, result);
    }

    public BasicResponse<RoomState> createRoom(String userId, String name) {
        String schoolCode = studentRepository.findSchoolCodeByUserId(userId);
        if (schoolCode == null) {
            return fail("חדרי הלמידה זמינים לתלמידים המשויכים לבית ספר");
        }
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 60) {
            return fail("שם החדר צריך להיות באורך 2 עד 60 תווים");
        }
        if (roomRepository.findBySchoolCodeAndActiveTrueOrderByCreatedAtDesc(schoolCode).size() >= MAX_ROOMS_PER_SCHOOL) {
            return fail("יש כבר הרבה חדרים פתוחים בבית הספר. אפשר להצטרף לאחד מהם");
        }
        LocalDateTime now = now();
        StudyRoomEntity room = new StudyRoomEntity();
        room.setSchoolCode(schoolCode);
        room.setName(trimmed);
        room.setCreatedByUserId(userId);
        room.setCreatedAt(now);
        room.setTimerStartedAt(now);
        room.setLastActivityAt(now);
        room.setActive(true);
        StudyRoomEntity saved = roomRepository.save(room);
        return heartbeat(userId, saved.getId(), null);
    }

    /** Join or stay in a room (the client calls it every few seconds) and get the room's state. */
    public BasicResponse<RoomState> heartbeat(String userId, Long roomId, String task) {
        StudyRoomEntity room = roomOfStudentSchool(userId, roomId);
        if (room == null) {
            return fail("החדר לא נמצא או שנסגר");
        }
        long now = nowMs();
        Map<String, Presence> members = presence.computeIfAbsent(roomId, id -> new ConcurrentHashMap<>());
        Presence current = members.get(userId);
        if ((current == null || now - current.lastSeenMs() > PRESENCE_TIMEOUT_MS)
                && activeMembers(roomId, now).size() >= MAX_MEMBERS) {
            return fail("החדר מלא. אפשר לפתוח חדר חדש");
        }
        String name = current != null ? current.name() : displayName(userId);
        members.put(userId, new Presence(name, cleanTask(task), now));
        touch(room);
        return ok(null, state(room, userId, now));
    }

    public BasicResponse<Void> leave(String userId, Long roomId) {
        Map<String, Presence> members = presence.get(roomId);
        if (members != null) {
            members.remove(userId);
        }
        return ok("יצאת מהחדר", null);
    }

    public BasicResponse<RoomState> sendChat(String userId, Long roomId, String text) {
        StudyRoomEntity room = roomOfStudentSchool(userId, roomId);
        if (room == null) {
            return fail("החדר לא נמצא או שנסגר");
        }
        long now = nowMs();
        Presence me = activeMembers(roomId, now).get(userId);
        if (me == null) {
            return fail("צריך להיות בחדר כדי לכתוב בצ'אט");
        }
        if (!BREAK.equals(phaseAt(toMs(room.getTimerStartedAt()), now).phase())) {
            return fail("הצ'אט נפתח רק בהפסקה. בינתיים מתרכזים!");
        }
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_CHAT_LENGTH) {
            return fail("הודעה צריכה להיות באורך 1 עד " + MAX_CHAT_LENGTH + " תווים");
        }
        Deque<StoredChat> log = chats.computeIfAbsent(roomId, id -> new ConcurrentLinkedDeque<>());
        log.addLast(new StoredChat(userId, me.name(), trimmed, now));
        while (log.size() > MAX_CHAT_MESSAGES) {
            log.pollFirst();
        }
        return ok(null, state(room, userId, now));
    }

    private RoomState state(StudyRoomEntity room, String userId, long now) {
        long startMs = toMs(room.getTimerStartedAt());
        Phase phase = phaseAt(startMs, now);
        boolean chatOpen = BREAK.equals(phase.phase());
        List<Member> members = activeMembers(room.getId(), now).entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getValue().name()))
                .map(e -> new Member(e.getValue().name(), e.getValue().task(), e.getKey().equals(userId)))
                .toList();
        List<ChatMessage> chat = List.of();
        if (chatOpen) {
            // Only the messages of this break: every break starts with an empty chat.
            chat = chats.getOrDefault(room.getId(), new ConcurrentLinkedDeque<>()).stream()
                    .filter(m -> m.sentAtMs() >= phase.phaseStartMs())
                    .map(m -> new ChatMessage(m.name(), m.text(), m.sentAtMs(), m.userId().equals(userId)))
                    .toList();
        }
        return new RoomState(room.getId(), room.getName(), now, startMs, FOCUS_MINUTES, BREAK_MINUTES,
                phase.phase(), phase.secondsLeft(), chatOpen, members, chat);
    }

    /** Members seen within the presence timeout; drops the others. */
    private Map<String, Presence> activeMembers(Long roomId, long now) {
        Map<String, Presence> members = presence.get(roomId);
        if (members == null) {
            return Map.of();
        }
        members.values().removeIf(p -> now - p.lastSeenMs() > PRESENCE_TIMEOUT_MS);
        return members;
    }

    private StudyRoomEntity roomOfStudentSchool(String userId, Long roomId) {
        if (roomId == null) {
            return null;
        }
        StudyRoomEntity room = roomRepository.findById(roomId).orElse(null);
        if (room == null || !room.isActive()) {
            return null;
        }
        String schoolCode = studentRepository.findSchoolCodeByUserId(userId);
        return room.getSchoolCode().equals(schoolCode) ? room : null;
    }

    private void touch(StudyRoomEntity room) {
        LocalDateTime now = now();
        if (room.getLastActivityAt().isBefore(now.minusMinutes(LAST_ACTIVITY_WRITE_EVERY_MINUTES))) {
            room.setLastActivityAt(now);
            roomRepository.save(room);
        }
    }

    private void forget(Long roomId) {
        presence.remove(roomId);
        chats.remove(roomId);
    }

    private String displayName(String userId) {
        StudentEntity student = studentRepository.findStudentByStudentId(userId);
        return student == null ? "תלמיד/ה" : student.getUsername();
    }

    private static String cleanTask(String task) {
        if (task == null || task.isBlank()) {
            return null;
        }
        String trimmed = task.trim();
        return trimmed.length() > 60 ? trimmed.substring(0, 60) : trimmed;
    }

    static LocalDateTime now() {
        return LocalDateTime.now(EngagementService.ZONE);
    }

    private static long nowMs() {
        return System.currentTimeMillis();
    }

    private static long toMs(LocalDateTime time) {
        return time.atZone(EngagementService.ZONE).toInstant().toEpochMilli();
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
