package org.example.learningprojectserver.service.examsim;

import org.example.learningprojectserver.entities.PastExamEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** ExamSimGrader (rubric grading), ScorePredictor and the sample exams built by PastExamSeeder. */
class ExamSimLogicTest {

    @Test
    void gradesPartsInAnyOrderAndNumbersByValue() {
        assertEquals(1.0, ExamSimGrader.fraction("x = 2, y = 3", "y=3 , x=2"));
        assertEquals(0.5, ExamSimGrader.fraction("x = 2, y = 3", "x = 2, y = 4"));
        assertEquals(1.0, ExamSimGrader.fraction("(3, -2)", "(3,-2)"));
        assertEquals(0.0, ExamSimGrader.fraction("(3, -2)", "(-2, 3)"));
        assertEquals(1.0, ExamSimGrader.fraction("1/2", "0.5"));
        assertEquals(1.0, ExamSimGrader.fraction("7", "\\frac{14}{2}"));
        assertEquals(0.0, ExamSimGrader.fraction("7", ""));
        assertEquals(0.5, ExamSimGrader.fraction("x = 2, y = 3", "x = 2, x = 2")); // one given part matches one expected part
        assertEquals(10.0, ExamSimGrader.points("x = 2, y = 3", "x=2", 20));
    }

    @Test
    void keepsTheBestQuestionsOfAChoiceSection() {
        assertEquals(Set.of(1, 2), ExamSimGrader.countedIndexes(List.of(5.0, 20.0, 10.0), 2));
        assertEquals(Set.of(0, 1, 2), ExamSimGrader.countedIndexes(List.of(5.0, 20.0, 10.0), null));
        assertEquals(40, ExamSimGrader.possiblePoints(List.of(20, 20, 10), 2));
        assertEquals(50, ExamSimGrader.possiblePoints(List.of(20, 20, 10), null));
        assertEquals("(□, □)", ExamSimGrader.answerHint("(3, -2)"));
        assertNull(ExamSimGrader.answerHint("-12"));
    }

    @Test
    void predictionFollowsMasteryAndPastScores() {
        List<ScorePredictor.Section> unknown = List.of(new ScorePredictor.Section(List.of(
                new ScorePredictor.Question(50, 3, null), new ScorePredictor.Question(50, 3, null)), null));
        ScorePredictor.Prediction fresh = ScorePredictor.predict(unknown, List.of());
        assertEquals(0, fresh.coverage());

        List<ScorePredictor.Section> strong = List.of(new ScorePredictor.Section(List.of(
                new ScorePredictor.Question(50, 3, 1600.0), new ScorePredictor.Question(50, 3, 1600.0)), null));
        ScorePredictor.Prediction good = ScorePredictor.predict(strong, List.of());
        assertTrue(good.expected() > fresh.expected());
        assertEquals(100, good.coverage());
        assertTrue(good.margin() >= ScorePredictor.MIN_MARGIN && good.margin() <= ScorePredictor.MAX_MARGIN);

        // Many consistent past results pull the estimate toward them and narrow the margin.
        ScorePredictor.Prediction withHistory = ScorePredictor.predict(unknown, List.of(82, 80, 84, 82, 81, 83, 82, 82));
        assertTrue(Math.abs(withHistory.expected() - 82) < Math.abs(fresh.expected() - 82));
        assertTrue(withHistory.margin() < fresh.margin());
    }

    @Test
    void seededExamsHaveARubricAndFullPoints() {
        PastExamEntity exam = PastExamSeeder.build("t", 2025, "s", "d", 60, List.of(
                new PastExamSeeder.SectionSpec("א", "i", null, List.of(
                        new PastExamSeeder.Spec("מערכת משוואות עם שני נעלמים", 3, 20),
                        new PastExamSeeder.Spec("כפל מספרים שלמים", 2, 30))),
                new PastExamSeeder.SectionSpec("ב", "i", 1, List.of(
                        new PastExamSeeder.Spec("נקודת חיתוך בין שני ישרים", 3, 50),
                        new PastExamSeeder.Spec("חיבור שברים", 2, 50)))));
        assertEquals(2, exam.getSections().size());
        exam.getSections().forEach(s -> s.getQuestions().forEach(q -> {
            assertNotNull(q.getRubric());
            assertFalse(q.getAnswer().isBlank());
            assertEquals(1.0, ExamSimGrader.fraction(q.getAnswer(), q.getAnswer()));
        }));
        assertTrue(exam.getSections().get(0).getQuestions().get(0).getRubric().contains("ניקוד חלקי"));
        assertEquals("נקודת חיתוך בין שני ישרים", exam.getSections().get(1).getQuestions().get(0).getSubTopic());
    }
}
