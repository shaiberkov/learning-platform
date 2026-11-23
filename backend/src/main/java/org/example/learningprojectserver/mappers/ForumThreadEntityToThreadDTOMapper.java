package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.community.ClassForumDTOs.ThreadDTO;
import org.example.learningprojectserver.entities.ForumThreadEntity;
import org.springframework.stereotype.Component;

@Component
public class ForumThreadEntityToThreadDTOMapper implements Mapper<ForumThreadEntity, ThreadDTO> {
    @Override
    public ThreadDTO apply(ForumThreadEntity entity) {
        ThreadDTO dto = new ThreadDTO();
        dto.setId(entity.getId());
        dto.setClassRoomId(entity.getClassRoomId());
        dto.setTitle(entity.getTitle());
        dto.setBody(entity.getBody());
        dto.setTopic(entity.getTopic());
        dto.setExerciseText(entity.getExerciseText());
        dto.setAuthorName(entity.getAuthorName());
        dto.setAnswerCount(entity.getAnswerCount());
        dto.setResolved(entity.isResolved());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setLastActivityAt(entity.getLastActivityAt());
        return dto;
    }
}
