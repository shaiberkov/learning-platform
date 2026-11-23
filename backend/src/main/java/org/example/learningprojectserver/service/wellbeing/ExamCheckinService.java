package org.example.learningprojectserver.service.wellbeing;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.MessageDTO;
import org.example.learningprojectserver.dto.StudentTestStatusDTO;
import org.example.learningprojectserver.dto.WellbeingDTOs.*;
import org.example.learningprojectserver.entities.StudentMoodCheckinEntity;
import org.example.learningprojectserver.entities.StudentWellbeingConsentEntity;
import org.example.learningprojectserver.mappers.StudentMoodCheckinEntityToDTOMapper;
import org.example.learningprojectserver.notification.NotificationType;
import org.example.learningprojectserver.notification.dto.NotificationDTO;
import org.example.learningprojectserver.notification.dto.SystemMessageDTO;
import org.example.learningprojectserver.notification.publisher.NotificationEventPublisher;
import org.example.learningprojectserver.repository.StudentMoodCheckinRepository;
import org.example.learningprojectserver.repository.StudentRepository;
import org.example.learningprojectserver.repository.StudentWellbeingConsentRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.EngagementService;
import org.example.learningprojectserver.service.wellbeing.WellbeingPolicy.Mood;
import org.example.learningprojectserver.utils.HolidayUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Emotional check-in before exams ([s10]). The student answers "how do I feel?" and gets a
 * breathing exercise or a coping tip. When the student marks distress in DISTRESS_THRESHOLD of
 * their latest DISTRESS_WINDOW check-ins, the school manager (the school's counselor contact; the
 * platform has no counselor role) gets an alert, but only if the student opted in. The alert
 * names the student and the pattern, never the entries. Teachers have no access to check-ins.
 */
@Service
@RequiredArgsConstructor
public class ExamCheckinService {

    private static final Logger LOG = Logger.getLogger(ExamCheckinService.class.getName());

    static final String ALERT_TITLE = "צ'ק-אין רגשי: תלמיד/ה ביקש/ה תמיכה";

    private final StudentMoodCheckinRepository checkinRepository;
    private final StudentWellbeingConsentRepository consentRepository;
    private final StudentRepository studentRepository;
    private final StudentMoodCheckinEntityToDTOMapper checkinMapper;
    private final ReflectionService reflectionService;
    private final NotificationEventPublisher notificationEventPublisher;

    /** What the dashboard card offers: the Sunday reflection and/or a check-in before an exam. */
    public BasicResponse<Prompt> prompt(String studentUserId) {
        LocalDate today = EngagementService.today();
        boolean checkedIn = checkinRepository.findFirstByStudentUserIdAndCheckinDate(studentUserId, today).isPresent();
        return ok(new Prompt(!reflectionService.hasThisWeeksReflection(studentUserId),
                today.getDayOfWeek() == DayOfWeek.SUNDAY, upcomingExam(studentUserId), checkedIn));
    }

    public BasicResponse<CheckinStatus> status(String studentUserId) {
        List<StudentMoodCheckinEntity> recent = checkinRepository.findTop10ByStudentUserIdOrderByCheckinDateDescIdDesc(studentUserId);
        int distress = WellbeingPolicy.distressCount(recent.stream().map(StudentMoodCheckinEntity::isDistress).toList());
        return ok(new CheckinStatus(consent(studentUserId), recent.stream().map(checkinMapper).toList(),
                upcomingExam(studentUserId), distress, WellbeingPolicy.DISTRESS_WINDOW));
    }

    /** Records today's check-in (a second one on the same day replaces the first). */
    @Transactional
    public BasicResponse<CheckinResult> checkIn(String studentUserId, CheckinRequest request) {
        Mood mood = Mood.parse(request.mood());
        if (mood == null) {
            return new BasicResponse<>(false, "יש לבחור איך את/ה מרגיש/ה");
        }
        LocalDate today = EngagementService.today();
        UpcomingExam exam = upcomingExam(studentUserId);

        StudentMoodCheckinEntity c = checkinRepository.findFirstByStudentUserIdAndCheckinDate(studentUserId, today)
                .orElseGet(() -> {
                    StudentMoodCheckinEntity created = new StudentMoodCheckinEntity();
                    created.setStudentUserId(studentUserId);
                    created.setCheckinDate(today);
                    return created;
                });
        c.setMood(mood.name());
        c.setDistress(mood.distress());
        c.setExamLabel(exam == null ? null : exam.subject() + " " + exam.topic());
        c.setCreatedAt(LocalDateTime.now(EngagementService.ZONE));
        checkinRepository.save(c);

        List<Boolean> latest = checkinRepository.findTop10ByStudentUserIdOrderByCheckinDateDescIdDesc(studentUserId)
                .stream().map(StudentMoodCheckinEntity::isDistress).toList();
        boolean pattern = WellbeingPolicy.distressPattern(latest);
        boolean consented = consent(studentUserId).consented();
        boolean alertedRecently = checkinRepository.existsByStudentUserIdAndEscalatedTrueAndCheckinDateAfter(
                studentUserId, today.minusDays(WellbeingPolicy.ALERT_COOLDOWN_DAYS));

        boolean alertSent = false;
        if (WellbeingPolicy.shouldAlert(pattern, consented, alertedRecently)) {
            alertSent = alertStaff(studentUserId, WellbeingPolicy.distressCount(latest));
            if (alertSent) {
                c.setEscalated(true);
                checkinRepository.save(c);
            }
        }

        WellbeingPolicy.Suggestion suggestion = WellbeingPolicy.suggestionFor(mood, today.getDayOfYear());
        BasicResponse<CheckinResult> response = new BasicResponse<>(true, "תודה ששיתפת");
        response.setData(new CheckinResult(checkinMapper.apply(c), suggestion, pattern, alertSent, consented));
        return response;
    }

