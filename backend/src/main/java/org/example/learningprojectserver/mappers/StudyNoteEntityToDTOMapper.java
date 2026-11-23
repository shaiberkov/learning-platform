package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.StudyNoteDTOs.StudyNoteDTO;
import org.example.learningprojectserver.entities.StudyNoteEntity;
import org.springframework.stereotype.Component;

@Component
public class StudyNoteEntityToDTOMapper implements Mapper<StudyNoteEntity, StudyNoteDTO> {
    @Override
    public StudyNoteDTO apply(StudyNoteEntity n) {
        return new StudyNoteDTO(n.getId(), n.getKind(), n.getTargetType(), n.getTargetId(), n.getTargetTitle(),
                n.getSubject(), n.getTopic(), n.getSubTopic(), n.getQuote(), n.getBody(),
                n.getCreatedAt(), n.getUpdatedAt());
    }
}
