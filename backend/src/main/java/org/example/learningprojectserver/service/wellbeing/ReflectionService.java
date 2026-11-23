package org.example.learningprojectserver.service.wellbeing;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.MessageDTO;
import org.example.learningprojectserver.dto.WellbeingDTOs.Reflection;
import org.example.learningprojectserver.dto.WellbeingDTOs.ReflectionOverview;
import org.example.learningprojectserver.dto.WellbeingDTOs.ReflectionRequest;
import org.example.learningprojectserver.entities.StudentDailyActivityEntity;
import org.example.learningprojectserver.entities.StudentReflectionEntity;
import org.example.learningprojectserver.mappers.StudentReflectionEntityToDTOMapper;
import org.example.learningprojectserver.notification.NotificationType;
import org.example.learningprojectserver.notification.dto.NotificationDTO;
import org.example.learningprojectserver.notification.dto.SystemMessageDTO;
import org.example.learningprojectserver.notification.publisher.NotificationEventPublisher;
import org.example.learningprojectserver.repository.StudentDailyActivityRepository;
import org.example.learningprojectserver.repository.StudentReflectionRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.EngagementService;
import org.example.learningprojectserver.service.wellbeing.WellbeingPolicy.GoalStatus;
import org.example.learningprojectserver.service.wellbeing.WellbeingPolicy.GoalType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Weekly reflection journal ([s10]): three short questions every Sunday and a goal for the week.
 * Measurable goals are checked from the student's activity; a free goal is marked by the student.
 * Reflections are private to the student: no teacher or manager endpoint reads them.
 */
@Service
@RequiredArgsConstructor
public class ReflectionService {

    static final String REMINDER_TITLE = "יומן רפלקציה: שבוע חדש מתחיל";
    static final String REMINDER_BODY = "שלוש שאלות קצרות: מה הלך טוב השבוע? במה נתקעתי? מה היעד שלי לשבוע הבא?";
    static final String REMINDER_LINK = "/reflection";

    private final StudentReflectionRepository reflectionRepository;
    private final StudentDailyActivityRepository dailyActivityRepository;
    private final StudentReflectionEntityToDTOMapper reflectionMapper;
    private final NotificationEventPublisher notificationEventPublisher;

    public BasicResponse<ReflectionOverview> overview(String studentUserId) {
        LocalDate today = EngagementService.today();
        LocalDate week = WellbeingPolicy.weekStart(today);
        Reflection current = reflectionRepository.findByStudentUserIdAndWeekStart(studentUserId, week)
                .map(r -> withStatus(r, today)).orElse(null);
        Reflection previous = reflectionRepository.findByStudentUserIdAndWeekStart(studentUserId, week.minusWeeks(1))
                .map(r -> withStatus(r, today)).orElse(null);
        return ok(new ReflectionOverview(week, today.getDayOfWeek() == DayOfWeek.SUNDAY, current, previous));
    }

    public BasicResponse<List<Reflection>> history(String studentUserId) {
        LocalDate today = EngagementService.today();
        List<Reflection> list = reflectionRepository.findTop12ByStudentUserIdOrderByWeekStartDesc(studentUserId)
                .stream().map(r -> withStatus(r, today)).toList();
        return ok(list);
    }

    /** Writes (or rewrites) this week's reflection. */
    @Transactional
    public BasicResponse<Reflection> save(String studentUserId, ReflectionRequest request) {
        GoalType type = GoalType.parse(request.goalType());
        if (type == null) {
            return new BasicResponse<>(false, "סוג היעד אינו מוכר");
        }
        if (type.measurable() && request.goalTarget() == null) {
            return new BasicResponse<>(false, "ליעד מדיד צריך לבחור מספר");
        }
        LocalDate today = EngagementService.today();
        LocalDate week = WellbeingPolicy.weekStart(today);
        LocalDateTime now = LocalDateTime.now(EngagementService.ZONE);

        StudentReflectionEntity r = reflectionRepository.findByStudentUserIdAndWeekStart(studentUserId, week)
                .orElseGet(() -> {
                    StudentReflectionEntity created = new StudentReflectionEntity();
                    created.setStudentUserId(studentUserId);
                    created.setWeekStart(week);
                    created.setCreatedAt(now);
                    return created;
                });
        if (r.getGoalType() != null && !r.getGoalType().equals(type.name())) {
            r.setGoalProgress(0);
            r.setAchieved(null);
        }
        r.setWentWell(trimToNull(request.wentWell()));
        r.setStuckOn(trimToNull(request.stuckOn()));
        r.setNextGoal(request.nextGoal().trim());
        r.setGoalType(type.name());
        r.setGoalTarget(type.measurable() ? request.goalTarget() : null);
        if (r.getId() != null) r.setUpdatedAt(now);
        StudentReflectionEntity saved = reflectionRepository.save(r);
        BasicResponse<Reflection> response = new BasicResponse<>(true, "הרפלקציה נשמרה. בהצלחה השבוע!");
        response.setData(withStatus(saved, today));
        return response;
    }

