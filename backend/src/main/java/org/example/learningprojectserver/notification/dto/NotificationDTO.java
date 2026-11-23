package org.example.learningprojectserver.notification.dto;

import org.example.learningprojectserver.notification.NotificationType;

import java.time.LocalDateTime;

public class NotificationDTO<T> {
    private NotificationType type;
    private T payload;
    // Filled per recipient once the notification is stored in the notification center.
    private Long id;
    private String title;
    private String body;
    private String link;
    private LocalDateTime createdAt;

    public NotificationDTO() {}

    public NotificationDTO(NotificationType type, T payload) {
        this.type = type;
        this.payload = payload;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
