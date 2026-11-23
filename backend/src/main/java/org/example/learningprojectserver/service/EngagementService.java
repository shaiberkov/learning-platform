package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.ClassChallengeDTO;
import org.example.learningprojectserver.dto.ClassChallengeRequest;
import org.example.learningprojectserver.dto.EngagementSummaryDTO;
import org.example.learningprojectserver.entities.*;
import org.example.learningprojectserver.mappers.StudentBadgeEntityToBadgeMapper;
import org.example.learningprojectserver.repository.*;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Gamification: XP, daily streak, badges for real achievements and class-wide challenges.
 * Everything here is personal or class-wide; there is no ranking of students against each other.
 */
@Service
@RequiredArgsConstructor
public class EngagementService {

    public static final ZoneId ZONE = ZoneId.of("Asia/Jerusalem");
    public static final int XP_PER_LEVEL = 200;
    public static final int MASTERY_LEVEL = 5;

    private static final int[] STREAK_BADGES = {3, 7, 30};
    private static final int[] XP_BADGES = {500, 2000, 5000};

    private final StudentEngagementRepository engagementRepository;
    private final StudentDailyActivityRepository dailyActivityRepository;
    private final StudentBadgeRepository badgeRepository;
    private final ClassChallengeRepository classChallengeRepository;
    private final StudentAccommodationRepository studentLookupRepository;
    private final ClassRoomRepository classRoomRepository;
    private final TeacherRepository teacherRepository;
    private final StudentBadgeEntityToBadgeMapper badgeMapper;

    /** A badge the caller already knows the student earned (mastery, perfect test...). */
    public record BadgeGrant(String code, String title, String description) {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /** Streak after an activity today, given the last active day and the streak before it. */
    public static int nextStreak(LocalDate lastActive, LocalDate today, int currentStreak) {
        if (lastActive == null) return 1;
        if (lastActive.equals(today)) return Math.max(currentStreak, 1);
        if (lastActive.plusDays(1).equals(today)) return currentStreak + 1;
        return 1;
    }

    /** Streak to show today: still alive if the student was active today or yesterday. */
    public static int visibleStreak(LocalDate lastActive, LocalDate today, int currentStreak) {
        if (lastActive == null) return 0;
        return lastActive.isBefore(today.minusDays(1)) ? 0 : currentStreak;
    }

    /** Encouraging, personal-only comparison of this week to last week. */
    public static String progressMessage(int thisWeek, int lastWeek) {
        if (thisWeek == 0 && lastWeek == 0) {
            return "זה זמן מצוין להתחיל – כל תרגול קטן נחשב.";
        }
        if (thisWeek > lastWeek) {
            return "השבוע צברת " + thisWeek + " נקודות – " + (thisWeek - lastWeek) + " יותר מהשבוע שעבר. כל הכבוד!";
        }
        if (thisWeek == lastWeek) {
            return "אותו קצב כמו בשבוע שעבר – המשך כך!";
        }
        return "השבוע צברת " + thisWeek + " נקודות. עוד " + (lastWeek - thisWeek) + " והשתווית לשבוע שעבר – אפשר!";
    }

