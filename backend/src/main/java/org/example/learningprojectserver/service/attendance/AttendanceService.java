package org.example.learningprojectserver.service.attendance;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.*;
import org.example.learningprojectserver.entities.*;
import org.example.learningprojectserver.enums.AttendanceStatus;
import org.example.learningprojectserver.notification.NotificationType;
import org.example.learningprojectserver.notification.dto.NotificationDTO;
import org.example.learningprojectserver.notification.dto.SystemMessageDTO;
import org.example.learningprojectserver.notification.publisher.NotificationEventPublisher;
import org.example.learningprojectserver.repository.AttendanceRecordRepository;
import org.example.learningprojectserver.repository.LessonRepository;
import org.example.learningprojectserver.repository.MessageRepository;
import org.example.learningprojectserver.repository.UserRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Attendance per timetable lesson and date. Teachers mark it, teachers and managers read absence reports,
 * and an unusual absence (AbsenceAlertPolicy) sends the school manager a message. Not cached itself; the
 * alert message evicts the manager's receivedMessages entry like MessageService.sendMessage does.
 */
@Service
@RequiredArgsConstructor
public class AttendanceService {

    static final String ALERT_TITLE = "התראת היעדרות חריגה";
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_REPORT_DAYS = 366;
    private static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Jerusalem");

    private final AttendanceRecordRepository attendanceRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final CacheManager cacheManager;

    @Transactional(readOnly = true)
    public BasicResponse<List<AttendanceLessonDTO>> getLessonsForDay(String teacherId, String dateText) {
        LocalDate date = parseDate(dateText, today());
        if (date == null) return new BasicResponse<>(false, "התאריך אינו תקין");

        List<AttendanceLessonDTO> lessons = attendanceRepository.findTeacherLessonsOnDay(teacherId, date.getDayOfWeek());
        if (!lessons.isEmpty()) {
            Map<Long, Long> marked = new HashMap<>();
            for (Object[] row : attendanceRepository.countMarkedByLesson(
                    lessons.stream().map(AttendanceLessonDTO::lessonId).toList(), date)) {
                marked.put((Long) row[0], (Long) row[1]);
            }
            lessons = lessons.stream().map(l -> l.withMarkedCount(marked.getOrDefault(l.lessonId(), 0L))).toList();
        }
        BasicResponse<List<AttendanceLessonDTO>> response = new BasicResponse<>(true,
                lessons.isEmpty() ? "אין לך שיעורים במערכת השעות ביום זה" : null);
        response.setData(lessons);
        return response;
    }

    @Transactional(readOnly = true)
    public BasicResponse<AttendanceSheetDTO> getSheet(String teacherId, Long lessonId, String dateText) {
        LocalDate date = parseDate(dateText, today());
        if (date == null) return new BasicResponse<>(false, "התאריך אינו תקין");
        LessonEntity lesson = lessonRepository.findById(lessonId).orElse(null);
        String error = checkLesson(lesson, teacherId, date);
        if (error != null) return new BasicResponse<>(false, error);

        BasicResponse<AttendanceSheetDTO> response = new BasicResponse<>(true, null);
        response.setData(sheet(lesson, date));
        return response;
    }

