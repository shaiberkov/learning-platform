package org.example.learningprojectserver.dto.ai;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A message to the tutor. {@code questionId} is the practice question on screen (null for a free
 * chat), {@code studentAnswer} what the student has typed so far, {@code imageDataUrl} an optional
 * photo of an exercise as a {@code data:image/...;base64,...} URL.
 */
@Getter
@Setter
@NoArgsConstructor
public class TutorRequestDTO {
    private Long questionId;
    private String studentAnswer;
    private String message;
    private String imageDataUrl;
}
