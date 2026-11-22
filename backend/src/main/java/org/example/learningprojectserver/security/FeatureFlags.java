package org.example.learningprojectserver.security;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.enums.Feature;
import org.example.learningprojectserver.service.FeatureFlagService;
import org.springframework.stereotype.Component;

/**
 * The helper code uses to check a per-school feature flag.
 * In Java: {@code featureFlags.isEnabled(schoolCode, Feature.AI_CHAT)}.
 * On an endpoint: {@code @PreAuthorize("@features.enabledForCurrentUser('AI_CHAT')")}, which
 * answers 403 with a Hebrew message when the caller's school has the feature off.
 */
@Component("features")
@RequiredArgsConstructor
public class FeatureFlags {

    private final FeatureFlagService featureFlagService;
    private final AccessGuard accessGuard;

    public boolean isEnabled(String schoolCode, Feature feature) {
        if (schoolCode == null) {
            return feature.isDefaultEnabled();
        }
        Boolean enabled = featureFlagService.flagsForSchool(schoolCode).get(feature.name());
        return enabled == null ? feature.isDefaultEnabled() : enabled;
    }

    /** True when the caller's school has the feature on; system admins always pass. */
    public boolean enabledForCurrentUser(String featureName) {
        if (accessGuard.isAdmin()) {
            return true;
        }
        Feature feature = Feature.valueOf(featureName);
        if (!isEnabled(accessGuard.currentSchoolCode(), feature)) {
            throw new FeatureDisabledException("האפשרות '" + feature.getLabel() + "' כבויה בבית הספר שלך");
        }
        return true;
    }
}
