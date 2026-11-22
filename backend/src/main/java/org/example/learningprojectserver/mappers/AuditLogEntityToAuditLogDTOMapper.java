package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.AuditLogDTO;
import org.example.learningprojectserver.entities.AuditLogEntity;
import org.springframework.stereotype.Component;

@Component
public class AuditLogEntityToAuditLogDTOMapper implements Mapper<AuditLogEntity, AuditLogDTO> {
    @Override
    public AuditLogDTO apply(AuditLogEntity e) {
        return new AuditLogDTO(
                e.getId(),
                e.getCreatedAt(),
                e.getActorUserId(),
                e.getActorUsername(),
                e.getActorRole(),
                e.getImpersonatorId(),
                e.getAction().name(),
                e.getAction().getLabel(),
                e.getEntityType(),
                e.getEntityId(),
                e.getBeforeValue(),
                e.getAfterValue(),
                e.getIp(),
                e.getSchoolCode()
        );
    }
}