    @Transactional
    public BasicResponse<AttendanceSheetDTO> mark(String teacherId, Long lessonId, String dateText,
                                                  List<AttendanceMarkRequest> marks) {
        LocalDate date = parseDate(dateText, today());
        if (date == null) return new BasicResponse<>(false, "התאריך אינו תקין");
        if (date.isAfter(today())) return new BasicResponse<>(false, "לא ניתן לסמן נוכחות לתאריך עתידי");
        LessonEntity lesson = lessonRepository.findById(lessonId).orElse(null);
        String error = checkLesson(lesson, teacherId, date);
        if (error != null) return new BasicResponse<>(false, error);
        if (marks == null || marks.isEmpty()) return new BasicResponse<>(false, "לא סומנה נוכחות");

        ClassRoomEntity classRoom = lesson.getSchedule().getClassRoom();
        Map<String, StudentEntity> students = classRoom.getStudents().stream()
                .collect(Collectors.toMap(StudentEntity::getUserId, s -> s, (a, b) -> a));

        Map<String, AttendanceStatus> requested = new LinkedHashMap<>();
        for (AttendanceMarkRequest m : marks) {
            if (m == null || m.getStudentId() == null || !students.containsKey(m.getStudentId())) {
                return new BasicResponse<>(false, "אחד התלמידים אינו לומד בכיתה זו");
            }
            AttendanceStatus status = parseStatus(m.getStatus());
            if (status == null) return new BasicResponse<>(false, "סטטוס נוכחות לא תקין");
            requested.put(m.getStudentId(), status);
        }

        Map<String, AttendanceRecordEntity> existing = attendanceRepository.findByLessonAndDate(lessonId, date).stream()
                .collect(Collectors.toMap(AttendanceRecordEntity::getStudentUserId, r -> r, (a, b) -> a));
        int absentBefore = (int) existing.values().stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();

        String schoolCode = classRoom.getSchool() == null ? null : classRoom.getSchool().getSchoolCode();
        List<String> newlyAbsent = new ArrayList<>();
        List<AttendanceRecordEntity> toSave = new ArrayList<>();
        for (Map.Entry<String, AttendanceStatus> e : requested.entrySet()) {
            AttendanceRecordEntity record = existing.get(e.getKey());
            if (record == null) {
                record = new AttendanceRecordEntity();
                record.setLessonId(lesson.getId());
                record.setLessonDate(date);
                record.setStudentUserId(e.getKey());
                existing.put(e.getKey(), record);
            }
            if (e.getValue() == AttendanceStatus.ABSENT && record.getStatus() != AttendanceStatus.ABSENT) {
                newlyAbsent.add(e.getKey());
            }
            StudentEntity student = students.get(e.getKey());
            record.setStudentName(student.getUsername());
            record.setStatus(e.getValue());
            record.setSubject(lesson.getSubject());
            record.setClassRoomId(classRoom.getId());
            record.setClassRoomName(classRoom.getName());
            record.setSchoolCode(schoolCode);
            record.setMarkedByTeacherId(teacherId);
            record.setMarkedAt(LocalDateTime.now());
            toSave.add(record);
        }
        attendanceRepository.saveAll(toSave);
        attendanceRepository.flush();

        int absentAfter = (int) existing.values().stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
        int alerts = sendAlerts(lesson, classRoom, date, newlyAbsent, absentBefore, absentAfter);

        BasicResponse<AttendanceSheetDTO> response = new BasicResponse<>(true, alerts == 0
                ? "הנוכחות נשמרה"
                : "הנוכחות נשמרה ונשלחה התראה למנהל/ת על היעדרות חריגה");
        response.setData(sheet(lesson, date));
        return response;
    }

