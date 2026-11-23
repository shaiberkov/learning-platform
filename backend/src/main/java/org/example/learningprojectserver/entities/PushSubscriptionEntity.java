package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A browser's Web Push subscription (one per device the user enabled push on). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "push_subscription_entity", indexes = {
        @Index(name = "idx_push_subscription_user", columnList = "userId"),
        @Index(name = "idx_push_subscription_endpoint", columnList = "endpoint")
})
public class PushSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 700)
    private String endpoint;

    @Column(nullable = false, length = 255)
    private String p256dh;

    @Column(nullable = false, length = 255)
    private String auth;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
