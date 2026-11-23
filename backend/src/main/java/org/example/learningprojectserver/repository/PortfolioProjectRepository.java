package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.PortfolioProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioProjectRepository extends JpaRepository<PortfolioProjectEntity, Long> {

    List<PortfolioProjectEntity> findByStudentUserIdOrderByCreatedAtDesc(String studentUserId);

    Optional<PortfolioProjectEntity> findByIdAndStudentUserId(Long id, String studentUserId);

    long countByStudentUserId(String studentUserId);
}