    /** The student's own mark for a free goal; measurable goals are checked automatically. */
    @Transactional
    public BasicResponse<Reflection> markAchieved(String studentUserId, Long reflectionId, boolean achieved) {
        StudentReflectionEntity r = reflectionRepository.findByIdAndStudentUserId(reflectionId, studentUserId).orElse(null);
        if (r == null) {
            return new BasicResponse<>(false, "הרפלקציה לא נמצאה");
        }
        GoalType type = GoalType.parse(r.getGoalType());
        if (type != null && type.measurable()) {
            return new BasicResponse<>(false, "יעד מדיד נבדק אוטומטית לפי הפעילות שלך");
        }
        r.setAchieved(achieved);
        r.setUpdatedAt(LocalDateTime.now(EngagementService.ZONE));
        reflectionRepository.save(r);
        BasicResponse<Reflection> response = new BasicResponse<>(true, achieved ? "כל הכבוד! היעד סומן כהושג" : "הסימון עודכן");
        response.setData(withStatus(r, EngagementService.today()));
        return response;
    }

    /**
     * One practice answer toward this week's PRACTICE_QUESTIONS goal. Runs in its own transaction
     * so a failure here never rolls back the student's answer (the caller swallows it).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPracticeAnswer(String studentUserId) {
        reflectionRepository.incrementProgress(studentUserId, WellbeingPolicy.weekStart(EngagementService.today()),
                GoalType.PRACTICE_QUESTIONS.name());
    }

    public boolean hasThisWeeksReflection(String studentUserId) {
        LocalDate week = WellbeingPolicy.weekStart(EngagementService.today());
        return reflectionRepository.findByStudentUserIdAndWeekStart(studentUserId, week).isPresent();
    }

    /** Sunday morning: reminds every student in a school who has not reflected this week yet. */
    public int sendSundayReminders() {
        LocalDate week = WellbeingPolicy.weekStart(EngagementService.today());
        List<String> students = reflectionRepository.findStudentsWithoutReflection(week);
        if (students.isEmpty()) return 0;
        MessageDTO message = new MessageDTO(REMINDER_TITLE, REMINDER_BODY, LocalDateTime.now(EngagementService.ZONE), "יומן רפלקציה");
        NotificationDTO<SystemMessageDTO> dto = new NotificationDTO<>(NotificationType.SYSTEM_MESSAGE, new SystemMessageDTO(message));
        dto.setTitle(REMINDER_TITLE);
        dto.setBody(REMINDER_BODY);
        dto.setLink(REMINDER_LINK);
        notificationEventPublisher.publish(students, dto);
        return students.size();
    }

    /** Adds the measured progress and the goal status to the stored reflection. */
    Reflection withStatus(StudentReflectionEntity r, LocalDate today) {
        GoalType type = GoalType.parse(r.getGoalType());
        LocalDate weekEnd = r.getWeekStart().plusDays(6);
        boolean weekOver = today.isAfter(weekEnd);
        int progress = switch (type == null ? GoalType.FREE : type) {
            case PRACTICE_QUESTIONS -> r.getGoalProgress();
            case XP -> activity(r).stream().mapToInt(StudentDailyActivityEntity::getXp).sum();
            case ACTIVE_DAYS -> (int) activity(r).stream().filter(a -> a.getXp() > 0).count();
            case FREE -> 0;
        };
        GoalStatus status = WellbeingPolicy.goalStatus(type, r.getGoalTarget(), progress, r.getAchieved(), weekOver);
        return reflectionMapper.apply(r).withProgress(progress, status.name());
    }

    private List<StudentDailyActivityEntity> activity(StudentReflectionEntity r) {
        return dailyActivityRepository.findByStudentUserIdAndActivityDateBetweenOrderByActivityDate(
                r.getStudentUserId(), r.getWeekStart(), r.getWeekStart().plusDays(6));
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static <T> BasicResponse<T> ok(T data) {
        BasicResponse<T> response = new BasicResponse<>(true, null);
        response.setData(data);
        return response;
    }
}
