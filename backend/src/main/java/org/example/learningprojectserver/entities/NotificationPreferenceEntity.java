package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Which channels a user wants notifications on. A user without a row gets the defaults:
 * in-app on, email off, Web Push off (push also needs a subscribed device).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "notification_preference_entity", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_pref_user", columnNames = "userId")
})
public class NotificationPreferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private boolean inApp = true;

    @Column(nullable = false)
    private boolean email = false;

    @Column(nullable = false)
    private boolean push = false;

    public NotificationPreferenceEntity(String userId) {
        this.userId = userId;
    }
}
