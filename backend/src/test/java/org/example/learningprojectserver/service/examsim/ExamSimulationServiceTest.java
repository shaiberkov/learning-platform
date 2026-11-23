package org.example.learningprojectserver.service.examsim;

import org.example.learningprojectserver.dto.examsim.ExamSimDTOs.*;
import org.example.learningprojectserver.entities.*;
import org.example.learningprojectserver.enums.ExamAttemptStatus;
import org.example.learningprojectserver.mappers.PastExamQuestionEntityToSimQuestionViewMapper;
import org.example.learningprojectserver.repository.ExamSimAttemptRepository;
import org.example.learningprojectserver.repository.PastExamRepository;
import org.example.learningprojectserver.repository.TopicMasteryRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.MasteryService;
import org.example.learningprojectserver.service.adaptive.SkillCatalog;
import org.example.learningprojectserver.service.exam.ExamQuestionCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExamSimulationServiceTest {

    @Mock PastExamRepository pastExamRepository;
    @Mock ExamSimAttemptRepository attemptRepository;
    @Mock TopicMasteryRepository topicMasteryRepository;
    @Mock MasteryService masteryService;

    private ExamSimulationService service;
    private PastExamEntity exam;
    private long nextId = 100;

    @BeforeEach
    void setUp() {
        service = new ExamSimulationService(pastExamRepository, attemptRepository, topicMasteryRepository,
                masteryService, new PastExamQuestionEntityToSimQuestionViewMapper());
        exam = new PastExamEntity();
        exam.setId(1L);
        exam.setTitle("מבחן");
        exam.setSubject(SkillCatalog.MATH);
        exam.setDurationMinutes(60);
        exam.getSections().add(section(1, null, q(11, "x = 2, y = 3", 20), q(12, "7", 20)));
        exam.getSections().add(section(2, 1, q(21, "(1, 2)", 30), q(22, "4", 30)));
        when(pastExamRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(attemptRepository.findByStudent(any())).thenReturn(List.of());
        when(attemptRepository.findByStudentExamAndStatus(any(), any(), any())).thenReturn(List.of());
        when(attemptRepository.save(any())).thenAnswer(inv -> {
            ExamSimAttemptEntity a = inv.getArgument(0);
            if (a.getId() == null) a.setId(nextId++);
            return a;
        });
    }

    @Test
    void startHidesAnswersAndSetsDeadlineAndPrediction() {
        BasicResponse<SimAttemptView> res = service.start("s1", 1L);
        assertTrue(res.isSuccess());
        SimAttemptView view = res.getData();
        assertEquals(2, view.sections().size());
        assertEquals("x = □, y = □", view.sections().get(0).questions().get(0).answerHint());
        assertNull(view.sections().get(0).questions().get(1).answerHint());
        assertTrue(view.remainingSeconds() > 59 * 60);
        assertNotNull(view.predictedScore());
    }

    @Test
    void submitGradesByRubricWithPartialCreditAndBestOfSection() {
        ExamSimAttemptEntity attempt = openAttempt("s1", LocalDateTime.now().plusMinutes(30));
        // Q11 half right (10/20), Q12 right (20), section 2 counts the best one: Q22 right (30).
        BasicResponse<SimResultView> res = service.submit("s1", attempt.getId(),
                Map.of(11L, "y=3, x=5", 12L, "7", 21L, "(2, 1)", 22L, "4.0"));
        assertTrue(res.isSuccess());
        SimResultView r = res.getData();
        // earned 10 + 20 + 30 = 60 of possible 40 + 30 = 70
        assertEquals(86, r.score());
        assertEquals("PARTIAL", r.sections().get(0).questions().get(0).status());
        assertFalse(r.sections().get(1).questions().get(0).counted());
        assertEquals(ExamAttemptStatus.SUBMITTED, attempt.getStatus());
        assertEquals(86, attempt.getScore());
        verify(masteryService, atLeastOnce()).recordAnswer(eq("s1"), eq(SkillCatalog.MATH), any(), any(), any(), anyDouble(), anyBoolean());
    }

    @Test
    void anotherStudentCannotTouchTheAttempt() {
        ExamSimAttemptEntity attempt = openAttempt("s1", LocalDateTime.now().plusMinutes(30));
        assertFalse(service.submit("s2", attempt.getId(), Map.of(12L, "7")).isSuccess());
        assertFalse(service.save("s2", attempt.getId(), Map.of(12L, "7")).isSuccess());
        assertFalse(service.result("s2", attempt.getId()).isSuccess());
        assertEquals(ExamAttemptStatus.IN_PROGRESS, attempt.getStatus());
    }

    @Test
    void lateSubmissionIsGradedFromSavedAnswersOnly() {
        ExamSimAttemptEntity attempt = openAttempt("s1", LocalDateTime.now().minusMinutes(5));
        attempt.setSavedAnswersJson(ExamQuestionCodec.write(Map.of(12L, "7")));
        BasicResponse<SimResultView> res = service.submit("s1", attempt.getId(), Map.of(11L, "x = 2, y = 3"));
        assertFalse(res.isSuccess());
        assertEquals(ExamAttemptStatus.AUTO_SUBMITTED, attempt.getStatus());
        assertEquals(29, res.getData().score()); // only Q12: 20 of 70
    }

    @Test
    void listFinalizesExpiredAttemptsAndReportsBestScore() {
        ExamSimAttemptEntity expired = openAttempt("s1", LocalDateTime.now().minusHours(1));
        expired.setSavedAnswersJson(ExamQuestionCodec.write(Map.of(12L, "7", 22L, "4")));
        when(attemptRepository.findByStudent("s1")).thenReturn(List.of(expired));
        when(pastExamRepository.findAllByOrderByExamYearDescIdAsc()).thenReturn(List.of(exam));

        PastExamSummaryDTO summary = service.listExams("s1").getData().get(0);
        assertEquals(ExamAttemptStatus.AUTO_SUBMITTED, expired.getStatus());
        assertEquals(71, summary.bestScore());
        assertEquals(4, summary.questionCount());
        assertNull(summary.inProgressAttemptId());
    }

    @Test
    void abandonedAttemptWithoutAnswersIsDiscarded() {
        ExamSimAttemptEntity abandoned = openAttempt("s1", LocalDateTime.now().minusHours(1));
        when(attemptRepository.findByStudent("s1")).thenReturn(List.of(abandoned));
        when(pastExamRepository.findAllByOrderByExamYearDescIdAsc()).thenReturn(List.of(exam));

        PastExamSummaryDTO summary = service.listExams("s1").getData().get(0);
        verify(attemptRepository).delete(abandoned);
        assertEquals(0, summary.attemptsCount());
        assertNull(summary.bestScore());
    }

    @Test
    void predictionMessageUsesExpectedAndMargin() {
        ScorePredictionDTO p = service.predict("s1", 1L).getData();
        assertEquals("לפי הביצועים שלך, הציון הצפוי הוא " + p.expectedScore() + " ± " + p.margin(), p.message());
        assertEquals(0, p.masteryCoverage());
        assertFalse(service.predict("s1", 99L).isSuccess());
    }

    private ExamSimAttemptEntity openAttempt(String userId, LocalDateTime deadline) {
        ExamSimAttemptEntity a = new ExamSimAttemptEntity();
        a.setId(nextId++);
        a.setExam(exam);
        a.setStudentUserId(userId);
        a.setStartedAt(deadline.minusMinutes(60));
        a.setDeadline(deadline);
        a.setStatus(ExamAttemptStatus.IN_PROGRESS);
        when(attemptRepository.findById(a.getId())).thenReturn(Optional.of(a));
        return a;
    }

    private PastExamSectionEntity section(int position, Integer toCount, PastExamQuestionEntity... questions) {
        PastExamSectionEntity s = new PastExamSectionEntity();
        s.setPosition(position);
        s.setTitle("פרק " + position);
        s.setQuestionsToCount(toCount);
        s.setExam(exam);
        for (PastExamQuestionEntity q : questions) {
            q.setSection(s);
            q.setPosition(s.getQuestions().size() + 1);
            s.getQuestions().add(q);
        }
        return s;
    }

    private static PastExamQuestionEntity q(long id, String answer, int points) {
        PastExamQuestionEntity q = new PastExamQuestionEntity();
        q.setId(id);
        q.setText("שאלה " + id);
        q.setAnswer(answer);
        q.setPoints(points);
        q.setLevel(3);
        q.setTopic("משוואות");
        q.setSubTopic("מישוואה עם נעלם אחד");
        return q;
    }
}
