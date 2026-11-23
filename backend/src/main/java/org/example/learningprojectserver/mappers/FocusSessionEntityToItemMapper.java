package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.FocusDTOs;
import org.example.learningprojectserver.entities.FocusSessionEntity;
import org.springframework.stereotype.Component;

@Component
public class FocusSessionEntityToItemMapper implements Mapper<FocusSessionEntity, FocusDTOs.SessionItem> {
    @Override
    public FocusDTOs.SessionItem apply(FocusSessionEntity s) {
        return new FocusDTOs.SessionItem(s.getId(), s.getTask(), s.getPlannedMinutes(), s.getActualMinutes(),
                s.isCompleted(), s.getStartedAt().toString());
    }
}
