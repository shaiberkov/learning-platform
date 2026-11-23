package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.ReviewItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewItemRepository extends JpaRepository<ReviewItemEntity, Long> {

    Optional<ReviewItemEntity> findByStudentUserIdAndQuestionId(String studentUserId, Long questionId);

    Optional<ReviewItemEntity> findByIdAndStudentUserId(Long id, String studentUserId);

    @Query("""
        SELECT r FROM ReviewItemEntity r
        WHERE r.studentUserId = :userId AND r.dueDate <= :today
        ORDER BY r.dueDate ASC, r.id ASC
    """)
    List<ReviewItemEntity> findDue(@Param("userId") String userId, @Param("today") LocalDate today);

    @Query("SELECT COUNT(r) FROM ReviewItemEntity r WHERE r.studentUserId = :userId AND r.dueDate <= :today")
    long countDue(@Param("userId") String userId, @Param("today") LocalDate today);

    @Query("SELECT COUNT(r) FROM ReviewItemEntity r WHERE r.studentUserId = :userId AND r.dueDate = :day")
    long countDueOn(@Param("userId") String userId, @Param("day") LocalDate day);

    long countByStudentUserId(String studentUserId);

    /** Items answered correctly at least three reviews in a row. */
    @Query("SELECT COUNT(r) FROM ReviewItemEntity r WHERE r.studentUserId = :userId AND r.repetitions >= 3")
    long countLearned(@Param("userId") String userId);

    /* [s06] Mistake-type diagnosis */
    List<ReviewItemEntity> findTop200ByStudentUserIdOrderByCreatedAtDescIdDesc(String studentUserId);

    /** Rows: errorType (may be null for items from before the diagnosis), count. */
    @Query("SELECT r.errorType, COUNT(r) FROM ReviewItemEntity r WHERE r.studentUserId = :userId GROUP BY r.errorType")
    List<Object[]> countByErrorType(@Param("userId") String userId);

    /** Rows: studentUserId, errorType, count. */
    @Query("""
        SELECT r.studentUserId, r.errorType, COUNT(r) FROM ReviewItemEntity r
        WHERE r.studentUserId IN :userIds
        GROUP BY r.studentUserId, r.errorType
    """)
    List<Object[]> countByStudentAndErrorType(@Param("userIds") List<String> userIds);
}
