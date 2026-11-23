package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.DailyChallengeDTOs.DailyChallengeQuestionDTO;
import org.example.learningprojectserver.entities.DailyChallengeEntity;
import org.springframework.stereotype.Component;

/** Includes the answer: use it only for a challenge whose day is over. */
@Component
public class DailyChallengeEntityToQuestionDTOMapper implements Mapper<DailyChallengeEntity, DailyChallengeQuestionDTO> {

    @Override
    public DailyChallengeQuestionDTO apply(DailyChallengeEntity c) {
        return new DailyChallengeQuestionDTO(c.getChallengeDate(), c.getGradeName(), c.getTopic(), c.getSubTopic(),
                c.getQuestionText(), c.getAnswer());
    }
}
