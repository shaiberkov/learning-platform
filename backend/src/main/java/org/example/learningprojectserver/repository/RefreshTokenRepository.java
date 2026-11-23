package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = :now WHERE r.familyId = :familyId AND r.revokedAt IS NULL")
    int revokeFamily(@Param("familyId") String familyId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = :now WHERE r.userId = :userId AND r.revokedAt IS NULL")
    int revokeAllForUser(@Param("userId") String userId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
