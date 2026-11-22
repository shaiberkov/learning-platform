package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.ExamAttemptAnswerEntity;
import org.example.learningprojectserver.enums.ExamAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamAttemptAnswerRepository extends JpaRepository<ExamAttemptAnswerEntity, Long> {

    /** One row per graded answer: attempt id, attempt score, question id, correct. */
    @Query("""
        SELECT a.attempt.id, a.attempt.score, a.questionId, a.correct
        FROM ExamAttemptAnswerEntity a
        WHERE a.attempt.test.id = :testId AND a.attempt.status <> :excluded
    """)
    List<Object[]> findGradedRowsForTest(@Param("testId") Long testId,
                                         @Param("excluded") ExamAttemptStatus excluded);
}
