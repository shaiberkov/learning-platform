package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.StudentCosmeticEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentCosmeticRepository extends JpaRepository<StudentCosmeticEntity, Long> {

    List<StudentCosmeticEntity> findByStudentUserId(String studentUserId);

    boolean existsByStudentUserIdAndItemCode(String studentUserId, String itemCode);
}
