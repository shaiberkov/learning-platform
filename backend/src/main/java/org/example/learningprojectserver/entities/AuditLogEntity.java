package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.learningprojectserver.enums.AuditAction;

import java.time.LocalDateTime;

/** One sensitive action: who did what to which record, the state before and after, when and from where. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_created_at", columnList = "createdAt"),
        @Index(name = "idx_audit_school_code", columnList = "schoolCode"),
        @Index(name = "idx_audit_actor", columnList = "actorUserId")
})
public class AuditLogEntity {

    public static final int VALUE_MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** The effective user (the impersonated user while impersonating). */
    private String actorUserId;
    private String actorUsername;
    private String actorRole;

    /** The system admin behind an impersonated session, otherwise null. */
    private String impersonatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AuditAction action;

    @Column(length = 64)
    private String entityType;
    private String entityId;

    @Column(length = VALUE_MAX_LENGTH)
    private String beforeValue;
    @Column(length = VALUE_MAX_LENGTH)
    private String afterValue;

    @Column(length = 64)
    private String ip;

    /** The school the action belongs to, so a school manager sees only their own school. */
    private String schoolCode;
}
