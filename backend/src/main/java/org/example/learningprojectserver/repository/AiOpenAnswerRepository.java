package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.AiOpenAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiOpenAnswerRepository extends JpaRepository<AiOpenAnswerEntity, Long> {

    @Query("SELECT a FROM AiOpenAnswerEntity a JOIN FETCH a.rubric r WHERE r.id = :rubricId ORDER BY a.id")
    List<AiOpenAnswerEntity> findByRubricId(@Param("rubricId") Long rubricId);

    @Query("SELECT a FROM AiOpenAnswerEntity a JOIN FETCH a.rubric WHERE a.studentUserId = :studentUserId ORDER BY a.id DESC")
    List<AiOpenAnswerEntity> findByStudent(@Param("studentUserId") String studentUserId);

    @Query("SELECT a FROM AiOpenAnswerEntity a JOIN FETCH a.rubric r WHERE r.id = :rubricId AND a.studentUserId = :studentUserId")
    AiOpenAnswerEntity findByRubricAndStudent(@Param("rubricId") Long rubricId, @Param("studentUserId") String studentUserId);

    @Query("SELECT a FROM AiOpenAnswerEntity a JOIN FETCH a.rubric WHERE a.id = :id")
    AiOpenAnswerEntity findWithRubric(@Param("id") Long id);
}
