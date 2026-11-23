package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.DiagnosisDTOs.MistakeItemDTO;
import org.example.learningprojectserver.entities.ReviewItemEntity;
import org.example.learningprojectserver.enums.MistakeType;
import org.springframework.stereotype.Component;

/** [s06] A notebook item with its mistake type, for the diagnosis screen. Items from before the diagnosis are UNCLASSIFIED. */
@Component
public class ReviewItemEntityToMistakeItemMapper implements Mapper<ReviewItemEntity, MistakeItemDTO> {
    @Override
    public MistakeItemDTO apply(ReviewItemEntity i) {
        MistakeType type = i.getErrorType() == null ? MistakeType.UNCLASSIFIED : i.getErrorType();
        return new MistakeItemDTO(i.getId(), i.getSubject(), i.getTopic(), i.getSubTopic(), i.getQuestionText(),
                i.getGivenAnswer(), type.name(), i.getErrorTypeSource(), i.getErrorNote(), i.getCreatedAt());
    }
}
