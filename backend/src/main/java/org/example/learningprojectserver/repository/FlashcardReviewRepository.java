package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.FlashcardReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlashcardReviewRepository extends JpaRepository<FlashcardReviewEntity, Long> {

    Optional<FlashcardReviewEntity> findByStudentUserIdAndFlashcardId(String studentUserId, Long flashcardId);

    List<FlashcardReviewEntity> findByStudentUserIdOrderByDueDateAscIdAsc(String studentUserId);

    @Query("""
        SELECT r FROM FlashcardReviewEntity r
        WHERE r.studentUserId = :userId AND r.dueDate <= :today
        ORDER BY r.dueDate ASC, r.id ASC
    """)
    List<FlashcardReviewEntity> findDue(@Param("userId") String userId, @Param("today") LocalDate today);

    @Query("SELECT COUNT(r) FROM FlashcardReviewEntity r WHERE r.studentUserId = :userId AND r.dueDate <= :today")
    long countDue(@Param("userId") String userId, @Param("today") LocalDate today);

    long countByStudentUserId(String studentUserId);

    @Modifying
    @Query("DELETE FROM FlashcardReviewEntity r WHERE r.flashcardId = :flashcardId")
    void deleteByFlashcardId(@Param("flashcardId") Long flashcardId);
}
