package org.example.learningprojectserver.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** API shapes of the student-authored questions (PeerQuestionService). */
public final class PeerQuestionDTOs {

    private PeerQuestionDTOs() {}

    public static final int MIN_DISTRACTORS = 2;
    public static final int MAX_DISTRACTORS = 5;
    public static final int MAX_TEXT = 2000;
    public static final int MAX_OPTION = 500;
    public static final int MAX_TOPIC = 100;
    public static final int MAX_EXPLANATION = 2000;
    public static final int MAX_NOTE = 500;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SubmitRequest {
        @NotBlank(message = "יש לכתוב את נוסח השאלה")
        @Size(max = MAX_TEXT, message = "נוסח השאלה ארוך מדי")
        private String questionText;
        @NotBlank(message = "יש לכתוב את התשובה הנכונה")
        @Size(max = MAX_OPTION, message = "התשובה ארוכה מדי")
        private String correctAnswer;
        /** The wrong options. */
        private List<String> distractors = new ArrayList<>();
        @Size(max = MAX_TOPIC, message = "שם הנושא ארוך מדי")
        private String topic;
        @Size(max = MAX_EXPLANATION, message = "ההסבר ארוך מדי")
        private String explanation;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ReviewRequest {
        @Size(max = MAX_NOTE, message = "ההערה ארוכה מדי")
        private String note;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AnswerRequest {
        @NotBlank(message = "יש לבחור תשובה")
        private String answer;
    }

    /** A question as its author and the reviewing teacher see it (with the answer). */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class PeerQuestionDTO {
        private Long id;
        private Long classRoomId;
        private String questionText;
        private String correctAnswer;
        private List<String> distractors = new ArrayList<>();
        private String topic;
        private String explanation;
        private String status;
        private String reviewNote;
        private String authorName;
        private LocalDateTime createdAt;
        private LocalDateTime reviewedAt;
        /** Classmates' answers in the class practice (approved questions only). */
        private int attempts;
        private int correctAttempts;
    }

    /** A question in the class practice: shuffled options, no answer. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class PracticeQuestionDTO {
        private Long id;
        private String questionText;
        private List<String> options = new ArrayList<>();
        private String topic;
        /** Credit: the student who wrote it. */
        private String authorName;
        private boolean mine;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AnswerResultDTO {
        private boolean correct;
        private String correctAnswer;
        private String explanation;
        private String authorName;
    }
}
