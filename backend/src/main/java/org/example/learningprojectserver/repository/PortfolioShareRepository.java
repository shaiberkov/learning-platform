package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.PortfolioShareEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortfolioShareRepository extends JpaRepository<PortfolioShareEntity, Long> {

    Optional<PortfolioShareEntity> findByStudentUserId(String studentUserId);

    Optional<PortfolioShareEntity> findByToken(String token);
}
