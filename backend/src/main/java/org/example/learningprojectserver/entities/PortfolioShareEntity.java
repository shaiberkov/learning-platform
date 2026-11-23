package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * The secret in a student's public portfolio link. One per student: sharing again replaces the token
 * (the old link stops working) and revoking deletes the row.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "portfolio_share", uniqueConstraints = {
        @UniqueConstraint(name = "uk_portfolio_share_student", columnNames = "studentUserId"),
        @UniqueConstraint(name = "uk_portfolio_share_token", columnNames = "token")
})
public class PortfolioShareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    @Column(nullable = false, length = 64)
    private String token;

    /** The name shown on the public page, chosen by the student (never the username or id). */
    @Column(length = 60)
    private String displayName;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
