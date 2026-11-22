package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.ai.AiQuestionDraftDTO;
import org.example.learningprojectserver.entities.AiQuestionDraftEntity;
import org.springframework.stereotype.Component;

@Component
public class AiQuestionDraftEntityToDTOMapper implements Mapper<AiQuestionDraftEntity, AiQuestionDraftDTO> {
    @Override
    public AiQuestionDraftDTO apply(AiQuestionDraftEntity d) {
        return new AiQuestionDraftDTO(d.getId(), d.getSubject(), d.getTopic(), d.getSubTopic(), d.getDifficulty(),
                d.getGradeLevel(), d.getQuestionText(), d.getAnswer(), d.getExplanation(), d.getStatus().name(),
                d.getBankQuestionId());
    }
}
