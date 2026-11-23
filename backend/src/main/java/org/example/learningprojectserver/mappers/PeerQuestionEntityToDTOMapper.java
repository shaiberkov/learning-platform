package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.community.PeerQuestionDTOs.PeerQuestionDTO;
import org.example.learningprojectserver.entities.PeerQuestionEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class PeerQuestionEntityToDTOMapper implements Mapper<PeerQuestionEntity, PeerQuestionDTO> {
    @Override
    public PeerQuestionDTO apply(PeerQuestionEntity entity) {
        PeerQuestionDTO dto = new PeerQuestionDTO();
        dto.setId(entity.getId());
        dto.setClassRoomId(entity.getClassRoomId());
        dto.setQuestionText(entity.getQuestionText());
        dto.setCorrectAnswer(entity.getCorrectAnswer());
        dto.setDistractors(splitLines(entity.getDistractors()));
        dto.setTopic(entity.getTopic());
        dto.setExplanation(entity.getExplanation());
        dto.setStatus(entity.getStatus() == null ? null : entity.getStatus().name());
        dto.setReviewNote(entity.getReviewNote());
        dto.setAuthorName(entity.getAuthorName());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setReviewedAt(entity.getReviewedAt());
        return dto;
    }

    public static List<String> splitLines(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
