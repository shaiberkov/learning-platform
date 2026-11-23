package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.FlashcardDTOs.FlashcardDTO;
import org.example.learningprojectserver.entities.FlashcardEntity;
import org.example.learningprojectserver.entities.FlashcardReviewEntity;
import org.springframework.stereotype.Component;

/** A card as seen by {@code viewerId}, with that viewer's schedule ({@code review}, null when not in their deck). */
@Component
public class FlashcardToDTOMapper implements Mapper<FlashcardToDTOMapper.View, FlashcardDTO> {

    public record View(FlashcardEntity card, FlashcardReviewEntity review, String viewerId) {}

    @Override
    public FlashcardDTO apply(View v) {
        FlashcardEntity c = v.card();
        FlashcardReviewEntity r = v.review();
        return new FlashcardDTO(c.getId(), c.getFront(), c.getBack(), c.getSubject(), c.getTopic(), c.getSource(),
                c.getClassRoomId() != null, c.getOwnerUserId().equals(v.viewerId()), r != null,
                r == null ? null : r.getDueDate(), r == null ? 0 : r.getReviewCount(), r == null ? 0 : r.getLapses());
    }
}
