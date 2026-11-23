package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.ExamAttemptEntity;
import org.example.learningprojectserver.enums.ExamAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttemptEntity, Long> {

    @Query("SELECT a FROM ExamAttemptEntity a WHERE a.test.id = :testId AND a.studentUserId = :userId")
    ExamAttemptEntity findByTestAndStudent(@Param("testId") Long testId, @Param("userId") String userId);

    @Query("SELECT a.id FROM ExamAttemptEntity a WHERE a.status = :status AND a.deadline < :before")
    List<Long> findIdsByStatusAndDeadlineBefore(@Param("status") ExamAttemptStatus status,
                                                @Param("before") LocalDateTime before);

    @Query("SELECT a FROM ExamAttemptEntity a WHERE a.test.id = :testId ORDER BY a.startedAt")
    List<ExamAttemptEntity> findByTestId(@Param("testId") Long testId);

    @Query("SELECT COUNT(a) FROM ExamAttemptEntity a WHERE a.test.id = :testId AND a.status <> :status")
    long countByTestIdAndStatusNot(@Param("testId") Long testId, @Param("status") ExamAttemptStatus status);
}
