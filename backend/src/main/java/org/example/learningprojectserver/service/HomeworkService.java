package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.HomeworkDTOs.*;
import org.example.learningprojectserver.dto.HomeworkRequest;
import org.example.learningprojectserver.dto.StudentBriefDTO;
import org.example.learningprojectserver.entities.HomeworkEntity;
import org.example.learningprojectserver.entities.HomeworkSubmissionEntity;
import org.example.learningprojectserver.notification.NotificationType;
import org.example.learningprojectserver.notification.dto.HomeworkNotificationDTO;
import org.example.learningprojectserver.notification.dto.NotificationDTO;
import org.example.learningprojectserver.notification.publisher.NotificationEventPublisher;
import org.example.learningprojectserver.repository.HomeworkRepository;
import org.example.learningprojectserver.repository.HomeworkSubmissionRepository;
import org.example.learningprojectserver.repository.StudentAccommodationRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Homework: separate from tests, with a due date, late flag, reminders and a teacher board. */
@Service
@RequiredArgsConstructor
public class HomeworkService {

    static final int REMINDER_HOURS_BEFORE_DUE = 24;
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int MAX_CONTENT = 20000;

    private final HomeworkRepository homeworkRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final StudentAccommodationRepository studentLookupRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final EngagementEvents engagementEvents;

    private static LocalDateTime now() {
        return LocalDateTime.now(EngagementService.ZONE);
    }

    /** Status of one student's homework at a given moment. */
    static String status(HomeworkSubmissionEntity submission, LocalDateTime dueDate, LocalDateTime now) {
        if (submission != null) return submission.isLate() ? "LATE" : "SUBMITTED";
        return now.isAfter(dueDate) ? "MISSING" : "PENDING";
    }

    private BasicResponse ok(String message, Object data) {
        BasicResponse<Object> response = new BasicResponse<>(true, message);
        response.setData(data);
        return response;
    }

    @Transactional(readOnly = true)
    public BasicResponse getTeacherClasses(String teacherId) {
        Map<Long, List<StudentBriefDTO>> byClass = studentLookupRepository.findStudentsOfTeacher(teacherId).stream()
                .collect(Collectors.groupingBy(StudentBriefDTO::classRoomId, LinkedHashMap::new, Collectors.toList()));
        List<TeacherClass> classes = byClass.values().stream()
                .map(list -> new TeacherClass(list.get(0).classRoomId(), list.get(0).className(), list.size()))
                .toList();
        return ok(null, classes);
    }

    @Transactional
    public BasicResponse create(String teacherId, HomeworkRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            return new BasicResponse(false, "יש להזין כותרת לשיעורי הבית");
        }
        LocalDateTime due;
        try {
            due = LocalDateTime.parse(request.dueDate());
        } catch (DateTimeParseException | NullPointerException e) {
            return new BasicResponse(false, "תאריך ההגשה אינו תקין");
        }
        if (!due.isAfter(now())) {
            return new BasicResponse(false, "תאריך ההגשה חייב להיות בעתיד");
        }
        if (request.classRoomIds() == null || request.classRoomIds().isEmpty()) {
            return new BasicResponse(false, "יש לבחור לפחות כיתה אחת");
        }
        Set<Long> chosen = new HashSet<>(request.classRoomIds());
        List<String> students = studentLookupRepository.findStudentsOfTeacher(teacherId).stream()
                .filter(s -> chosen.contains(s.classRoomId()))
                .map(StudentBriefDTO::userId)
                .distinct()
                .toList();
        if (students.isEmpty()) {
            return new BasicResponse(false, "לא נמצאו תלמידים בכיתות שבחרת");
        }

        HomeworkEntity h = new HomeworkEntity();
        h.setTeacherUserId(teacherId);
        h.setTitle(request.title().trim());
        h.setDescription(request.description());
        h.setSubject(request.subject());
        h.setDueDate(due);
        h.setCreatedAt(now());
        h.setStudentUserIds(new ArrayList<>(students));
        homeworkRepository.save(h);

