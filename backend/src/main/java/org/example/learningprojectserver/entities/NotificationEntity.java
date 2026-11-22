package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.learningprojectserver.notification.NotificationType;

import java.time.LocalDateTime;

/** One notification as a given user received it: the history behind the notification center. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "notification_entity", indexes = {
        @Index(name = "idx_notification_user_id", columnList = "userId,id"),
        @Index(name = "idx_notification_user_read", columnList = "userId,readAt")
})
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String body;

    private String link;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;
}
