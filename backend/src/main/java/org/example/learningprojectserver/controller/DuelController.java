package org.example.learningprojectserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.DuelDTOs.DuelAnswerRequest;
import org.example.learningprojectserver.dto.DuelDTOs.DuelInviteRequest;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.CurrentUser;
import org.example.learningprojectserver.service.competition.DuelService;
import org.springframework.web.bind.annotation.*;

import static org.example.learningprojectserver.constants.ControllerConstants.Duel.*;

/**
 * Student duels (STUDENT route group). The acting student is always the authenticated user; a duel id is
 * checked in DuelService against its two participants.
 */
@RestController
@RequestMapping(DUEL_BASE_PATH)
@RequiredArgsConstructor
public class DuelController {

    private final DuelService duelService;

    @GetMapping(DUEL_TOPICS)
    public BasicResponse getTopics() {
        return duelService.getTopics();
    }

    @GetMapping(DUEL_OPPONENTS)
    public BasicResponse getOpponents(@RequestParam String subTopic) {
        return duelService.getOpponents(CurrentUser.userId(), subTopic);
    }

    @GetMapping(DUEL_LIST)
    public BasicResponse getMyDuels() {
        return duelService.getMyDuels(CurrentUser.userId());
    }

    @PostMapping(DUEL_INVITE)
    public BasicResponse invite(@Valid @RequestBody DuelInviteRequest request) {
        return duelService.invite(CurrentUser.userId(), request);
    }

    @GetMapping(DUEL_STATE)
    public BasicResponse getState(@PathVariable Long duelId) {
        return duelService.getState(CurrentUser.userId(), duelId);
    }

    @PostMapping(DUEL_ACCEPT)
    public BasicResponse accept(@PathVariable Long duelId) {
        return duelService.accept(CurrentUser.userId(), duelId);
    }

    @PostMapping(DUEL_DECLINE)
    public BasicResponse decline(@PathVariable Long duelId) {
        return duelService.decline(CurrentUser.userId(), duelId);
    }

    @PostMapping(DUEL_FORFEIT)
    public BasicResponse forfeit(@PathVariable Long duelId) {
        return duelService.forfeit(CurrentUser.userId(), duelId);
    }

    @PostMapping(DUEL_ANSWER)
    public BasicResponse answer(@PathVariable Long duelId, @Valid @RequestBody DuelAnswerRequest request) {
        return duelService.answer(CurrentUser.userId(), duelId, request);
    }
}
