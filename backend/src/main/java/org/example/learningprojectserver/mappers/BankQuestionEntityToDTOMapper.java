package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.ai.BankQuestionDTO;
import org.example.learningprojectserver.entities.BankQuestionEntity;
import org.springframework.stereotype.Component;

@Component
public class BankQuestionEntityToDTOMapper implements Mapper<BankQuestionEntity, BankQuestionDTO> {
    @Override
    public BankQuestionDTO apply(BankQuestionEntity q) {
        return new BankQuestionDTO(q.getId(), q.getSubject(), q.getTopic(), q.getSubTopic(), q.getLevel(),
                q.getGrade(), q.getQuestionText(), q.getAnswer(), q.getExplanation(), q.getSource(), q.getCreatedAt());
    }
}
