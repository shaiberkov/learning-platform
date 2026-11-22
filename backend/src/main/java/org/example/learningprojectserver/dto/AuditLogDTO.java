package org.example.learningprojectserver.dto;

import java.time.LocalDateTime;

public record AuditLogDTO(
        Long id,
        LocalDateTime createdAt,
        String actorUserId,
        String actorUsername,
        String actorRole,
        String impersonatorId,
        String action,
        String actionLabel,
        String entityType,
        String entityId,
        String beforeValue,
        String afterValue,
        String ip,
        String schoolCode
) {}
