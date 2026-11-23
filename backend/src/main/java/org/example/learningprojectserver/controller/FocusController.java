package org.example.learningprojectserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.FocusDTOs;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.CurrentUser;
import org.example.learningprojectserver.service.FocusService;
import org.springframework.web.bind.annotation.*;

import static org.example.learningprojectserver.constants.ControllerConstants.Focus.*;

/** Focus mode and study-habit insights of the calling student (under /student/**). */
@RestController
@RequestMapping(FOCUS_BASE_PATH)
@RequiredArgsConstructor
public class FocusController {

    private final FocusService focusService;

    @PostMapping(RECORD_SESSION)
    public BasicResponse recordSession(@Valid @RequestBody FocusDTOs.SessionRequest request) {
        return focusService.recordSession(CurrentUser.userId(), request);
    }

    @GetMapping(INSIGHTS)
    public BasicResponse insights() {
        return focusService.getInsights(CurrentUser.userId());
    }
}
