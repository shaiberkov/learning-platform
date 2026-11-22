package org.example.learningprojectserver.dto;

import java.util.List;
import java.util.Map;

/**
 * One class gradebook for one teacher: students (rows), columns (the teacher's own assignments plus the
 * system tests the teacher gave the class), scores, category weights and the computed grades.
 */
public record GradebookDTO(
        Long classRoomId,
        String className,
        List<Student> students,
        List<Column> columns,
        /* column key -> student userId -> score */
        Map<String, Map<String, Double>> grades,
        /* category -> weight percent */
        Map<String, Integer> weights,
        /* category -> Hebrew label */
        Map<String, String> categoryLabels,
        /* student userId -> category -> average percent */
        Map<String, Map<String, Double>> categoryAverages,
        /* student userId -> weighted final grade (null when nothing is graded) */
        Map<String, Double> finalGrades,
        Double classAverage
) {
    public record Student(String userId, String name) {}

    /** source is MANUAL (editable, assignmentId set) or TEACHER_TEST (scores come from the test results). */
    public record Column(String key, Long assignmentId, Long testId, String title, String category,
                         String categoryLabel, double maxScore, String date, String source, boolean editable) {}
}
