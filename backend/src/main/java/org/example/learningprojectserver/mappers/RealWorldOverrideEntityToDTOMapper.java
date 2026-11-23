package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.RealWorldDTOs.RealWorldTopicDTO;
import org.example.learningprojectserver.entities.RealWorldOverrideEntity;
import org.example.learningprojectserver.service.adaptive.SkillCatalog;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** A teacher-written card; the sub-topics come from the practice catalog. */
@Component
public class RealWorldOverrideEntityToDTOMapper implements Mapper<RealWorldOverrideEntity, RealWorldTopicDTO> {

    private static final DateTimeFormatter UPDATED_AT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public RealWorldTopicDTO apply(RealWorldOverrideEntity e) {
        return new RealWorldTopicDTO(e.getTopic(),
                SkillCatalog.mathTopics().getOrDefault(e.getTopic(), List.of()),
                e.getEverydayExample(), e.getCareerLink(), true,
                e.getUpdatedAt() == null ? null : e.getUpdatedAt().format(UPDATED_AT_FORMAT));
    }
}
