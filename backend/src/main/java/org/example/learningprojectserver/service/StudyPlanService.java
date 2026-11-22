package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.StudyPlanDTO;
import org.example.learningprojectserver.entities.StudyPlanTaskEntity;
import org.example.learningprojectserver.entities.TeacherTestEntity;
import org.example.learningprojectserver.mappers.StudyPlanTaskEntityToTaskMapper;
import org.example.learningprojectserver.repository.StudyPlanTaskRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Daily study plans ahead of upcoming teacher tests. A plan is generated the first time the
 * student opens it after the test was assigned, then kept, so adherence can be measured.
 */
@Service
@RequiredArgsConstructor
public class StudyPlanService {

    /** A plan covers at most this many days before the exam. */
    static final int PLAN_HORIZON_DAYS = 14;

    private static final DateTimeFormatter TEST_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final StudyPlanTaskRepository taskRepository;
    private final StudyPlanTaskEntityToTaskMapper taskMapper;
    private final EngagementEvents engagementEvents;

    /** Daily minutes for a test's difficulty; a little more when the exam is close. */
    static int dailyMinutes(String difficulty, int studyDays) {
        int base;
        String d = difficulty == null ? "" : difficulty.trim();
        if (d.contains("קשה") || d.equalsIgnoreCase("hard")) base = 20;
        else if (d.contains("קל") || d.equalsIgnoreCase("easy")) base = 10;
        else base = 15;
        return studyDays <= 3 ? base + 5 : base;
    }

    /**
     * Builds the plan from today until the day before the exam (or just today when the exam is
     * today): learn first, practice in between, review on the last day.
     */
    static List<StudyPlanTaskEntity> buildTasks(String studentUserId, Long testId, String subject, String topic,
                                                String difficulty, LocalDate today, LocalDate examDate) {
        LocalDate first = today;
        LocalDate last = examDate.isAfter(today) ? examDate.minusDays(1) : today;
        if (ChronoUnit.DAYS.between(first, last) >= PLAN_HORIZON_DAYS) {
            first = last.minusDays(PLAN_HORIZON_DAYS - 1);
        }
        int days = (int) ChronoUnit.DAYS.between(first, last) + 1;
        int minutes = dailyMinutes(difficulty, days);
        String what = topic == null || topic.isBlank() ? subject : topic;

        List<StudyPlanTaskEntity> tasks = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            String type;
            String description;
            if (i == days - 1) {
                type = "REVIEW";
                description = "היום: " + minutes + " דקות חזרה על " + what + " ומבחן תרגול קצר";
            } else if (i == 0) {
                type = "LEARN";
                description = "היום: " + minutes + " דקות לימוד של " + what + " (שיעור ודוגמאות)";
            } else {
                type = "PRACTICE";
                description = "היום: " + minutes + " דקות תרגול על " + what;
            }
            StudyPlanTaskEntity t = new StudyPlanTaskEntity();
            t.setStudentUserId(studentUserId);
            t.setTestId(testId);
            t.setTaskDate(first.plusDays(i));
            t.setSubject(subject);
            t.setTopic(topic);
            t.setMinutes(minutes);
            t.setTaskType(type);
            t.setDescription(description);
            tasks.add(t);
        }
        return tasks;
    }

    private static LocalDateTime parseStart(String startTime) {
        try {
            return startTime == null ? null : LocalDateTime.parse(startTime, TEST_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Transactional
    public BasicResponse getPlan(String studentId) {
        LocalDate today = EngagementService.today();
        LocalDateTime now = LocalDateTime.now(EngagementService.ZONE);
        Set<Long> submitted = new HashSet<>(taskRepository.findSubmittedTestIds(studentId));

        List<TeacherTestEntity> upcoming = taskRepository.findTeacherTestsForStudent(studentId).stream()
                .filter(t -> !submitted.contains(t.getId()))
                .filter(t -> {
                    LocalDateTime start = parseStart(t.getStartTime());
                    return start != null && start.isAfter(now);
                })
                .sorted(Comparator.comparing(t -> parseStart(t.getStartTime())))
                .toList();

        for (TeacherTestEntity test : upcoming) {
            if (!taskRepository.existsByStudentUserIdAndTestId(studentId, test.getId())) {
                LocalDate examDate = parseStart(test.getStartTime()).toLocalDate();
                taskRepository.saveAll(buildTasks(studentId, test.getId(), test.getSubject(), test.getTopic(),
                        test.getDifficulty(), today, examDate));
            }
        }

        List<Long> testIds = upcoming.stream().map(TeacherTestEntity::getId).toList();
        Map<Long, List<StudyPlanTaskEntity>> tasksByTest = testIds.isEmpty() ? Map.of()
                : taskRepository.findByStudentUserIdAndTestIdInOrderByTaskDate(studentId, testIds).stream()
                .collect(Collectors.groupingBy(StudyPlanTaskEntity::getTestId));

        List<StudyPlanDTO.ExamPlan> exams = new ArrayList<>();
        List<StudyPlanDTO.Task> todayTasks = new ArrayList<>();
        for (TeacherTestEntity test : upcoming) {
            List<StudyPlanTaskEntity> tasks = tasksByTest.getOrDefault(test.getId(), List.of());
            List<StudyPlanTaskEntity> due = tasks.stream().filter(t -> !t.getTaskDate().isAfter(today)).toList();
            int doneDue = (int) due.stream().filter(StudyPlanTaskEntity::isCompleted).count();
            int adherence = due.isEmpty() ? 100 : doneDue * 100 / due.size();
            List<StudyPlanDTO.Task> dtoTasks = tasks.stream().map(taskMapper).toList();
            dtoTasks.stream().filter(t -> t.date().equals(today)).forEach(todayTasks::add);
            LocalDate examDate = parseStart(test.getStartTime()).toLocalDate();
            exams.add(new StudyPlanDTO.ExamPlan(test.getId(), test.getSubject(), test.getTopic(), test.getStartTime(),
                    (int) ChronoUnit.DAYS.between(today, examDate), due.size(), doneDue, adherence, dtoTasks));
        }

        BasicResponse<StudyPlanDTO> response = new BasicResponse<>(true, null);
        response.setData(new StudyPlanDTO(todayTasks, exams));
        return response;
    }

    @Transactional
    public BasicResponse completeTask(String studentId, Long taskId) {
        StudyPlanTaskEntity task = taskRepository.findById(taskId).orElse(null);
        if (task == null || !task.getStudentUserId().equals(studentId)) {
            return new BasicResponse(false, "המשימה לא נמצאה");
        }
        if (task.getTaskDate().isAfter(EngagementService.today())) {
            return new BasicResponse(false, "אפשר לסמן רק משימות של היום או של ימים שעברו");
        }
        if (task.isCompleted()) {
            return new BasicResponse(true, "המשימה כבר סומנה כבוצעה");
        }
        task.setCompleted(true);
        task.setCompletedAt(LocalDateTime.now(EngagementService.ZONE));
        taskRepository.save(task);
        engagementEvents.studyTaskDone(studentId);
        return new BasicResponse(true, "כל הכבוד! המשימה סומנה כבוצעה");
    }
}
