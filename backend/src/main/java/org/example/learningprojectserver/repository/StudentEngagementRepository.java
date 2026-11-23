package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.StudentEngagementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentEngagementRepository extends JpaRepository<StudentEngagementEntity, Long> {
    Optional<StudentEngagementEntity> findByStudentUserId(String studentUserId);
}
