package org.example.learningprojectserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
public class MessageDTO {

    private Long id;
    private String title;
    private String content;
     private LocalDateTime sentAt;
    private String senderName;
    private LocalDateTime readAt;
    private String batchId;
    private String threadId;
    private Long parentMessageId;
    private Long attachmentId;
    private String attachmentName;
    // Set only in thread views: true when the viewer wrote this message.
    private Boolean mine;

    public MessageDTO(String title, String content, LocalDateTime sentAt, String senderName) {
        this.title = title;
        this.content = content;
        this.sentAt = sentAt;
        this.senderName = senderName;
    }

    public MessageDTO(Long id, String title, String content, LocalDateTime sentAt, String senderName,
                      LocalDateTime readAt, String batchId, String threadId, Long parentMessageId,
                      Long attachmentId, String attachmentName) {
        this(title, content, sentAt, senderName);
        this.id = id;
        this.readAt = readAt;
        this.batchId = batchId;
        this.threadId = threadId;
        this.parentMessageId = parentMessageId;
        this.attachmentId = attachmentId;
        this.attachmentName = attachmentName;
    }
}
