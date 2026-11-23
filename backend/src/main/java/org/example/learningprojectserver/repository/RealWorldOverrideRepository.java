package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.RealWorldOverrideEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RealWorldOverrideRepository extends JpaRepository<RealWorldOverrideEntity, Long> {

    List<RealWorldOverrideEntity> findBySchoolCode(String schoolCode);

    Optional<RealWorldOverrideEntity> findBySchoolCodeAndTopic(String schoolCode, String topic);
}