    /** Opt in or out of the alert to the school staff; both moments are kept with a timestamp. */
    @Transactional
    public BasicResponse<Consent> setConsent(String studentUserId, boolean consent) {
        StudentWellbeingConsentEntity e = consentRepository.findById(studentUserId)
                .orElseGet(() -> new StudentWellbeingConsentEntity(studentUserId));
        LocalDateTime now = LocalDateTime.now(EngagementService.ZONE);
        e.setConsented(consent);
        if (consent) {
            e.setConsentedAt(now);
            e.setPolicyVersion(WellbeingPolicy.PRIVACY_POLICY_VERSION);
        } else {
            e.setRevokedAt(now);
        }
        consentRepository.save(e);
        BasicResponse<Consent> response = new BasicResponse<>(true,
                consent ? "ההסכמה נשמרה. אפשר לבטל אותה בכל רגע." : "ההסכמה בוטלה. לא יישלחו התראות לצוות.");
        response.setData(toDto(e));
        return response;
    }

    /** The nearest teacher test the student has not taken yet, within EXAM_HORIZON_DAYS. */
    UpcomingExam upcomingExam(String studentUserId) {
        return studentRepository.findAllTestsForStudent(studentUserId).stream()
                .filter(t -> t.getScore() < 0)
                .map(t -> new UpcomingExam(t.getTestId(), t.getSubject(), t.getTopic(), t.getStartTime(),
                        HolidayUtils.daysUntil(t.getStartTime())))
                .filter(e -> e.daysLeft() >= 0 && e.daysLeft() <= WellbeingPolicy.EXAM_HORIZON_DAYS)
                .min(Comparator.comparingInt(UpcomingExam::daysLeft))
                .orElse(null);
    }

    private Consent consent(String studentUserId) {
        return consentRepository.findById(studentUserId).map(ExamCheckinService::toDto)
                .orElse(new Consent(false, null, null, null));
    }

    private static Consent toDto(StudentWellbeingConsentEntity e) {
        return new Consent(e.isConsented(), e.getConsentedAt(), e.getRevokedAt(), e.getPolicyVersion());
    }

    /** Notifies the school manager; a failure never undoes the student's check-in. */
    private boolean alertStaff(String studentUserId, int distressCount) {
        try {
            List<Object[]> rows = checkinRepository.findStudentContact(studentUserId);
            if (rows.isEmpty() || rows.get(0)[2] == null) return false;
            Object[] row = rows.get(0);
            String name = String.valueOf(row[0]);
            String className = row[1] == null ? "" : " (כיתה " + row[1] + ")";
            String managerId = String.valueOf(row[2]);
            String content = "התלמיד/ה " + name + className + " סימן/ה לחץ או מצוקה ב-" + distressCount + " מתוך "
                    + WellbeingPolicy.DISTRESS_WINDOW + " הצ'ק-אינים האחרונים לפני מבחנים, ונתן/ה הסכמה מפורשת לפנייה מהצוות. "
                    + "מומלץ לשוחח איתו/ה בעדינות או להפנות ליועצ/ת. תוכן הצ'ק-אינים עצמו אינו משותף.";
            MessageDTO message = new MessageDTO(ALERT_TITLE, content, LocalDateTime.now(EngagementService.ZONE), "צ'ק-אין רגשי");
            NotificationDTO<SystemMessageDTO> dto = new NotificationDTO<>(NotificationType.SYSTEM_MESSAGE, new SystemMessageDTO(message));
            dto.setTitle(ALERT_TITLE);
            dto.setBody(content);
            dto.setLink("/messages");
            notificationEventPublisher.publish(List.of(managerId), dto);
            return true;
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Wellbeing alert failed", e);
            return false;
        }
    }

    private static <T> BasicResponse<T> ok(T data) {
        BasicResponse<T> response = new BasicResponse<>(true, null);
        response.setData(data);
        return response;
    }
}
