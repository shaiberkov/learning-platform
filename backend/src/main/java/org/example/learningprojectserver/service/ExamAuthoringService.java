package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.StudentTestStatusDTO;
import org.example.learningprojectserver.dto.exam.CloneExamRequest;
import org.example.learningprojectserver.dto.exam.CreateExamRequest;
import org.example.learningprojectserver.dto.exam.ExamQuestionInput;
import org.example.learningprojectserver.dto.exam.TeacherExamSummary;
import org.example.learningprojectserver.entities.StudentEntity;
import org.example.learningprojectserver.entities.TeacherEntity;
import org.example.learningprojectserver.entities.TeacherTestEntity;
import org.example.learningprojectserver.entities.TestQuestionEntity;
import org.example.learningprojectserver.enums.ExamAttemptStatus;
import org.example.learningprojectserver.mappers.TeacherTestToExamSummaryMapper;
import org.example.learningprojectserver.notification.NotificationType;
import org.example.learningprojectserver.notification.dto.NewTestMessageDTO;
import org.example.learningprojectserver.notification.dto.NotificationDTO;
import org.example.learningprojectserver.notification.publisher.NotificationEventPublisher;
import org.example.learningprojectserver.repository.ExamAttemptRepository;
import org.example.learningprojectserver.repository.QuestionRepository;
import org.example.learningprojectserver.repository.StudentRepository;
import org.example.learningprojectserver.repository.TeacherRepository;
import org.example.learningprojectserver.repository.TeacherTestRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.exam.ExamQuestionCodec;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Teacher-side exam management: authoring with the new question types, cloning, templates,
 * A/B versions and scheduled publishing. A test with {@code published = false} stays hidden from
 * its students until {@code jobs/ExamSchedulerJob} publishes it at {@code publishAt}.
 */
@Service
@RequiredArgsConstructor
public class ExamAuthoringService {

    static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int MAX_QUESTIONS = 100;

    private final TeacherRepository teacherRepository;
    private final TeacherTestRepository teacherTestRepository;
    private final StudentRepository studentRepository;
    private final QuestionRepository questionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamQuestionCodec examQuestionCodec;
    private final TeacherTestToExamSummaryMapper summaryMapper;
    private final NotificationEventPublisher notificationEventPublisher;
    private final CacheManager cacheManager;

    /** Everything needed to create one exam (or its A/B pair) from a list of question prototypes. */
    private record ExamSpec(String title, String subject, String topic, String startTime, int timeLimitMinutes,
                            String publishAt, List<String> studentIds, boolean template, boolean versions,
                            Boolean shuffleQuestions, Boolean shuffleOptions, Long sourceTestId) {}

