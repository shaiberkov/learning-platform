package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.entities.SchoolFeatureFlagEntity;
import org.example.learningprojectserver.enums.AuditAction;
import org.example.learningprojectserver.enums.Feature;
import org.example.learningprojectserver.repository.SchoolFeatureFlagRepository;
import org.example.learningprojectserver.repository.SchoolRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.CurrentUser;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Per-school feature flags. Code checks a flag through security/FeatureFlags, not this class. */
@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final SchoolFeatureFlagRepository schoolFeatureFlagRepository;
    private final SchoolRepository schoolRepository;
    private final AuditService auditService;

    /** The effective flags of a school (stored value, or the feature's default), keyed by Feature name. */
    @Cacheable(value = "schoolFeatureFlags", key = "#schoolCode")
    public Map<String, Boolean> flagsForSchool(String schoolCode) {
        Map<String, Boolean> flags = new HashMap<>();
        for (Feature feature : Feature.values()) {
            flags.put(feature.name(), feature.isDefaultEnabled());
        }
        for (SchoolFeatureFlagEntity stored : schoolFeatureFlagRepository.findBySchoolCode(schoolCode)) {
            flags.put(stored.getFeature().name(), stored.isEnabled());
        }
        return flags;
    }

    /** The flags of a school for the admin screen: one row per feature with its label and default. */
    public BasicResponse<List<Map<String, Object>>> getFlagsForAdmin(String schoolCode) {
        if (schoolCode == null || schoolRepository.findBySchoolCode(schoolCode) == null) {
            return new BasicResponse<>(false, "בית הספר לא נמצא");
        }
        Map<Feature, SchoolFeatureFlagEntity> stored = new HashMap<>();
        for (SchoolFeatureFlagEntity flag : schoolFeatureFlagRepository.findBySchoolCode(schoolCode)) {
            stored.put(flag.getFeature(), flag);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Feature feature : Feature.values()) {
            SchoolFeatureFlagEntity flag = stored.get(feature);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("feature", feature.name());
            row.put("label", feature.getLabel());
            row.put("enabled", flag != null ? flag.isEnabled() : feature.isDefaultEnabled());
            row.put("defaultEnabled", feature.isDefaultEnabled());
            row.put("updatedAt", flag != null ? flag.getUpdatedAt() : null);
            row.put("updatedBy", flag != null ? flag.getUpdatedBy() : null);
            rows.add(row);
        }
        BasicResponse<List<Map<String, Object>>> response = new BasicResponse<>(true, null);
        response.setData(rows);
        return response;
    }

    @CacheEvict(value = "schoolFeatureFlags", key = "#schoolCode")
    public BasicResponse<Void> setFlag(String schoolCode, String featureName, boolean enabled) {
        if (schoolCode == null || schoolRepository.findBySchoolCode(schoolCode) == null) {
            return new BasicResponse<>(false, "בית הספר לא נמצא");
        }
        Feature feature;
        try {
            feature = Feature.valueOf(featureName);
        } catch (IllegalArgumentException | NullPointerException e) {
            return new BasicResponse<>(false, "פיצ'ר לא מוכר");
        }

        SchoolFeatureFlagEntity flag = schoolFeatureFlagRepository.findBySchoolCodeAndFeature(schoolCode, feature)
                .orElseGet(() -> {
                    SchoolFeatureFlagEntity created = new SchoolFeatureFlagEntity();
                    created.setSchoolCode(schoolCode);
                    created.setFeature(feature);
                    created.setEnabled(feature.isDefaultEnabled());
                    return created;
                });
        boolean before = flag.isEnabled();
        flag.setEnabled(enabled);
        flag.setUpdatedAt(LocalDateTime.now());
        flag.setUpdatedBy(CurrentUser.userId());
        schoolFeatureFlagRepository.save(flag);

        auditService.record(AuditAction.FEATURE_FLAG_CHANGED, "FEATURE_FLAG", feature.name(),
                "enabled=" + before, "enabled=" + enabled, schoolCode);

        return new BasicResponse<>(true, "ההגדרה '" + feature.getLabel() + "' " + (enabled ? "הופעלה" : "כובתה")
                + " לבית הספר " + schoolCode);
    }
}