    /**
     * Report over [from, to] (defaults: the last 30 days). A teacher sees the lessons they marked; a school
     * manager sees the whole school.
     */
    @Transactional(readOnly = true)
    public BasicResponse<AttendanceReportDTO> getReport(String requesterId, String fromText, String toText,
                                                        String className) {
        UserEntity requester = userRepository.findUserByUserId(requesterId);
        String schoolCode;
        String teacherFilter;
        String scope;
        if (requester instanceof TeacherEntity teacher) {
            schoolCode = teacher.getTeachingSchool() == null ? null : teacher.getTeachingSchool().getSchoolCode();
            teacherFilter = requesterId;
            scope = "TEACHER";
        } else if (requester instanceof SchoolManagerEntity manager) {
            schoolCode = manager.getSchool() == null ? null : manager.getSchool().getSchoolCode();
            teacherFilter = null;
            scope = "SCHOOL";
        } else {
            return new BasicResponse<>(false, "דוח נוכחות זמין למורים ולמנהלי בית ספר בלבד");
        }
        if (schoolCode == null) return new BasicResponse<>(false, "המשתמש אינו משויך לבית ספר");

        LocalDate to = parseDate(toText, today());
        LocalDate from = parseDate(fromText, (to == null ? today() : to).minusDays(AbsenceAlertPolicy.WINDOW_DAYS - 1));
        if (from == null || to == null) return new BasicResponse<>(false, "התאריך אינו תקין");
        if (from.isAfter(to)) return new BasicResponse<>(false, "תאריך ההתחלה מאוחר מתאריך הסיום");
        if (ChronoUnit.DAYS.between(from, to) > MAX_REPORT_DAYS) {
            return new BasicResponse<>(false, "ניתן להפיק דוח לטווח של עד שנה");
        }
        String classFilter = className == null || className.isBlank() ? null : className.trim();

        List<AttendanceCountDTO> counts = attendanceRepository.countForReport(schoolCode, teacherFilter, classFilter, from, to);
        Map<String, long[]> byStudent = new LinkedHashMap<>(); // present, absent, late, excused
        Map<String, String[]> names = new HashMap<>();
        for (AttendanceCountDTO c : counts) {
            String key = c.studentUserId() + "|" + c.classRoomName();
            long[] acc = byStudent.computeIfAbsent(key, k -> new long[4]);
            acc[c.status().ordinal()] += c.count();
            names.put(key, new String[]{c.studentUserId(), c.studentName(), c.classRoomName()});
        }

        List<AttendanceReportDTO.Row> rows = new ArrayList<>();
        long[] totals = new long[4];
        byStudent.forEach((key, acc) -> {
            long total = acc[0] + acc[1] + acc[2] + acc[3];
            for (int i = 0; i < 4; i++) totals[i] += acc[i];
            String[] n = names.get(key);
            double rate = total == 0 ? 0 : Math.round(1000.0 * acc[1] / total) / 10.0;
            rows.add(new AttendanceReportDTO.Row(n[0], n[1], n[2], acc[0], acc[1], acc[2], acc[3], total, rate,
                    AbsenceAlertPolicy.unusualInReport(acc[1], total)));
        });
        rows.sort(Comparator.comparingLong(AttendanceReportDTO.Row::absent).reversed()
                .thenComparing(AttendanceReportDTO.Row::name, Comparator.nullsLast(String::compareTo)));

        Map<String, Long> totalsOut = new LinkedHashMap<>();
        for (AttendanceStatus s : AttendanceStatus.values()) totalsOut.put(s.name(), totals[s.ordinal()]);

        BasicResponse<AttendanceReportDTO> response = new BasicResponse<>(true, rows.isEmpty() ? "אין נתוני נוכחות בטווח שנבחר" : null);
        response.setData(new AttendanceReportDTO(from.toString(), to.toString(), scope,
                attendanceRepository.findReportClassNames(schoolCode, teacherFilter), rows, totalsOut));
        return response;
    }

    // ---- internals ----

