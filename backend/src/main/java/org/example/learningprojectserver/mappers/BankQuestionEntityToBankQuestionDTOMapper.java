package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.BankQuestionDTO;
import org.example.learningprojectserver.entities.BankQuestionEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class BankQuestionEntityToBankQuestionDTOMapper implements Mapper<BankQuestionEntity, BankQuestionDTO> {
    @Override
    public BankQuestionDTO apply(BankQuestionEntity entity) {
        BankQuestionDTO dto = new BankQuestionDTO();
        dto.setId(entity.getId());
        dto.setQuestionText(entity.getQuestionText());
        dto.setAnswer(entity.getAnswer());
        if (entity.getOptions() != null && !entity.getOptions().isBlank()) {
            dto.setOptions(Arrays.stream(entity.getOptions().split("\n"))
                    .map(String::trim)
                    .filter(o -> !o.isEmpty())
                    .toList());
        }
        dto.setSubject(entity.getSubject());
        dto.setTopic(entity.getTopic());
        dto.setLevel(entity.getLevel());
        dto.setGrade(entity.getGrade());
        dto.setOwnerName(entity.getOwnerName());
        dto.setShared(entity.isShared());
        dto.setTimesAsked(entity.getTimesAsked());
        dto.setAttempts(entity.getAttempts());
        dto.setCorrectAttempts(entity.getCorrectAttempts());
        if (entity.getAttempts() > 0) {
            dto.setSuccessRate(Math.round(1000.0 * entity.getCorrectAttempts() / entity.getAttempts()) / 10.0);
        }
        return dto;
    }
}
