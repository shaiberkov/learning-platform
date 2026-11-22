package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.ExamIntegrityEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamIntegrityEventRepository extends JpaRepository<ExamIntegrityEventEntity, Long> {

    @Query("SELECT e FROM ExamIntegrityEventEntity e WHERE e.attempt.test.id = :testId ORDER BY e.occurredAt")
    List<ExamIntegrityEventEntity> findByTestId(@Param("testId") Long testId);
}
