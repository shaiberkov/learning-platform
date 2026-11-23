package org.example.learningprojectserver.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** API shapes of the class forum (ClassForumService). */
public final class ClassForumDTOs {

    private ClassForumDTOs() {}

    public static final int MAX_TITLE = 200;
    public static final int MAX_BODY = 4000;
    public static final int MAX_TOPIC = 100;
    public static final int MAX_EXERCISE = 1000;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ThreadRequest {
        @NotBlank(message = "יש לכתוב כותרת לשאלה")
        @Size(max = MAX_TITLE, message = "הכותרת ארוכה מדי")
        private String title;
        @Size(max = MAX_BODY, message = "תיאור השאלה ארוך מדי")
        private String body;
        @Size(max = MAX_TOPIC, message = "שם הנושא ארוך מדי")
        private String topic;
        @Size(max = MAX_EXERCISE, message = "נוסח התרגיל ארוך מדי")
        private String exerciseText;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ReplyRequest {
        @NotBlank(message = "יש לכתוב תשובה")
        @Size(max = MAX_BODY, message = "התשובה ארוכה מדי")
        private String body;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ThreadDTO {
        private Long id;
        private Long classRoomId;
        private String title;
        private String body;
        private String topic;
        private String exerciseText;
        private String authorName;
        /** The caller asked it. */
        private boolean mine;
        private int answerCount;
        private boolean resolved;
        private LocalDateTime createdAt;
        private LocalDateTime lastActivityAt;
        /** Only in the similar-threads list: how close it is to the text typed (0-100). */
        private Integer matchScore;
        /** Only in the thread view. */
        private List<PostDTO> posts = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PostDTO {
        private Long id;
        private String authorName;
        private String authorRole;
        private String body;
        private boolean approved;
        private String approvedByName;
        private LocalDateTime createdAt;
        private boolean mine;
    }
}
