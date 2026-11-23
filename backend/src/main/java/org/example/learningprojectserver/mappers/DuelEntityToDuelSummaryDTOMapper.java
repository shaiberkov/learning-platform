package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.DuelDTOs.DuelSummaryDTO;
import org.example.learningprojectserver.entities.DuelEntity;
import org.example.learningprojectserver.service.competition.CompetitionCatalog;
import org.springframework.stereotype.Component;

@Component
public class DuelEntityToDuelSummaryDTOMapper implements Mapper<DuelEntity, DuelSummaryDTO> {

    @Override
    public DuelSummaryDTO apply(DuelEntity d) {
        return new DuelSummaryDTO(d.getId(), d.getMode(), d.getStatus(), d.getTopic(), d.getSubTopic(), d.getLevel(),
                d.getChallengerId(), d.getChallengerName(), d.getOpponentId(), d.getOpponentName(),
                CompetitionCatalog.DUEL_QUESTION_COUNT, d.getChallengerAnswered(), d.getChallengerCorrect(),
                d.getOpponentAnswered(), d.getOpponentCorrect(), d.getSystemScore(), d.getOutcome(),
                d.getCreatedAt());
    }
}
