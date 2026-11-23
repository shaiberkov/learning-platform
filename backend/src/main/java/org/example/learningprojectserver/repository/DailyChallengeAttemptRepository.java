package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.DailyChallengeAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyChallengeAttemptRepository extends JpaRepository<DailyChallengeAttemptEntity, Long> {

    Optional<DailyChallengeAttemptEntity> findByChallengeIdAndStudentUserId(Long challengeId, String studentUserId);

    long countByChallengeIdAndCorrectTrue(Long challengeId);

    /** Dates the student answered correctly on or after {@code from}, newest first (for the streak). */
    @Query("""
        SELECT a.challengeDate FROM DailyChallengeAttemptEntity a
        WHERE a.studentUserId = :userId AND a.correct = true AND a.challengeDate >= :from
        ORDER BY a.challengeDate DESC
    """)
    List<LocalDate> findCorrectDates(@Param("userId") String userId, @Param("from") LocalDate from);
}
