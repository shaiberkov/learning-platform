package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One AI call: who made it, for which school and feature, tokens and estimated cost. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ai_usage", indexes = {
        @Index(name = "idx_ai_usage_user_time", columnList = "userId,createdAt"),
        @Index(name = "idx_ai_usage_school_time", columnList = "schoolCode,createdAt")
})
public class AiUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    private String schoolCode;

    @Column(nullable = false, length = 40)
    private String feature;

    private String provider;
    private String model;
    private int promptTokens;
    private int completionTokens;
    private double costUsd;

    /** Served from the prompt cache: free, and not counted against the daily quota. */
    private boolean cached;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
