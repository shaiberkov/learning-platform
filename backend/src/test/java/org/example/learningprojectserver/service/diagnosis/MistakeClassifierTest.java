package org.example.learningprojectserver.service.diagnosis;

import org.example.learningprojectserver.enums.MistakeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MistakeClassifierTest {

    private static MistakeType type(String question, String expected, String given) {
        return MistakeClassifier.classify(question, expected, given).type();
    }

    @Test
    void signErrorIsCalculation() {
        assertEquals(MistakeType.CALCULATION, type("7-12=?", "-5", "5"));
    }

    @Test
    void smallSlipIsCalculation() {
        assertEquals(MistakeType.CALCULATION, type("6*7=?", "42", "41"));
    }

    @Test
    void swappedDigitsAreCalculation() {
        assertEquals(MistakeType.CALCULATION, type("9*8=?", "72", "27"));
    }

    @Test
    void shiftedDecimalPointIsCalculation() {
        assertEquals(MistakeType.CALCULATION, type("2.5*3=?", "7.5", "75"));
    }

    @Test
    void numberCopiedFromQuestionIsMisread() {
        assertEquals(MistakeType.MISREAD, type("35+48=?", "83", "48"));
    }

    @Test
    void sameValuesInWrongOrderAreMisread() {
        assertEquals(MistakeType.MISREAD, type("x+y=7, x-y=1", "4,3", "3,4"));
    }

    @Test
    void rightValueInAnotherFormatIsMisread() {
        assertEquals(MistakeType.MISREAD, type("1/2+1/4=?", "3/4", "0.75"));
        assertEquals(MistakeType.MISREAD, type("2x=8", "x=4", "x = 4"));
    }

    @Test
    void farOffNumberIsConceptual() {
        assertEquals(MistakeType.CONCEPTUAL, type("1/2+1/3=?", "5/6", "2/5"));
    }

    @Test
    void correctAnswerAfterFullSolutionIsConceptual() {
        assertEquals(MistakeType.CONCEPTUAL, type("3*4=?", "12", "12"));
    }

    @Test
    void blankOrNonNumericStaysUnclassified() {
        assertEquals(MistakeType.UNCLASSIFIED, type("3*4=?", "12", " "));
        assertEquals(MistakeType.UNCLASSIFIED, type("?", "חיובי", "שלילי"));
    }
}
