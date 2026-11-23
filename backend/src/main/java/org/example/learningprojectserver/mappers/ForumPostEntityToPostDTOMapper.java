package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.community.ClassForumDTOs.PostDTO;
import org.example.learningprojectserver.entities.ForumPostEntity;
import org.springframework.stereotype.Component;

@Component
public class ForumPostEntityToPostDTOMapper implements Mapper<ForumPostEntity, PostDTO> {
    @Override
    public PostDTO apply(ForumPostEntity entity) {
        PostDTO dto = new PostDTO();
        dto.setId(entity.getId());
        dto.setAuthorName(entity.getAuthorName());
        dto.setAuthorRole(entity.getAuthorRole());
        dto.setBody(entity.getBody());
        dto.setApproved(entity.isApproved());
        dto.setApprovedByName(entity.getApprovedByName());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
