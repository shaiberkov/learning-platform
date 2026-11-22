package org.example.learningprojectserver.service.ai;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.ai.AiChatMessageDTO;
import org.example.learningprojectserver.dto.ai.AiUsageByFeatureDTO;
import org.example.learningprojectserver.mappers.AiConversationMessageEntityToDTOMapper;
import org.example.learningprojectserver.repository.AiConversationMessageRepository;
import org.example.learningprojectserver.repository.AiUsageRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/** What school staff see about AI use in their school: cost per feature and safety-flagged messages. */
@Service
@RequiredArgsConstructor
public class AiOversightService {

    private final AiUsageRepository usageRepository;
    private final AiConversationMessageRepository conversationRepository;
    private final AiConversationMessageEntityToDTOMapper messageMapper;

    public BasicResponse<List<AiUsageByFeatureDTO>> schoolUsage(AiUser staff, int days) {
        if (staff.schoolCode() == null) {
            return new BasicResponse<>(false, "המשתמש אינו משויך לבית ספר");
        }
        int window = Math.max(1, Math.min(days, 365));
        BasicResponse<List<AiUsageByFeatureDTO>> response = new BasicResponse<>(true, null);
        response.setData(usageRepository.summarizeBySchool(staff.schoolCode(), AiGateway.now().minusDays(window)));
        return response;
    }

    public BasicResponse<List<AiChatMessageDTO>> flaggedMessages(AiUser staff) {
        if (staff.schoolCode() == null) {
            return new BasicResponse<>(false, "המשתמש אינו משויך לבית ספר");
        }
        BasicResponse<List<AiChatMessageDTO>> response = new BasicResponse<>(true, null);
        response.setData(conversationRepository.findFlaggedBySchool(staff.schoolCode(), PageRequest.of(0, 100))
                .stream().map(messageMapper).toList());
        return response;
    }
}
