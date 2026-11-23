package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.FocusSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FocusSessionRepository extends JpaRepository<FocusSessionEntity, Long> {

    List<FocusSessionEntity> findByStudentUserIdAndStartedAtAfterOrderByStartedAtDesc(String studentUserId, LocalDateTime from);

    long countByStudentUserId(String studentUserId);

    /**
     * The student's answered exam questions since a date, for the study-habit insights.
     * Rows: testId, createdAt, secondsSpent (nullable), correct; in answer order within each test.
     */
    @Query("""
        SELECT r.testId, r.createdAt, r.secondsSpent, r.correct
        FROM TestAnswerRecordEntity r
        WHERE r.studentUserId = :userId AND r.createdAt >= :from
        ORDER BY r.testId, r.id
    """)
    List<Object[]> answerTimings(@Param("userId") String userId, @Param("from") LocalDateTime from);
}
