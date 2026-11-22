package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.ai.RubricDTO;
import org.example.learningprojectserver.entities.AiRubricEntity;
import org.springframework.stereotype.Component;

@Component
public class AiRubricEntityToDTOMapper implements Mapper<AiRubricEntity, RubricDTO> {
    @Override
    public RubricDTO apply(AiRubricEntity r) {
        return new RubricDTO(r.getId(), r.getTitle(), r.getQuestionText(), r.getRubric(), r.getMaxScore(), r.isPublished());
    }
}
