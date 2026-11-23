package org.example.learningprojectserver.dto.examsim;

import java.util.List;
import java.util.Map;

/** API shapes of the "what do I need?" calculator (מחשבון מה אני צריך). */
public final class GradeTargetDTOs {

    private GradeTargetDTOs() {}

    /** A gradebook column the student has no grade in yet, with the score it needs. */
    public record RemainingItemDTO(String title, String categoryLabel, double maxScore, String date,
                                   Double requiredScore) {}

    /**
     * One subject gradebook (teacher + class). status: POSSIBLE, ALREADY_SECURED, IMPOSSIBLE,
     * NO_REMAINING_MET, NO_REMAINING_MISSED or NO_DATA. requiredPercent is the average needed in each
     * remaining item (null unless POSSIBLE).
     */
    public record SubjectPlanDTO(String key, String title, String teacherName, Double currentGrade,
                                 Double minPossible, Double maxPossible, Double requiredPercent, String status,
                                 String message, int gradedCount, List<RemainingItemDTO> remaining,
                                 Map<String, Integer> weights, Map<String, String> categoryLabels) {}

    /** The certificate average: the mean of the subject grades, and the uniform score needed everywhere. */
    public record OverallPlanDTO(Double currentAverage, Double minPossible, Double maxPossible,
                                 Double requiredPercent, String status, String message) {}

    public record GradeTargetDTO(double target, List<SubjectPlanDTO> subjects, OverallPlanDTO overall) {}
}
