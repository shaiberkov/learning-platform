package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.ExamSimAttemptEntity;
import org.example.learningprojectserver.enums.ExamAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamSimAttemptRepository extends JpaRepository<ExamSimAttemptEntity, Long> {

    @Query("SELECT a FROM ExamSimAttemptEntity a JOIN FETCH a.exam WHERE a.studentUserId = :userId ORDER BY a.startedAt DESC")
    List<ExamSimAttemptEntity> findByStudent(@Param("userId") String userId);

    @Query("""
        SELECT a FROM ExamSimAttemptEntity a
        WHERE a.studentUserId = :userId AND a.exam.id = :examId AND a.status = :status
        ORDER BY a.startedAt DESC
    """)
    List<ExamSimAttemptEntity> findByStudentExamAndStatus(@Param("userId") String userId,
                                                          @Param("examId") Long examId,
                                                          @Param("status") ExamAttemptStatus status);
}
