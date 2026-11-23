package org.example.learningprojectserver.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessageDTO {
    private Long id;
    private String userId;
    private Long questionId;
    private String role;
    private String content;
    private boolean flagged;
    private String flagReason;
    private LocalDateTime createdAt;
}
