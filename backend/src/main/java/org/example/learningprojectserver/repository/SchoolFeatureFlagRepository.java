package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.SchoolFeatureFlagEntity;
import org.example.learningprojectserver.enums.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolFeatureFlagRepository extends JpaRepository<SchoolFeatureFlagEntity, Long> {
    List<SchoolFeatureFlagEntity> findBySchoolCode(String schoolCode);

    Optional<SchoolFeatureFlagEntity> findBySchoolCodeAndFeature(String schoolCode, Feature feature);
}
