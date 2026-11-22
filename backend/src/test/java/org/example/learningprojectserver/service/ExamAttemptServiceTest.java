package org.example.learningprojectserver.service;

import org.example.learningprojectserver.dto.exam.ExamAttemptView;
import org.example.learningprojectserver.dto.exam.ExamQuestionInput;
import org.example.learningprojectserver.dto.exam.ExamResultView;
import org.example.learningprojectserver.entities.ExamAttemptEntity;
import org.example.learningprojectserver.entities.TeacherTestEntity;
import org.example.learningprojectserver.entities.TestQuestionEntity;
import org.example.learningprojectserver.enums.ExamAttemptStatus;
import org.example.learningprojectserver.repository.*;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.exam.ExamGrader;
import org.example.learningprojectserver.service.exam.ExamQuestionCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExamAttemptServiceTest {

    @Mock TeacherTestRepository teacherTestRepository;
    @Mock TeacherTestResultRepository teacherTestResultRepository;
    @Mock ExamAttemptRepository examAttemptRepository;
    @Mock ExamAttemptAnswerRepository examAttemptAnswerRepository;
    @Mock ExamIntegrityEventRepository examIntegrityEventRepository;
    @Mock StudentRepository studentRepository;
    @Mock CacheManager cacheManager;

    private ExamAttemptService service;
    private TeacherTestEntity test;

    @BeforeEach
    void setUp() {
        ExamQuestionCodec codec = new ExamQuestionCodec();
        service = new ExamAttemptService(teacherTestRepository, teacherTestResultRepository, examAttemptRepository,
                examAttemptAnswerRepository, examIntegrityEventRepository, studentRepository, codec,
                new ExamGrader(), cacheManager);

        test = new TeacherTestEntity();
        test.setId(5L);
        test.setTitle("בוחן");
        test.setTimeLimitMinutes(30);
        ExamQuestionInput in = new ExamQuestionInput();
        in.setKind("NUMERIC");
        in.setText("כמה זה 6*7?");
        in.setNumericAnswer(42.0);
        TestQuestionEntity q = codec.toEntity(in, "מתמטיקה", "כפל");
        q.setId(100L);
        q.setTest(test);
        test.getQuestions().add(q);

        when(teacherTestRepository.findTeacherTestByTestId(5L)).thenReturn(test);
        when(teacherTestRepository.isStudentAssigned(5L, "s1")).thenReturn(true);
        when(examAttemptRepository.save(any())).thenAnswer(inv -> {
            ExamAttemptEntity a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(900L);
            }
            return a;
        });
    }

    private ExamAttemptEntity attempt(LocalDateTime deadline) {
        ExamAttemptEntity a = new ExamAttemptEntity();
        a.setId(900L);
        a.setTest(test);
        a.setStudentUserId("s1");
        a.setStartedAt(deadline.minusMinutes(30));
        a.setDeadline(deadline);
        a.setStatus(ExamAttemptStatus.IN_PROGRESS);
        a.setSeed(1L);
        when(examAttemptRepository.findById(900L)).thenReturn(Optional.of(a));
        return a;
    }

    @Test
    void startRecordsServerDeadlineAndHidesTheKey() {
        test.setStartTime(LocalDateTime.now().minusMinutes(5).format(ExamAttemptService.START_FORMAT));

        BasicResponse<ExamAttemptView> response = service.startAttempt("s1", 5L);

        assertTrue(response.isSuccess());
        ExamAttemptView view = response.getData();
        assertTrue(view.getRemainingSeconds() > 23 * 60 && view.getRemainingSeconds() <= 25 * 60);
        assertEquals(1, view.getQuestions().size());
        assertEquals("NUMERIC", view.getQuestions().get(0).getKind());
        assertNull(view.getQuestions().get(0).getOptions());
    }

    @Test
    void startBeforeTheScheduledTimeIsRefused() {
        test.setStartTime(LocalDateTime.now().plusHours(1).format(ExamAttemptService.START_FORMAT));
        assertFalse(service.startAttempt("s1", 5L).isSuccess());
        verify(examAttemptRepository, never()).save(any());
    }

    @Test
    void unpublishedExamCannotStart() {
        test.setPublished(false);
        test.setStartTime(LocalDateTime.now().minusMinutes(1).format(ExamAttemptService.START_FORMAT));
        assertFalse(service.startAttempt("s1", 5L).isSuccess());
    }

    @Test
    void submitInTimeIsGraded() {
        ExamAttemptEntity a = attempt(LocalDateTime.now().plusMinutes(10));

        BasicResponse<ExamResultView> response = service.submitAttempt("s1", 900L, Map.of(100L, "42"));

        assertTrue(response.isSuccess());
        assertEquals(100, response.getData().getScore());
        assertEquals(ExamAttemptStatus.SUBMITTED, a.getStatus());
    }

    @Test
    void lateSubmissionIsRejectedAndGradedFromSavedAnswers() {
        ExamAttemptEntity a = attempt(LocalDateTime.now().minusMinutes(2));
        a.setSavedAnswersJson("{\"100\":\"41\"}");

        BasicResponse<ExamResultView> response = service.submitAttempt("s1", 900L, Map.of(100L, "42"));

        assertFalse(response.isSuccess());
        assertEquals(0, response.getData().getScore());
        assertTrue(response.getData().isAutoSubmitted());
        assertEquals(ExamAttemptStatus.AUTO_SUBMITTED, a.getStatus());
    }

    @Test
    void anotherStudentCannotSubmitTheAttempt() {
        attempt(LocalDateTime.now().plusMinutes(10));
        assertFalse(service.submitAttempt("intruder", 900L, Map.of(100L, "42")).isSuccess());
    }

    @Test
    void tabHiddenEventsAreCounted() {
        ExamAttemptEntity a = attempt(LocalDateTime.now().plusMinutes(10));
        service.recordIntegrityEvent("s1", 900L, "TAB_HIDDEN");
        service.recordIntegrityEvent("s1", 900L, "TAB_VISIBLE");
        service.recordIntegrityEvent("s1", 900L, "TAB_HIDDEN");
        assertEquals(2, a.getTabLeaveCount());
        assertFalse(service.recordIntegrityEvent("s1", 900L, "SOMETHING").isSuccess());
        verify(examIntegrityEventRepository, times(3)).save(any());
    }

    @Test
    void expiredAttemptIsAutoSubmittedByTheJob() {
        ExamAttemptEntity a = attempt(LocalDateTime.now().minusMinutes(5));
        a.setSavedAnswersJson("{\"100\":\"42\"}");
        service.autoSubmitExpired(900L);
        assertEquals(ExamAttemptStatus.AUTO_SUBMITTED, a.getStatus());
        assertEquals(100, a.getScore());
        verify(examAttemptAnswerRepository).saveAll(any());
        verify(teacherTestResultRepository).save(any());
    }
}
