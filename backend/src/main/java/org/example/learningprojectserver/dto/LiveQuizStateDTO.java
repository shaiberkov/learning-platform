package org.example.learningprojectserver.dto;

import java.util.List;

/**
 * What a student sees of a live quiz. correctAnswer is filled only after the teacher closes the question;
 * myAnswer / myAnswerCorrect only in the student's own view (not in the broadcast).
 */
public record LiveQuizStateDTO(
        boolean active,
        String code,
        String teacherName,
        String className,
        int questionNumber,
        String questionText,
        List<String> options,
        boolean questionOpen,
        String correctAnswer,
        String myAnswer,
        Boolean myAnswerCorrect
) {
    public static LiveQuizStateDTO ended(String code) {
        return new LiveQuizStateDTO(false, code, null, null, 0, null, List.of(), false, null, null, null);
    }
}
