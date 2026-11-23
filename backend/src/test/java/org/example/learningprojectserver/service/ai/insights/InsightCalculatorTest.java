package org.example.learningprojectserver.service.ai.insights;

import org.example.learningprojectserver.dto.ai.InsightStatsDTO;
import org.example.learningprojectserver.entities.TestAnswerRecordEntity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsightCalculatorTest {

    private static TestAnswerRecordEntity answer(String student, long questionId, String subTopic, String given, boolean correct) {
        TestAnswerRecordEntity r = new TestAnswerRecordEntity();
        r.setStudentUserId(student);
        r.setQuestionId(questionId);
        r.setSubTopic(subTopic);
        r.setQuestionText("שאלה " + questionId);
        r.setCorrectAnswer("10");
        r.setGivenAnswer(given);
        r.setCorrect(correct);
        return r;
    }

    @Test
    void studentBelowHalfInATopicIsStruggling() {
        List<TestAnswerRecordEntity> records = List.of(
                answer("s1", 1, "שברים", "1", false),
                answer("s1", 2, "שברים", "2", false),
                answer("s1", 3, "שברים", "10", true),
                answer("s2", 1, "שברים", "10", true),
                answer("s2", 2, "שברים", "10", true),
                answer("s2", 3, "שברים", "10", true));

        InsightStatsDTO stats = InsightCalculator.compute(records, Map.of("s1", "דנה"));

        assertEquals(1, stats.getStrugglingStudents().size());
        InsightStatsDTO.StrugglingStudent s = stats.getStrugglingStudents().get(0);
        assertEquals("דנה", s.getStudentName());
        assertEquals("S1", s.getLabel());
        assertEquals(33, s.getAccuracyPct());
        assertEquals(2, stats.getStudentCount());
    }

    @Test
    void questionWhereMostChoseTheSameWrongAnswerIsSuspicious() {
        List<TestAnswerRecordEntity> records = new ArrayList<>();
        records.add(answer("a", 7, "אחוזים", "12", false));
        records.add(answer("b", 7, "אחוזים", " 12 ", false));
        records.add(answer("c", 7, "אחוזים", "12", false));
        records.add(answer("d", 7, "אחוזים", "10", true));
        records.add(answer("e", 7, "אחוזים", "", false)); // blank answers do not count

        InsightStatsDTO stats = InsightCalculator.compute(records, Map.of());

        assertEquals(1, stats.getSuspiciousQuestions().size());
        InsightStatsDTO.SuspiciousQuestion q = stats.getSuspiciousQuestions().get(0);
        assertEquals("12", q.getCommonWrongAnswer());
        assertEquals(3, q.getCommonWrongCount());
        assertEquals(4, q.getAnswered());
        assertEquals(75, q.getCommonWrongPct());
    }

    @Test
    void scatteredWrongAnswersAreNotSuspicious() {
        List<TestAnswerRecordEntity> records = List.of(
                answer("a", 8, "אחוזים", "1", false),
                answer("b", 8, "אחוזים", "2", false),
                answer("c", 8, "אחוזים", "3", false),
                answer("d", 8, "אחוזים", "4", false));

        assertTrue(InsightCalculator.compute(records, Map.of()).getSuspiciousQuestions().isEmpty());
    }

    @Test
    void anonymizedFactsNeverContainNamesOrIds() {
        List<TestAnswerRecordEntity> records = List.of(
                answer("325256022", 1, "שברים", "1", false),
                answer("325256022", 2, "שברים", "2", false),
                answer("325256022", 3, "שברים", "3", false));
        InsightStatsDTO stats = InsightCalculator.compute(records, Map.of("325256022", "יוסי כהן"));

        String facts = TeacherInsightService.anonymizedFacts(stats);

        assertTrue(facts.contains("S1"));
        assertTrue(!facts.contains("יוסי") && !facts.contains("325256022"));
    }
}
