package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.StudentBadgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentBadgeRepository extends JpaRepository<StudentBadgeEntity, Long> {
    List<StudentBadgeEntity> findByStudentUserIdOrderByEarnedAtDesc(String studentUserId);

    boolean existsByStudentUserIdAndCode(String studentUserId, String code);
}