    /**
     * Adds XP, moves the streak and awards badges. Runs in its own transaction so that a failure
     * here never rolls back the student's answer, test or homework (see EngagementEvents).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordActivity(String studentUserId, int xp, List<BadgeGrant> extraBadges) {
        LocalDate today = today();
        StudentEngagementEntity e = engagementRepository.findByStudentUserId(studentUserId)
                .orElseGet(() -> new StudentEngagementEntity(studentUserId));
        boolean firstActivity = e.getLastActiveDate() == null;

        int streak = nextStreak(e.getLastActiveDate(), today, e.getCurrentStreak());
        e.setCurrentStreak(streak);
        e.setLongestStreak(Math.max(e.getLongestStreak(), streak));
        e.setLastActiveDate(today);
        e.setTotalXp(e.getTotalXp() + Math.max(xp, 0));
        engagementRepository.save(e);

        StudentDailyActivityEntity day = dailyActivityRepository
                .findFirstByStudentUserIdAndActivityDate(studentUserId, today)
                .orElseGet(() -> new StudentDailyActivityEntity(studentUserId, today));
        day.setXp(day.getXp() + Math.max(xp, 0));
        dailyActivityRepository.save(day);

        if (firstActivity) {
            grant(studentUserId, new BadgeGrant("FIRST_STEPS", "צעד ראשון", "השלמת את הפעילות הראשונה שלך"));
        }
        for (int days : STREAK_BADGES) {
            if (streak >= days) {
                grant(studentUserId, new BadgeGrant("STREAK_" + days, "רצף של " + days + " ימים",
                        "למדת " + days + " ימים ברצף"));
            }
        }
        for (int points : XP_BADGES) {
            if (e.getTotalXp() >= points) {
                grant(studentUserId, new BadgeGrant("XP_" + points, points + " נקודות ניסיון",
                        "צברת " + points + " נקודות ניסיון"));
            }
        }
        if (extraBadges != null) {
            extraBadges.forEach(b -> grant(studentUserId, b));
        }
    }

    private void grant(String studentUserId, BadgeGrant b) {
        if (badgeRepository.existsByStudentUserIdAndCode(studentUserId, b.code())) return;
        StudentBadgeEntity badge = new StudentBadgeEntity();
        badge.setStudentUserId(studentUserId);
        badge.setCode(b.code());
        badge.setTitle(b.title());
        badge.setDescription(b.description());
        badge.setEarnedAt(LocalDateTime.now(ZONE));
        badgeRepository.save(badge);
    }

    @Transactional(readOnly = true)
    public BasicResponse getSummary(String studentId) {
        LocalDate today = today();
        StudentEngagementEntity e = engagementRepository.findByStudentUserId(studentId)
                .orElseGet(() -> new StudentEngagementEntity(studentId));

        List<StudentDailyActivityEntity> days = dailyActivityRepository
                .findByStudentUserIdAndActivityDateBetweenOrderByActivityDate(studentId, today.minusDays(13), today);
        Map<LocalDate, Integer> xpByDay = new HashMap<>();
        days.forEach(d -> xpByDay.merge(d.getActivityDate(), d.getXp(), Integer::sum));

        int thisWeek = 0;
        int lastWeek = 0;
        List<EngagementSummaryDTO.DailyXp> lastDays = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            int xp = xpByDay.getOrDefault(d, 0);
            if (i <= 6) {
                thisWeek += xp;
                lastDays.add(new EngagementSummaryDTO.DailyXp(d, xp));
            } else {
                lastWeek += xp;
            }
        }

        List<EngagementSummaryDTO.Badge> badges = badgeRepository.findByStudentUserIdOrderByEarnedAtDesc(studentId)
                .stream().map(badgeMapper).toList();

        Long classRoomId = studentLookupRepository.findClassRoomIdOfStudent(studentId);
        List<ClassChallengeDTO> challenges = classRoomId == null ? List.of()
                : classChallengeRepository.findByClassRoomIdOrderByEndDateDesc(classRoomId)
                .stream().map(c -> toChallengeDTO(c, today)).toList();

        EngagementSummaryDTO dto = new EngagementSummaryDTO(
                e.getTotalXp(),
                e.getTotalXp() / XP_PER_LEVEL + 1,
                e.getTotalXp() % XP_PER_LEVEL,
                XP_PER_LEVEL,
                visibleStreak(e.getLastActiveDate(), today, e.getCurrentStreak()),
                e.getLongestStreak(),
                today.equals(e.getLastActiveDate()),
                thisWeek,
                lastWeek,
                progressMessage(thisWeek, lastWeek),
                lastDays,
                badges,
                challenges);
        BasicResponse<EngagementSummaryDTO> response = new BasicResponse<>(true, null);
        response.setData(dto);
        return response;
    }

    @Transactional
    public BasicResponse createClassChallenge(String teacherId, ClassChallengeRequest request) {
        TeacherEntity teacher = teacherRepository.findByUserId(teacherId);
        if (teacher == null) {
            return new BasicResponse(false, "המורה לא נמצא");
        }
        if (request == null || request.title() == null || request.title().isBlank()) {
            return new BasicResponse(false, "יש להזין שם לאתגר");
        }
        if (request.targetXp() <= 0) {
            return new BasicResponse(false, "יעד הנקודות חייב להיות גדול מאפס");
        }
        boolean teachesClass = teacher.getTeachingClassRooms().stream()
                .anyMatch(c -> c.getId().equals(request.classRoomId()));
        if (!teachesClass) {
            return new BasicResponse(false, "ניתן ליצור אתגר רק לכיתה שאתה מלמד");
        }
        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(request.startDate());
            end = LocalDate.parse(request.endDate());
        } catch (DateTimeParseException | NullPointerException ex) {
            return new BasicResponse(false, "תאריכי האתגר אינם תקינים");
        }
        if (end.isBefore(start)) {
            return new BasicResponse(false, "תאריך הסיום חייב להיות אחרי תאריך ההתחלה");
        }

        ClassChallengeEntity c = new ClassChallengeEntity();
        c.setTeacherUserId(teacherId);
        c.setClassRoomId(request.classRoomId());
        c.setTitle(request.title().trim());
        c.setTargetXp(request.targetXp());
        c.setStartDate(start);
        c.setEndDate(end);
        classChallengeRepository.save(c);

        BasicResponse<ClassChallengeDTO> response = new BasicResponse<>(true, "האתגר נוצר בהצלחה");
        response.setData(toChallengeDTO(c, today()));
        return response;
    }

    @Transactional(readOnly = true)
    public BasicResponse getTeacherChallenges(String teacherId) {
        LocalDate today = today();
        List<ClassChallengeDTO> list = classChallengeRepository.findByTeacherUserIdOrderByEndDateDesc(teacherId)
                .stream().map(c -> toChallengeDTO(c, today)).toList();
        BasicResponse<List<ClassChallengeDTO>> response = new BasicResponse<>(true, null);
        response.setData(list);
        return response;
    }

    private ClassChallengeDTO toChallengeDTO(ClassChallengeEntity c, LocalDate today) {
        List<String> students = studentLookupRepository.findStudentUserIdsInClass(c.getClassRoomId());
        LocalDate to = today.isBefore(c.getEndDate()) ? today : c.getEndDate();
        long classXp = students.isEmpty() || to.isBefore(c.getStartDate()) ? 0
                : dailyActivityRepository.sumXpForStudentsBetween(students, c.getStartDate(), to);
        int percent = c.getTargetXp() <= 0 ? 0 : (int) Math.min(100, classXp * 100 / c.getTargetXp());
        String status = today.isBefore(c.getStartDate()) ? "UPCOMING"
                : today.isAfter(c.getEndDate()) ? "ENDED" : "ACTIVE";
        String className = classRoomRepository.findById(c.getClassRoomId())
                .map(ClassRoomEntity::getName).orElse("");
        return new ClassChallengeDTO(c.getId(), c.getTitle(), c.getClassRoomId(), className, c.getTargetXp(),
                classXp, percent, c.getStartDate(), c.getEndDate(), status);
    }
}
