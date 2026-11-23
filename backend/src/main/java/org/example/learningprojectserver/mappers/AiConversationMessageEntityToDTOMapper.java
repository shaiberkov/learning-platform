package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.ai.AiChatMessageDTO;
import org.example.learningprojectserver.entities.AiConversationMessageEntity;
import org.springframework.stereotype.Component;

@Component
public class AiConversationMessageEntityToDTOMapper implements Mapper<AiConversationMessageEntity, AiChatMessageDTO> {
    @Override
    public AiChatMessageDTO apply(AiConversationMessageEntity m) {
        return new AiChatMessageDTO(m.getId(), m.getUserId(), m.getQuestionId(), m.getRole(), m.getContent(),
                m.isFlagged(), m.getFlagReason(), m.getCreatedAt());
    }
}
