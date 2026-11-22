package org.example.learningprojectserver.service.exam;

import org.example.learningprojectserver.dto.exam.ExamQuestionInput;
import org.example.learningprojectserver.entities.TestQuestionEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExamGraderTest {

    private final ExamQuestionCodec codec = new ExamQuestionCodec();
    private final ExamGrader grader = new ExamGrader();

    private TestQuestionEntity build(ExamQuestionInput in) {
        assertNull(codec.validate(in, 1));
        TestQuestionEntity q = codec.toEntity(in, "מתמטיקה", "כללי");
        q.setId(7L);
        return q;
    }

    private static ExamQuestionInput input(String kind) {
        ExamQuestionInput in = new ExamQuestionInput();
        in.setKind(kind);
        in.setText("שאלה");
        return in;
    }

    @Test
    void multiSelectNeedsExactlyTheCorrectSet() {
        ExamQuestionInput in = input("MULTI_SELECT");
        in.setOptions(List.of("2", "3", "4", "6"));
        in.setCorrectOptions(List.of(0, 3));
        TestQuestionEntity q = build(in);

        assertTrue(grader.isCorrect(q, "[3,0]"));
        assertFalse(grader.isCorrect(q, "[0]"));
        assertFalse(grader.isCorrect(q, "[0,3,1]"));
        assertFalse(grader.isCorrect(q, "[0,0,3]"));
        assertFalse(grader.isCorrect(q, "not json"));
        assertFalse(grader.isCorrect(q, ""));
    }

    @Test
    void numericAcceptsToleranceFractionsAndLatex() {
        ExamQuestionInput in = input("NUMERIC");
        in.setNumericAnswer(3.5);
        in.setTolerance(0.01);
        TestQuestionEntity q = build(in);

        assertTrue(grader.isCorrect(q, "3.5"));
        assertTrue(grader.isCorrect(q, "3,505"));
        assertTrue(grader.isCorrect(q, "7/2"));
        assertTrue(grader.isCorrect(q, "\\frac{7}{2}"));
        assertTrue(grader.isCorrect(q, "3{,}5"));
        assertFalse(grader.isCorrect(q, "3.52"));
        assertFalse(grader.isCorrect(q, "abc"));
    }

    @Test
    void numericWithoutToleranceIsExact() {
        ExamQuestionInput in = input("NUMERIC");
        in.setNumericAnswer(-0.5);
        TestQuestionEntity q = build(in);

        assertTrue(grader.isCorrect(q, "-\\frac{1}{2}"));
        assertTrue(grader.isCorrect(q, "−0.5"));
        assertFalse(grader.isCorrect(q, "-0.51"));
    }

    @Test
    void matchingComparesEveryPair() {
        ExamQuestionInput in = input("MATCHING");
        in.setMatchLeft(List.of("2+2", "3*3"));
        in.setMatchRight(List.of("9", "4"));
        in.setMatchKey(List.of(1, 0));
        TestQuestionEntity q = build(in);

        assertTrue(grader.isCorrect(q, "[1,0]"));
        assertFalse(grader.isCorrect(q, "[0,1]"));
        assertFalse(grader.isCorrect(q, "[1]"));
    }

    @Test
    void orderingNeedsTheAuthoredOrder() {
        ExamQuestionInput in = input("ORDERING");
        in.setOptions(List.of("1", "2", "3"));
        TestQuestionEntity q = build(in);

        assertTrue(grader.isCorrect(q, "[0,1,2]"));
        assertFalse(grader.isCorrect(q, "[1,0,2]"));
        assertFalse(grader.isCorrect(q, "[0,1]"));
    }

    @Test
    void shortOpenNormalisesSpacingAndLatex() {
        ExamQuestionInput in = input("SHORT_OPEN");
        in.setAcceptedAnswers(List.of("x^2+1", "ירושלים"));
        TestQuestionEntity q = build(in);

        assertTrue(grader.isCorrect(q, " x^2 + 1 "));
        assertTrue(grader.isCorrect(q, "\\left x^2+1\\right"));
        assertTrue(grader.isCorrect(q, "ירושלים."));
        assertFalse(grader.isCorrect(q, "x^2"));
    }

    @Test
    void legacyTextQuestionUsesTheAnswerColumn() {
        TestQuestionEntity q = new TestQuestionEntity();
        q.setAnswer("12");
        assertTrue(grader.isCorrect(q, " 12 "));
        assertFalse(grader.isCorrect(q, "13"));
    }

    @Test
    void invalidInputsAreRejectedWithHebrewMessages() {
        ExamQuestionInput in = input("MULTI_SELECT");
        in.setOptions(List.of("א", " "));
        in.setCorrectOptions(List.of(0));
        assertNotNull(codec.validate(in, 2));

        ExamQuestionInput unknown = input("ESSAY");
        assertTrue(codec.validate(unknown, 1).contains("סוג שאלה"));

        ExamQuestionInput numeric = input("NUMERIC");
        assertNotNull(codec.validate(numeric, 1));
    }
}
