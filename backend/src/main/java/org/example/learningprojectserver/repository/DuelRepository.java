package org.example.learningprojectserver.repository;

import jakarta.persistence.LockModeType;
import org.example.learningprojectserver.entities.DuelEntity;
import org.example.learningprojectserver.enums.DuelStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DuelRepository extends JpaRepository<DuelEntity, Long> {

    /** The duel row locked for the answer write, so the two players' counters never overwrite each other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DuelEntity d WHERE d.id = :id")
    Optional<DuelEntity> findForUpdate(@Param("id") Long id);

    @Query("""
        SELECT d FROM DuelEntity d
        WHERE d.challengerId = :userId OR d.opponentId = :userId
        ORDER BY d.createdAt DESC
    """)
    List<DuelEntity> findForStudent(@Param("userId") String userId, Pageable page);

    @Query("""
        SELECT COUNT(d) FROM DuelEntity d
        WHERE d.status IN :statuses
          AND ((d.challengerId = :a AND d.opponentId = :b) OR (d.challengerId = :b AND d.opponentId = :a))
    """)
    long countOpenBetween(@Param("a") String a, @Param("b") String b, @Param("statuses") Collection<DuelStatus> statuses);

    @Query("SELECT COUNT(d) FROM DuelEntity d WHERE d.challengerId = :userId AND d.status IN :statuses")
    long countOpenByChallenger(@Param("userId") String userId, @Param("statuses") Collection<DuelStatus> statuses);

    /** Rows: userId, username, classRoom id (may be null). Every student of the school except the caller. */
    @Query("""
        SELECT s.userId, s.username, c.id FROM StudentEntity s LEFT JOIN s.classRoom c
        WHERE s.schoolName.schoolCode = :schoolCode AND s.userId <> :userId
        ORDER BY s.username
    """)
    List<Object[]> findSchoolmates(@Param("schoolCode") String schoolCode, @Param("userId") String userId);

    /** Rows: username, school code, classRoom id (may be null). Empty when the user is not a student. */
    @Query("""
        SELECT s.username, s.schoolName.schoolCode, c.id FROM StudentEntity s LEFT JOIN s.classRoom c
        WHERE s.userId = :userId
    """)
    List<Object[]> findStudentInfo(@Param("userId") String userId);
}
