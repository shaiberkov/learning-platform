package org.example.learningprojectserver.service;

import org.example.learningprojectserver.dto.AdaptiveDTOs.*;
import org.example.learningprojectserver.entities.QuestionEntity;
import org.example.learningprojectserver.entities.ReviewItemEntity;
import org.example.learningprojectserver.entities.TestAnswerRecordEntity;
import org.example.learningprojectserver.entities.TestQuestionEntity;
import org.example.learningprojectserver.entities.TestEntity;
import org.example.learningprojectserver.repository.ReviewItemRepository;
import org.example.learningprojectserver.repository.TestAnswerRecordRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdaptiveServicesTest {

    private static QuestionEntity question(long id, String subTopic, String answer) {
        QuestionEntity q = new QuestionEntity("מתמטיקה", "מספרים שלמים", subTopic, "1+1=?", answer);
        q.setId(id);
        return q;
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Review {
        @Mock ReviewItemRepository reviewItemRepository;
        @InjectMocks ReviewService reviewService;

        @Test
        void wrongAnswerEntersNotebookDueToday() {
            when(reviewItemRepository.findByStudentUserIdAndQuestionId("s1", 7L)).thenReturn(Optional.empty());
            reviewService.addMistake("s1", question(7L, "חיבור מספרים שלמים", "2"), "PRACTICE", "3");

            ArgumentCaptor<ReviewItemEntity> saved = ArgumentCaptor.forClass(ReviewItemEntity.class);
            verify(reviewItemRepository).save(saved.capture());
            assertEquals(ReviewService.today(), saved.getValue().getDueDate());
            assertEquals("2", saved.getValue().getAnswer());
            assertEquals("מספרים שלמים", saved.getValue().getTopic());
        }

        @Test
        void correctReviewPushesItemToTomorrow() {
            ReviewItemEntity item = new ReviewItemEntity();
            item.setId(3L);
            item.setStudentUserId("s1");
            item.setAnswer("2");
            item.setDueDate(ReviewService.today());
            when(reviewItemRepository.findByIdAndStudentUserId(3L, "s1")).thenReturn(Optional.of(item));

            BasicResponse response = reviewService.answer("s1", 3L, " 2 ");

            assertTrue(response.isSuccess());
            ReviewAnswerDTO dto = (ReviewAnswerDTO) response.getData();
            assertTrue(dto.correct());
            assertEquals(ReviewService.today().plusDays(1), dto.nextDueDate());
        }

        @Test
        void itemNotDueYetIsRejected() {
            ReviewItemEntity item = new ReviewItemEntity();
            item.setAnswer("2");
            item.setDueDate(ReviewService.today().plusDays(3));
            when(reviewItemRepository.findByIdAndStudentUserId(3L, "s1")).thenReturn(Optional.of(item));

            assertFalse(reviewService.answer("s1", 3L, "2").isSuccess());
            verify(reviewItemRepository, never()).save(any());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Feedback {
        @Mock TestAnswerRecordRepository testAnswerRecordRepository;
        @Mock ReviewService reviewService;
        @Mock MasteryService masteryService;
        @InjectMocks ExamFeedbackService examFeedbackService;

        private TestAnswerRecordEntity record(long qid, String subTopic, boolean correct, Integer seconds) {
            TestAnswerRecordEntity r = new TestAnswerRecordEntity();
            r.setTestId(10L);
            r.setStudentUserId("s1");
            r.setClassRoomId(4L);
            r.setQuestionId(qid);
            r.setSubject("מתמטיקה");
            r.setTopic("מספרים שלמים");
            r.setSubTopic(subTopic);
            r.setCorrect(correct);
            r.setSecondsSpent(seconds);
            r.setSource(ExamFeedbackService.PRACTICE_TEST);
            return r;
        }

        @Test
        void recordingAnExamQueuesMistakesAndUpdatesMastery() {
            TestEntity test = new TestEntity();
            test.setId(10L);
            TestQuestionEntity right = new TestQuestionEntity();
            right.setId(1L); right.setSubject("מתמטיקה"); right.setTopic("מספרים שלמים");
            right.setSubTopic("חיבור מספרים שלמים"); right.setAnswer("5"); right.setDifficultyLevel(2);
            TestQuestionEntity wrong = new TestQuestionEntity();
            wrong.setId(2L); wrong.setSubject("מתמטיקה"); wrong.setTopic("מספרים שלמים");
            wrong.setSubTopic("כפל מספרים שלמים"); wrong.setAnswer("12"); wrong.setDifficultyLevel(2);
            test.setQuestions(List.of(right, wrong));
            when(testAnswerRecordRepository.existsByTestIdAndStudentUserId(10L, "s1")).thenReturn(false);

            examFeedbackService.recordExamAnswers("s1", test, ExamFeedbackService.PRACTICE_TEST, Map.of(1L, "5", 2L, "13"));

            verify(reviewService).addMistake("s1", wrong, ExamFeedbackService.PRACTICE_TEST, "13");
            verify(reviewService, never()).addMistake(eq("s1"), eq(right), anyString(), any());
            verify(masteryService, times(2)).recordAnswer(eq("s1"), anyString(), anyString(), anyString(), eq(2), anyDouble(), anyBoolean());
            verify(testAnswerRecordRepository).saveAll(argThat(list -> ((List<?>) list).size() == 2));
        }

        @Test
        void examIsRecordedOnlyOnce() {
            TestEntity test = new TestEntity();
            test.setId(10L);
            when(testAnswerRecordRepository.existsByTestIdAndStudentUserId(10L, "s1")).thenReturn(true);
            examFeedbackService.recordExamAnswers("s1", test, ExamFeedbackService.PRACTICE_TEST, Map.of());
            verifyNoInteractions(reviewService, masteryService);
            verify(testAnswerRecordRepository, never()).saveAll(any());
        }

        @Test
        void feedbackBreaksDownByTopicAndRecommendsWeakest() {
            when(testAnswerRecordRepository.findByTestIdAndStudentUserIdOrderByIdAsc(10L, "s1")).thenReturn(List.of(
                    record(1, "חיבור מספרים שלמים", true, 20),
                    record(2, "חיבור מספרים שלמים", true, 40),
                    record(3, "כפל מספרים שלמים", false, 90),
                    record(4, "כפל מספרים שלמים", true, 30)));
            List<Object[]> classRows = new ArrayList<>();
            classRows.add(new Object[]{"כפל מספרים שלמים", 6L, 8L, 4L});      // 4 classmates: shown
            classRows.add(new Object[]{"חיבור מספרים שלמים", 1L, 2L, 2L});    // 2 classmates: hidden
            when(testAnswerRecordRepository.classAccuracyBySubTopic(eq(4L), eq("s1"), any())).thenReturn(classRows);

            BasicResponse response = examFeedbackService.getFeedback("s1", 10L);

            ExamFeedbackDTO dto = (ExamFeedbackDTO) response.getData();
            assertEquals(75, dto.score());
            assertEquals(180, dto.totalSeconds());
            assertNull(dto.classAverageScore());
            TopicBreakdownDTO addition = dto.topics().get(0);
            assertEquals(100, addition.percent());
            assertEquals(30, addition.averageSeconds());
            assertNull(addition.classAveragePercent());
            TopicBreakdownDTO multiplication = dto.topics().get(1);
            assertEquals(50, multiplication.percent());
            assertEquals(75, multiplication.classAveragePercent());
            assertEquals("כפל מספרים שלמים", dto.recommendation().subTopic());
        }

        @Test
        void noRecordsGivesHebrewError() {
            when(testAnswerRecordRepository.findByTestIdAndStudentUserIdOrderByIdAsc(10L, "s1")).thenReturn(List.of());
            BasicResponse response = examFeedbackService.getFeedback("s1", 10L);
            assertFalse(response.isSuccess());
            assertNotNull(response.getErrorCode());
        }
    }
}
