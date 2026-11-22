package org.example.learningprojectserver.dto;

import java.util.List;
import java.util.Map;

/** The teacher's live view of the current question and the running scoreboard. */
public record LiveQuizResultsDTO(
        boolean active,
        String code,
        Long classRoomId,
        String className,
        int questionNumber,
        String questionText,
        List<String> options,
        String correctAnswer,
        boolean questionOpen,
        int totalStudents,
        int answeredCount,
        int correctCount,
        /* answer -> how many students gave it (the options in order, then other answers) */
        Map<String, Integer> answerCounts,
        List<StudentAnswer> answers,
        List<ScoreRow> scoreboard
) {
    public record StudentAnswer(String studentName, String answer, boolean correct) {}

    public record ScoreRow(String studentName, int answered, int correct) {}
}
