package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.QuestDTOs;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.CurrentUser;
import org.example.learningprojectserver.service.quest.QuestService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.example.learningprojectserver.constants.ControllerConstants.Quest.*;

/** Story quests of the signed-in student (a system admin reaches them only while impersonating a student). */
@RestController
@RequestMapping(QUEST_BASE_PATH)
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class QuestController {

    private final QuestService questService;

    @GetMapping(LIST_QUESTS)
    public BasicResponse<List<QuestDTOs.QuestSummary>> list() {
        return questService.listQuests(CurrentUser.userId());
    }

    @GetMapping(QUEST_STATE)
    public BasicResponse<QuestDTOs.QuestState> state(@RequestParam String questCode) {
        return questService.getState(CurrentUser.userId(), questCode);
    }

    @PostMapping(START_QUEST)
    public BasicResponse<QuestDTOs.QuestState> start(@RequestParam String questCode) {
        return questService.start(CurrentUser.userId(), questCode);
    }

    @PostMapping(ANSWER_QUEST)
    public BasicResponse<QuestDTOs.AnswerResult> answer(@RequestParam String questCode, @RequestParam String answer) {
        return questService.answer(CurrentUser.userId(), questCode, answer);
    }

    @GetMapping(COSMETICS)
    public BasicResponse<QuestDTOs.Cosmetics> cosmetics() {
        return questService.getCosmetics(CurrentUser.userId());
    }

    @PostMapping(EQUIP_COSMETIC)
    public BasicResponse<QuestDTOs.Cosmetics> equip(@RequestParam String itemCode) {
        return questService.equip(CurrentUser.userId(), itemCode);
    }
}
