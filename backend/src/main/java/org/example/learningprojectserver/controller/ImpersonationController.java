package org.example.learningprojectserver.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.ImpersonationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.example.learningprojectserver.constants.ControllerConstants.Impersonation.*;

/** Start: system admins only (SecurityConfig). Stop: any caller holding an impersonation token. */
@RestController
@RequestMapping(IMPERSONATION_BASE_PATH)
@RequiredArgsConstructor
public class ImpersonationController {

    private final ImpersonationService impersonationService;

    @PostMapping(START_IMPERSONATION)
    public BasicResponse<Map<String, String>> start(@RequestParam String targetUserId, HttpServletResponse response) {
        return impersonationService.start(targetUserId, response);
    }

    @PostMapping(STOP_IMPERSONATION)
    public BasicResponse<Map<String, String>> stop(HttpServletResponse response) {
        return impersonationService.stop(response);
    }
}
