package org.example.learningprojectserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.DailyChallengeDTOs.DailyChallengeAnswerRequest;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.CurrentUser;
import org.example.learningprojectserver.service.competition.DailyChallengeService;
import org.springframework.web.bind.annotation.*;

import static org.example.learningprojectserver.constants.ControllerConstants.DailyChallenge.*;

/** The daily challenge of the student's grade (STUDENT route group); the student is the authenticated user. */
@RestController
@RequestMapping(DAILY_CHALLENGE_BASE_PATH)
@RequiredArgsConstructor
public class DailyChallengeController {

    private final DailyChallengeService dailyChallengeService;

    @GetMapping(DAILY_CHALLENGE_TODAY)
    public BasicResponse getToday() {
        return dailyChallengeService.getToday(CurrentUser.userId());
    }

    @PostMapping(DAILY_CHALLENGE_START)
    public BasicResponse start() {
        return dailyChallengeService.start(CurrentUser.userId());
    }

    @PostMapping(DAILY_CHALLENGE_ANSWER)
    public BasicResponse answer(@Valid @RequestBody DailyChallengeAnswerRequest request) {
        return dailyChallengeService.answer(CurrentUser.userId(), request.answer());
    }
}