        notify(students, NotificationType.NEW_HOMEWORK, h, "שיעורי בית חדשים: " + h.getTitle());
        return ok("שיעורי הבית נשלחו ל-" + students.size() + " תלמידים", h.getId());
    }

    private void notify(Collection<String> students, NotificationType type, HomeworkEntity h, String message) {
        if (students.isEmpty()) return;
        HomeworkNotificationDTO payload =
                new HomeworkNotificationDTO(h.getId(), h.getTitle(), h.getDueDate().format(DISPLAY), message);
        notificationEventPublisher.publish(students, new NotificationDTO<>(type, payload));
    }

    @Transactional(readOnly = true)
    public BasicResponse getTeacherBoard(String teacherId) {
        List<HomeworkEntity> homework = homeworkRepository.findByTeacherUserIdOrderByDueDateDesc(teacherId);
        if (homework.isEmpty()) return ok(null, List.of());

        Map<String, StudentBriefDTO> students = studentLookupRepository.findStudentsOfTeacher(teacherId).stream()
                .collect(Collectors.toMap(StudentBriefDTO::userId, Function.identity(), (a, b) -> a));
        Map<Long, Map<String, HomeworkSubmissionEntity>> submissions = submissionRepository
                .findByHomeworkIdIn(homework.stream().map(HomeworkEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(s -> s.getHomework().getId(),
                        Collectors.toMap(HomeworkSubmissionEntity::getStudentUserId, Function.identity(), (a, b) -> a)));

        LocalDateTime now = now();
        List<BoardHomework> board = new ArrayList<>();
        for (HomeworkEntity h : homework) {
            Map<String, HomeworkSubmissionEntity> subs = submissions.getOrDefault(h.getId(), Map.of());
            List<BoardRow> rows = h.getStudentUserIds().stream().map(id -> {
                StudentBriefDTO s = students.get(id);
                HomeworkSubmissionEntity sub = subs.get(id);
                return new BoardRow(id, s == null ? id : s.username(), s == null ? "" : s.className(),
                        status(sub, h.getDueDate(), now),
                        sub == null ? null : sub.getSubmittedAt(),
                        sub == null ? null : sub.getContent());
            }).toList();
            int submitted = (int) rows.stream().filter(r -> r.status().equals("SUBMITTED") || r.status().equals("LATE")).count();
            int late = (int) rows.stream().filter(r -> r.status().equals("LATE")).count();
            board.add(new BoardHomework(h.getId(), h.getTitle(), h.getDescription(), h.getSubject(), h.getDueDate(),
                    h.isReminderSent(), rows.size(), submitted, late, rows));
        }
        return ok(null, board);
    }

    @Transactional(readOnly = true)
    public BasicResponse getStudentHomework(String studentId) {
        List<HomeworkEntity> homework = homeworkRepository.findForStudent(studentId);
        if (homework.isEmpty()) return ok(null, List.of());
        Map<Long, HomeworkSubmissionEntity> mine = submissionRepository
                .findByHomeworkIdIn(homework.stream().map(HomeworkEntity::getId).toList()).stream()
                .filter(s -> s.getStudentUserId().equals(studentId))
                .collect(Collectors.toMap(s -> s.getHomework().getId(), Function.identity(), (a, b) -> a));
        LocalDateTime now = now();
        List<StudentHomework> list = homework.stream().map(h -> {
            HomeworkSubmissionEntity s = mine.get(h.getId());
            return new StudentHomework(h.getId(), h.getTitle(), h.getDescription(), h.getSubject(), h.getDueDate(),
                    status(s, h.getDueDate(), now), s == null ? null : s.getContent(), s == null ? null : s.getSubmittedAt());
        }).toList();
        return ok(null, list);
    }

    /** Submits or resubmits. After the due date it is still accepted, and flagged late. */
    @Transactional
    public BasicResponse submit(String studentId, Long homeworkId, String content) {
        HomeworkEntity h = homeworkRepository.findById(homeworkId).orElse(null);
        if (h == null || !h.getStudentUserIds().contains(studentId)) {
            return new BasicResponse(false, "שיעורי הבית לא נמצאו");
        }
        if (content == null || content.isBlank()) {
            return new BasicResponse(false, "לא ניתן להגיש תשובה ריקה");
        }
        if (content.length() > MAX_CONTENT) {
            return new BasicResponse(false, "התשובה ארוכה מדי");
        }
        LocalDateTime now = now();
        HomeworkSubmissionEntity s = submissionRepository.findFirstByHomeworkIdAndStudentUserId(homeworkId, studentId)
                .orElse(null);
        boolean firstSubmission = s == null;
        if (firstSubmission) {
            s = new HomeworkSubmissionEntity();
            s.setHomework(h);
            s.setStudentUserId(studentId);
        }
        boolean late = now.isAfter(h.getDueDate());
        s.setContent(content);
        s.setSubmittedAt(now);
        s.setLate(late);
        submissionRepository.save(s);

        if (firstSubmission) {
            engagementEvents.homeworkSubmitted(studentId, late, submissionRepository.countByStudentUserIdAndLateFalse(studentId));
        }
        return new BasicResponse(true, late ? "ההגשה נקלטה, אך לאחר מועד ההגשה" : "ההגשה נקלטה בהצלחה");
    }

    /**
     * Called by HomeworkReminderJob: one reminder per homework, to the students who have not
     * submitted, once it is due within REMINDER_HOURS_BEFORE_DUE hours.
     */
    @Transactional
    public int sendDueReminders() {
        LocalDateTime now = now();
        List<HomeworkEntity> due = homeworkRepository.findDueForReminder(now, now.plusHours(REMINDER_HOURS_BEFORE_DUE));
        if (due.isEmpty()) return 0;
        Map<Long, Set<String>> submitted = submissionRepository
                .findByHomeworkIdIn(due.stream().map(HomeworkEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(s -> s.getHomework().getId(),
                        Collectors.mapping(HomeworkSubmissionEntity::getStudentUserId, Collectors.toSet())));
        int sent = 0;
        for (HomeworkEntity h : due) {
            Set<String> done = submitted.getOrDefault(h.getId(), Set.of());
            List<String> missing = h.getStudentUserIds().stream().filter(id -> !done.contains(id)).toList();
            notify(missing, NotificationType.HOMEWORK_REMINDER, h,
                    "תזכורת: יש להגיש את \"" + h.getTitle() + "\" עד " + h.getDueDate().format(DISPLAY));
            h.setReminderSent(true);
            sent += missing.size();
        }
        homeworkRepository.saveAll(due);
        return sent;
    }
}
