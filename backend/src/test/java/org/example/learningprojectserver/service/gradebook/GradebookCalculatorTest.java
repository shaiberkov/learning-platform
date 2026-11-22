package org.example.learningprojectserver.service.gradebook;

import org.example.learningprojectserver.enums.GradeCategory;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GradebookCalculatorTest {

    private static final Map<GradeCategory, Integer> WEIGHTS_60_40 = Map.of(
            GradeCategory.EXAM, 60, GradeCategory.HOMEWORK, 40, GradeCategory.QUIZ, 0, GradeCategory.PROJECT, 0);

    @Test
    void weightsCategoryAveragesSixtyForty() {
        List<GradebookCalculator.ColumnSpec> columns = List.of(
                new GradebookCalculator.ColumnSpec("e1", GradeCategory.EXAM, 100),
                new GradebookCalculator.ColumnSpec("e2", GradeCategory.EXAM, 100),
                new GradebookCalculator.ColumnSpec("h1", GradeCategory.HOMEWORK, 10));
        Map<String, Map<String, Double>> grades = Map.of(
                "e1", Map.of("s1", 80.0),
                "e2", Map.of("s1", 100.0),
                "h1", Map.of("s1", 5.0));

        GradebookCalculator.StudentSummary s1 = GradebookCalculator.compute(List.of("s1"), columns, grades, WEIGHTS_60_40).get("s1");

        assertEquals(90.0, s1.categoryAverages().get("EXAM"));
        assertEquals(50.0, s1.categoryAverages().get("HOMEWORK"));
        assertEquals(74.0, s1.finalGrade()); // 0.6*90 + 0.4*50
    }

    @Test
    void missingCategoryWeightIsSpreadOverTheRest() {
        List<GradebookCalculator.ColumnSpec> columns = List.of(
                new GradebookCalculator.ColumnSpec("e1", GradeCategory.EXAM, 100),
                new GradebookCalculator.ColumnSpec("h1", GradeCategory.HOMEWORK, 100));
        Map<String, Map<String, Double>> grades = Map.of("e1", Map.of("s1", 70.0));

        Map<String, GradebookCalculator.StudentSummary> result =
                GradebookCalculator.compute(List.of("s1", "s2"), columns, grades, WEIGHTS_60_40);

        assertEquals(70.0, result.get("s1").finalGrade());
        assertNull(result.get("s2").finalGrade());
        assertTrue(result.get("s2").categoryAverages().isEmpty());
    }

    @Test
    void zeroWeightCategoryIsShownButNotCounted() {
        List<GradebookCalculator.ColumnSpec> columns = List.of(
                new GradebookCalculator.ColumnSpec("q1", GradeCategory.QUIZ, 100),
                new GradebookCalculator.ColumnSpec("h1", GradeCategory.HOMEWORK, 100));
        Map<String, Map<String, Double>> grades = Map.of(
                "q1", Map.of("s1", 20.0),
                "h1", Map.of("s1", 90.0));

        GradebookCalculator.StudentSummary s1 = GradebookCalculator.compute(List.of("s1"), columns, grades, WEIGHTS_60_40).get("s1");

        assertEquals(20.0, s1.categoryAverages().get("QUIZ"));
        assertEquals(90.0, s1.finalGrade());
    }

    @Test
    void averageIgnoresNulls() {
        assertEquals(75.0, GradebookCalculator.average(Arrays.asList(70.0, null, 80.0)));
        assertNull(GradebookCalculator.average(Arrays.asList(null, null)));
    }
}