    private int sendAlerts(LessonEntity lesson, ClassRoomEntity classRoom, LocalDate date, List<String> newlyAbsent,
                           int absentBefore, int absentAfter) {
        List<String> lines = new ArrayList<>();
        LocalDate windowStart = date.minusDays(AbsenceAlertPolicy.WINDOW_DAYS - 1);
        for (String studentId : newlyAbsent) {
            long absences = attendanceRepository.countByStudentAndStatus(studentId, AttendanceStatus.ABSENT, windowStart, date);
            if (AbsenceAlertPolicy.studentAlert(absences)) {
                String name = classRoom.getStudents().stream().filter(s -> s.getUserId().equals(studentId))
                        .map(UserEntity::getUsername).findFirst().orElse(studentId);
                lines.add("התלמיד/ה " + name + " (כיתה " + classRoom.getName() + ") נעדר/ה " + absences
                        + " פעמים ב-" + AbsenceAlertPolicy.WINDOW_DAYS + " הימים האחרונים.");
            }
        }
        int classSize = classRoom.getStudents().size();
        if (AbsenceAlertPolicy.classAlert(absentBefore, absentAfter, classSize)) {
            lines.add(absentAfter + " מתוך " + classSize + " תלמידי כיתה " + classRoom.getName()
                    + " נעדרו משיעור " + lesson.getSubject() + " בתאריך " + date.format(DISPLAY_DATE) + ".");
        }
        if (lines.isEmpty()) return 0;

        try {
            SchoolEntity school = classRoom.getSchool();
            SchoolManagerEntity manager = school == null ? null : school.getSchoolManager();
            if (manager == null) return 0;
            TeacherEntity teacher = lesson.getTeacher();
            String content = String.join("\n", lines) + "\nסומן על ידי " + teacher.getUsername() + ".";

            MessageEntity message = new MessageEntity();
            message.setSender(teacher);
            message.setReceiver(manager);
            message.setTitle(ALERT_TITLE);
            message.setContent(content);
            message.setSentAt(LocalDateTime.now());
            messageRepository.save(message);

            Cache received = cacheManager.getCache("receivedMessages");
            if (received != null) received.evict(manager.getUserId());

            MessageDTO dto = new MessageDTO(ALERT_TITLE, content, message.getSentAt(), teacher.getUsername());
            notificationEventPublisher.publish(List.of(manager.getUserId()),
                    new NotificationDTO<>(NotificationType.SYSTEM_MESSAGE, new SystemMessageDTO(dto)));
            return lines.size();
        } catch (RuntimeException e) {
            // The alert is a side effect: never let it undo the teacher's attendance.
            return 0;
        }
    }

    private AttendanceSheetDTO sheet(LessonEntity lesson, LocalDate date) {
        ClassRoomEntity classRoom = lesson.getSchedule().getClassRoom();
        Map<String, AttendanceStatus> statuses = attendanceRepository.findByLessonAndDate(lesson.getId(), date).stream()
                .collect(Collectors.toMap(AttendanceRecordEntity::getStudentUserId, AttendanceRecordEntity::getStatus, (a, b) -> a));
        List<AttendanceSheetDTO.Row> rows = classRoom.getStudents().stream()
                .sorted(Comparator.comparing(UserEntity::getUsername, Comparator.nullsLast(String::compareTo)))
                .map(s -> new AttendanceSheetDTO.Row(s.getUserId(), s.getUsername(),
                        statuses.containsKey(s.getUserId()) ? statuses.get(s.getUserId()).name() : null))
                .toList();
        DateTimeFormatter hhmm = DateTimeFormatter.ofPattern("HH:mm");
        return new AttendanceSheetDTO(lesson.getId(), date.toString(), lesson.getSubject(), classRoom.getName(),
                lesson.getStartTime() == null ? null : lesson.getStartTime().format(hhmm),
                lesson.getEndTime() == null ? null : lesson.getEndTime().format(hhmm), rows);
    }

    private static String checkLesson(LessonEntity lesson, String teacherId, LocalDate date) {
        if (lesson == null || lesson.getTeacher() == null || !teacherId.equals(lesson.getTeacher().getUserId())) {
            return "השיעור לא נמצא במערכת השעות שלך";
        }
        if (lesson.getSchedule() == null || lesson.getSchedule().getClassRoom() == null) {
            return "השיעור אינו משויך לכיתה";
        }
        DayOfWeek day = lesson.getDayOfWeek();
        if (day != null && day != date.getDayOfWeek()) {
            return "השיעור אינו מתקיים ביום זה בשבוע";
        }
        return null;
    }

    /** School days follow Israel time, not the server's zone (Render runs in UTC). */
    static LocalDate today() {
        return LocalDate.now(SCHOOL_ZONE);
    }

    static LocalDate parseDate(String text, LocalDate fallback) {
        if (text == null || text.isBlank()) return fallback;
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static AttendanceStatus parseStatus(String value) {
        if (value == null) return null;
        try {
            return AttendanceStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
