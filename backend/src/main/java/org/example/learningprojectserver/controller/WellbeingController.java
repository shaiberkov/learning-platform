package org.example.learningprojectserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.WellbeingDTOs.*;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.CurrentUser;
import org.example.learningprojectserver.service.wellbeing.ExamCheckinService;
import org.example.learningprojectserver.service.wellbeing.ReflectionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.example.learningprojectserver.constants.ControllerConstants.Wellbeing.*;

/**
 * [s10] The student's reflection journal and pre-exam check-in. Every endpoint acts on the
 * authenticated student (CurrentUser), never on an id from the request.
 */
@RestController
@RequestMapping(WELLBEING_BASE_PATH)
@RequiredArgsConstructor
public class WellbeingController {

    private final ReflectionService reflectionService;
    private final ExamCheckinService examCheckinService;

    @GetMapping(WELLBEING_PROMPT)
    public BasicResponse<Prompt> prompt() {
        return examCheckinService.prompt(CurrentUser.userId());
    }

    @GetMapping(REFLECTION_CURRENT)
    public BasicResponse<ReflectionOverview> currentReflection() {
        return reflectionService.overview(CurrentUser.userId());
    }

    @GetMapping(REFLECTION_HISTORY)
    public BasicResponse<List<Reflection>> reflectionHistory() {
        return reflectionService.history(CurrentUser.userId());
    }

    @PostMapping(REFLECTION_SAVE)
    public BasicResponse<Reflection> saveReflection(@Valid @RequestBody ReflectionRequest request) {
        return reflectionService.save(CurrentUser.userId(), request);
    }

    @PostMapping(REFLECTION_MARK)
    public BasicResponse<Reflection> markAchieved(@RequestParam Long reflectionId, @RequestParam boolean achieved) {
        return reflectionService.markAchieved(CurrentUser.userId(), reflectionId, achieved);
    }

    @GetMapping(CHECKIN_STATUS)
    public BasicResponse<CheckinStatus> checkinStatus() {
        return examCheckinService.status(CurrentUser.userId());
    }

    @PostMapping(CHECKIN_SUBMIT)
    public BasicResponse<CheckinResult> checkIn(@Valid @RequestBody CheckinRequest request) {
        return examCheckinService.checkIn(CurrentUser.userId(), request);
    }

    @PostMapping(CHECKIN_CONSENT)
    public BasicResponse<Consent> setConsent(@RequestParam boolean consent) {
        return examCheckinService.setConsent(CurrentUser.userId(), consent);
    }
}
