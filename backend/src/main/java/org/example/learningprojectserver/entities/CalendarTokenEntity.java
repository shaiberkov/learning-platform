package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** The secret in a user's personal .ics feed URL. Regenerating it revokes the old link. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "calendar_token_entity", uniqueConstraints = {
        @UniqueConstraint(name = "uk_calendar_token_user", columnNames = "userId"),
        @UniqueConstraint(name = "uk_calendar_token_token", columnNames = "token")
})
public class CalendarTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 64)
    private String token;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
