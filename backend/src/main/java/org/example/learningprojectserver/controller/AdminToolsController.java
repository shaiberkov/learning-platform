package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.FeatureFlagService;
import org.example.learningprojectserver.service.SystemHealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.example.learningprojectserver.constants.ControllerConstants.AdminTools.*;
import static org.example.learningprojectserver.constants.ControllerConstants.SystemAdmin.SYSTEM_ADMIN_BASE_PATH;

/** System admin tools: health screen and per-school feature flags (system admins only, SecurityConfig). */
@RestController
@RequestMapping(SYSTEM_ADMIN_BASE_PATH)
@RequiredArgsConstructor
public class AdminToolsController {

    private final SystemHealthService systemHealthService;
    private final FeatureFlagService featureFlagService;

    @GetMapping(SYSTEM_HEALTH)
    public BasicResponse<Map<String, Object>> health() {
        return systemHealthService.getHealth();
    }

    @GetMapping(FEATURE_FLAGS)
    public BasicResponse<List<Map<String, Object>>> featureFlags(@RequestParam String schoolCode) {
        return featureFlagService.getFlagsForAdmin(schoolCode);
    }

    @PostMapping(SET_FEATURE_FLAG)
    public BasicResponse<Void> setFeatureFlag(@RequestParam String schoolCode,
                                              @RequestParam String feature,
                                              @RequestParam boolean enabled) {
        return featureFlagService.setFlag(schoolCode, feature, enabled);
    }
}
