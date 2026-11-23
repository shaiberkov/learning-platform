package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.EngagementSummaryDTO;
import org.example.learningprojectserver.entities.StudentBadgeEntity;
import org.springframework.stereotype.Component;

@Component
public class StudentBadgeEntityToBadgeMapper implements Mapper<StudentBadgeEntity, EngagementSummaryDTO.Badge> {
    @Override
    public EngagementSummaryDTO.Badge apply(StudentBadgeEntity b) {
        return new EngagementSummaryDTO.Badge(b.getCode(), b.getTitle(), b.getDescription(), b.getEarnedAt());
    }
}
