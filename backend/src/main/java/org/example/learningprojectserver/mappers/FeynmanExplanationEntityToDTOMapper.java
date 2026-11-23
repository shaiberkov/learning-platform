package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.DiagnosisDTOs.FeynmanExplanationDTO;
import org.example.learningprojectserver.entities.FeynmanExplanationEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/** [s06] A Feynman attempt; gaps are stored one per line. The student name is filled in by the teacher view. */
@Component
public class FeynmanExplanationEntityToDTOMapper implements Mapper<FeynmanExplanationEntity, FeynmanExplanationDTO> {
    @Override
    public FeynmanExplanationDTO apply(FeynmanExplanationEntity e) {
        List<String> gaps = e.getGaps() == null || e.getGaps().isBlank() ? List.of()
                : Arrays.stream(e.getGaps().split("\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return new FeynmanExplanationDTO(e.getId(), e.getStudentUserId(), null, e.getPromptKey(), e.getTopic(),
                e.getPromptText(), e.getExplanationText(), e.getInputMode(), e.getScore(), gaps, e.getFeedback(),
                e.getEvaluatedBy(), e.getCreatedAt());
    }
}
