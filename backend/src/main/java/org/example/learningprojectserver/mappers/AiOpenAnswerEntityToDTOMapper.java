package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.ai.OpenAnswerDTO;
import org.example.learningprojectserver.entities.AiOpenAnswerEntity;
import org.springframework.stereotype.Component;

/** The teacher view of an answer. The student view is derived from it by {@code AiGradingService}. */
@Component
public class AiOpenAnswerEntityToDTOMapper implements Mapper<AiOpenAnswerEntity, OpenAnswerDTO> {
    @Override
    public OpenAnswerDTO apply(AiOpenAnswerEntity a) {
        return new OpenAnswerDTO(a.getId(), a.getRubric().getId(), a.getRubric().getTitle(), a.getRubric().getMaxScore(),
                a.getStudentUserId(), a.getStudentName(), a.getAnswerText(), a.getAiScore(), a.getAiReasoning(),
                a.getFinalScore(), a.getTeacherComment(), a.getStatus().name(), a.getSubmittedAt());
    }
}
