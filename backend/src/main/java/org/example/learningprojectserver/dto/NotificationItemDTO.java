package org.example.learningprojectserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.learningprojectserver.notification.NotificationType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItemDTO {
    private Long id;
    private NotificationType type;
    private String title;
    private String body;
    private String link;
    private LocalDateTime createdAt;
    private boolean read;
}
