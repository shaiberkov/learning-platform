package org.example.learningprojectserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One send action as its sender sees it, with read-receipt counts. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SentMessageSummaryDTO {
    private String batchId;
    private String threadId;
    private String title;
    private String content;
    private LocalDateTime sentAt;
    private LocalDateTime scheduledFor;
    private boolean pending;
    private long recipientsCount;
    private long readCount;
    private Long attachmentId;
}