    @Transactional
    public BasicResponse<List<TeacherExamSummary>> createExam(String teacherId, CreateExamRequest request) {
        TeacherEntity teacher = teacherRepository.findByUserId(teacherId);
        if (teacher == null) {
            return new BasicResponse<>(false, "המורה לא נמצא");
        }
        if (isBlank(request.getTitle())) {
            return new BasicResponse<>(false, "יש להזין כותרת למבחן");
        }
        if (isBlank(request.getSubject())) {
            return new BasicResponse<>(false, "המקצוע לא הוזן");
        }
        List<ExamQuestionInput> inputs = request.getQuestions() == null ? List.of() : request.getQuestions();
        if (inputs.isEmpty()) {
            return new BasicResponse<>(false, "יש להוסיף לפחות שאלה אחת");
        }
        if (inputs.size() > MAX_QUESTIONS) {
            return new BasicResponse<>(false, "ניתן להוסיף עד " + MAX_QUESTIONS + " שאלות");
        }
        String topic = isBlank(request.getTopic()) ? request.getTitle().trim() : request.getTopic().trim();
        List<TestQuestionEntity> prototypes = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            String error = examQuestionCodec.validate(inputs.get(i), i + 1);
            if (error != null) {
                return new BasicResponse<>(false, error);
            }
            TestQuestionEntity q = examQuestionCodec.toEntity(inputs.get(i), request.getSubject().trim(), topic);
            q.setPosition(i);
            prototypes.add(q);
        }
        ExamSpec spec = new ExamSpec(request.getTitle().trim(), request.getSubject().trim(), topic,
                request.getStartTime(), request.getTimeLimitMinutes(), request.getPublishAt(),
                request.getStudentIds(), request.isTemplate(), request.isVersions(),
                request.getShuffleQuestions(), request.getShuffleOptions(), null);
        return materialize(teacher, spec, prototypes);
    }

    @Transactional
    public BasicResponse<List<TeacherExamSummary>> cloneExam(String teacherId, Long testId, CloneExamRequest request) {
        TeacherTestEntity source = teacherTestRepository.findTeacherTestByTestId(testId);
        if (source == null || source.getTeacher() == null || !source.getTeacher().getUserId().equals(teacherId)) {
            return new BasicResponse<>(false, "המבחן לא נמצא או שאינו שלך");
        }
        List<TestQuestionEntity> prototypes = new ArrayList<>();
        int position = 0;
        for (TestQuestionEntity q : ExamAttemptService.orderedQuestions(source)) {
            TestQuestionEntity copy = examQuestionCodec.copy(q);
            copy.setPosition(position++);
            prototypes.add(copy);
        }
        if (prototypes.isEmpty()) {
            return new BasicResponse<>(false, "במבחן המקורי אין שאלות");
        }
        String sourceTitle = source.getTitle() != null ? source.getTitle() : source.getSubject() + " - " + source.getTopic();
        String title = isBlank(request.getTitle()) ? sourceTitle + " (עותק)" : request.getTitle().trim();
        int timeLimit = request.getTimeLimitMinutes() != null ? request.getTimeLimitMinutes() : source.getTimeLimitMinutes();
        ExamSpec spec = new ExamSpec(title, source.getSubject(), source.getTopic(), request.getStartTime(), timeLimit,
                request.getPublishAt(), request.getStudentIds(), request.isTemplate(), request.isVersions(),
                source.getShuffleQuestions(), source.getShuffleOptions(), source.getId());
        return materialize(source.getTeacher(), spec, prototypes);
    }

    @Transactional(readOnly = true)
    public BasicResponse<List<TeacherExamSummary>> listTeacherExams(String teacherId) {
        List<TeacherExamSummary> list = new ArrayList<>();
        for (TeacherTestEntity test : teacherTestRepository.findByTeacherUserId(teacherId)) {
            TeacherExamSummary summary = summaryMapper.apply(test);
            summary.setSubmittedCount(examAttemptRepository.countByTestIdAndStatusNot(test.getId(), ExamAttemptStatus.IN_PROGRESS));
            list.add(summary);
        }
        BasicResponse<List<TeacherExamSummary>> response = new BasicResponse<>(true, null);
        response.setData(list);
        return response;
    }

    /** Ids of scheduled exams whose publish time has come. */
    public List<Long> findDueForPublish() {
        return teacherTestRepository.findIdsDueForPublish(LocalDateTime.now());
    }

    /** Publishes one scheduled exam: makes it visible, notifies its students. Called by the scheduler. */
    @Transactional
    public void publishScheduled(Long testId) {
        TeacherTestEntity test = teacherTestRepository.findTeacherTestByTestId(testId);
        if (test == null || test.isPublishedNow() || test.isTemplateExam()) {
            return;
        }
        test.setPublished(true);
        teacherTestRepository.save(test);
        announce(test, test.getStudents());
    }

    // ---- internals ----

    private BasicResponse<List<TeacherExamSummary>> materialize(TeacherEntity teacher, ExamSpec spec,
                                                                List<TestQuestionEntity> prototypes) {
        if (spec.timeLimitMinutes() <= 0 || spec.timeLimitMinutes() > 600) {
            return new BasicResponse<>(false, "זמן המבחן חייב להיות בין 1 ל-600 דקות");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime publishAt = null;
        List<StudentEntity> students = new ArrayList<>();
        String startTime = isBlank(spec.startTime()) ? null : spec.startTime().trim();

        if (!spec.template()) {
            LocalDateTime start = parse(startTime);
            if (start == null) {
                return new BasicResponse<>(false, "יש לבחור מועד תקין למבחן");
            }
            if (!start.isAfter(now)) {
                return new BasicResponse<>(false, "לא ניתן לקבוע מבחן לזמן שכבר עבר");
            }
            if (!isBlank(spec.publishAt())) {
                publishAt = parse(spec.publishAt().trim());
                if (publishAt == null) {
                    return new BasicResponse<>(false, "מועד הפרסום אינו תקין");
                }
                if (publishAt.isAfter(start)) {
                    return new BasicResponse<>(false, "מועד הפרסום חייב להיות לפני תחילת המבחן");
                }
            }
            List<String> ids = spec.studentIds() == null ? List.of() : spec.studentIds().stream()
                    .filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
            if (ids.isEmpty()) {
                return new BasicResponse<>(false, "יש להזין לפחות תלמיד אחד");
            }
            students = new ArrayList<>(studentRepository.findStudentsByUserIds(ids));
            if (students.size() != ids.size()) {
                return new BasicResponse<>(false, "לא נמצאו " + (ids.size() - students.size()) + " מתוך תעודות הזהות שהוזנו");
            }
            students.sort(Comparator.comparing(StudentEntity::getUserId));
        } else if (startTime != null && parse(startTime) == null) {
            return new BasicResponse<>(false, "יש לבחור מועד תקין למבחן");
        }

        boolean publishNow = !spec.template() && (publishAt == null || !publishAt.isAfter(now));
        String groupId = UUID.randomUUID().toString();

        List<TeacherTestEntity> created = new ArrayList<>();
        if (spec.versions() && !spec.template() && students.size() >= 2) {
            List<StudentEntity> groupA = new ArrayList<>();
            List<StudentEntity> groupB = new ArrayList<>();
            for (int i = 0; i < students.size(); i++) {
                (i % 2 == 0 ? groupA : groupB).add(students.get(i));
            }
            created.add(saveExam(teacher, spec, startTime, publishAt, publishNow, groupId, "A", groupA,
                    prototypes, identity(prototypes.size())));
            created.add(saveExam(teacher, spec, startTime, publishAt, publishNow, groupId, "B", groupB,
                    prototypes, versionOrder(prototypes.size(), groupId)));
        } else {
            created.add(saveExam(teacher, spec, startTime, publishAt, publishNow, groupId, null, students,
                    prototypes, identity(prototypes.size())));
        }

        List<TeacherExamSummary> summaries = new ArrayList<>();
        for (TeacherTestEntity test : created) {
            if (publishNow) {
                announce(test, test.getStudents());
            }
            summaries.add(summaryMapper.apply(test));
        }
        String message;
        if (spec.template()) {
            message = "התבנית נשמרה";
        } else if (publishNow) {
            message = created.size() > 1 ? "נוצרו גרסאות A ו-B ונשלחו לתלמידים" : "המבחן נוצר ונשלח לתלמידים";
        } else {
            message = "המבחן נוצר ויפורסם ב-" + publishAt.format(FORMAT);
        }
        BasicResponse<List<TeacherExamSummary>> response = new BasicResponse<>(true, message);
        response.setData(summaries);
        return response;
    }

    private TeacherTestEntity saveExam(TeacherEntity teacher, ExamSpec spec, String startTime, LocalDateTime publishAt,
                                       boolean publishNow, String groupId, String versionLabel,
                                       List<StudentEntity> students, List<TestQuestionEntity> prototypes,
                                       List<Integer> order) {
        TeacherTestEntity test = new TeacherTestEntity();
        test.setTeacher(teacher);
        test.setTitle(spec.title());
        test.setSubject(spec.subject());
        test.setTopic(spec.topic());
        test.setDifficulty("מותאם אישית");
        test.setStartTime(startTime);
        test.setTimeLimitMinutes(spec.timeLimitMinutes());
        test.setQuestionCount(prototypes.size());
        test.setStudents(new ArrayList<>(students));
        test.setTemplate(spec.template());
        test.setPublished(publishNow);
        test.setPublishAt(publishAt);
        test.setVersionLabel(versionLabel);
        test.setExamGroupId(groupId);
        test.setSourceTestId(spec.sourceTestId());
        test.setShuffleQuestions(spec.shuffleQuestions() == null || spec.shuffleQuestions());
        test.setShuffleOptions(spec.shuffleOptions() == null || spec.shuffleOptions());
        teacherTestRepository.save(test);

        List<TestQuestionEntity> questions = new ArrayList<>();
        for (int position = 0; position < order.size(); position++) {
            TestQuestionEntity q = examQuestionCodec.copy(prototypes.get(order.get(position)));
            q.setPosition(position);
            q.setTest(test);
            questions.add(q);
        }
        questionRepository.saveAll(questions);
        test.getQuestions().addAll(questions);
        return test;
    }

    /** Sends NEW_TEST to each student and clears their cached tests table. */
    private void announce(TeacherTestEntity test, List<StudentEntity> students) {
        Cache cache = cacheManager.getCache("studentTestsStatus");
        for (StudentEntity student : students) {
            if (cache != null) {
                cache.evict(student.getUserId());
            }
            StudentTestStatusDTO status = new StudentTestStatusDTO(test.getId(), test.getSubject(), test.getTopic(),
                    -1, test.getStartTime(), test.getTimeLimitMinutes());
            notificationEventPublisher.publish(List.of(student.getUserId()),
                    new NotificationDTO<>(NotificationType.NEW_TEST, new NewTestMessageDTO(status)));
        }
    }

    private static List<Integer> identity(int size) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            order.add(i);
        }
        return order;
    }

    /** Version B's question order: a fixed permutation that differs from A's whenever possible. */
    static List<Integer> versionOrder(int size, String groupId) {
        List<Integer> order = identity(size);
        Collections.shuffle(order, new Random(groupId.hashCode()));
        if (size > 1 && order.equals(identity(size))) {
            Collections.rotate(order, 1);
        }
        return order;
    }

    private static LocalDateTime parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
