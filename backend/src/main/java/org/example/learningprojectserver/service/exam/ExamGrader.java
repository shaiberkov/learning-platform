package org.example.learningprojectserver.service.exam;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.learningprojectserver.entities.TestQuestionEntity;
import org.example.learningprojectserver.enums.QuestionKind;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/**
 * Server-side grading, one rule per question kind. Answers arrive as strings:
 * JSON arrays of original option ids for MULTI_SELECT / MATCHING / ORDERING,
 * plain text or LaTeX for NUMERIC / SHORT_OPEN / TEXT.
 * Every question is all-or-nothing, which keeps item analysis dichotomous.
 */
@Component
public class ExamGrader {

    private static final double DEFAULT_EPSILON = 1e-9;

    public boolean isCorrect(TestQuestionEntity q, String answer) {
        if (answer == null || answer.isBlank()) {
            return false;
        }
        QuestionKind kind = QuestionKind.of(q.getKind());
        if (kind == null) {
            return false;
        }
        return switch (kind) {
            case TEXT -> gradeText(q, answer);
            case MULTI_SELECT -> gradeMultiSelect(q, answer);
            case NUMERIC -> gradeNumeric(q, answer);
            case MATCHING -> gradeMatching(q, answer);
            case ORDERING -> gradeOrdering(q, answer);
            case SHORT_OPEN -> gradeShortOpen(q, answer);
        };
    }

    private boolean gradeText(TestQuestionEntity q, String answer) {
        if (q.getAnswer() == null) {
            return false;
        }
        String expected = q.getAnswer().replaceAll("[‎‏]", "").trim();
        return answer.replaceAll("[‎‏]", "").trim().equalsIgnoreCase(expected);
    }

    private boolean gradeMultiSelect(TestQuestionEntity q, String answer) {
        List<Integer> expected = ExamQuestionCodec.readInts(q.getAnswerKeyJson());
        List<Integer> given = ExamQuestionCodec.readInts(answer);
        if (expected == null || given == null || given.contains(null)) {
            return false;
        }
        return new HashSet<>(expected).equals(new HashSet<>(given)) && given.size() == new HashSet<>(given).size();
    }

    private boolean gradeNumeric(TestQuestionEntity q, String answer) {
        JsonNode key = ExamQuestionCodec.readTree(q.getAnswerKeyJson());
        if (!key.isNumber()) {
            return false;
        }
        Double given = LatexAnswerParser.parseNumber(answer);
        if (given == null) {
            return false;
        }
        double tolerance = q.getTolerance() == null ? 0 : q.getTolerance();
        return Math.abs(given - key.asDouble()) <= tolerance + DEFAULT_EPSILON;
    }

    private boolean gradeMatching(TestQuestionEntity q, String answer) {
        List<Integer> expected = ExamQuestionCodec.readInts(q.getAnswerKeyJson());
        List<Integer> given = ExamQuestionCodec.readInts(answer);
        return expected != null && expected.equals(given);
    }

    private boolean gradeOrdering(TestQuestionEntity q, String answer) {
        int size = ExamQuestionCodec.readStrings(q.getOptionsJson()).size();
        List<Integer> given = ExamQuestionCodec.readInts(answer);
        if (given == null || given.size() != size) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (given.get(i) == null || given.get(i) != i) {
                return false;
            }
        }
        return true;
    }

    private boolean gradeShortOpen(TestQuestionEntity q, String answer) {
        String given = LatexAnswerParser.normalize(answer);
        if (given.isEmpty()) {
            return false;
        }
        for (String accepted : ExamQuestionCodec.readStrings(q.getAnswerKeyJson())) {
            if (LatexAnswerParser.normalize(accepted).equals(given)) {
                return true;
            }
        }
        return false;
    }
}
