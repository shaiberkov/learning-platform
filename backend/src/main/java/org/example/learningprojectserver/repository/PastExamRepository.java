package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.PastExamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PastExamRepository extends JpaRepository<PastExamEntity, Long> {
    List<PastExamEntity> findAllByOrderByExamYearDescIdAsc();
}
