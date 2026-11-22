package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.dto.ai.AiUsageByFeatureDTO;
import org.example.learningprojectserver.entities.AiUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AiUsageRepository extends JpaRepository<AiUsageEntity, Long> {

    @Query("SELECT COUNT(u) FROM AiUsageEntity u WHERE u.userId = :userId AND u.cached = false AND u.createdAt >= :since")
    long countBilledSince(@Param("userId") String userId, @Param("since") LocalDateTime since);

    @Query("""
        SELECT new org.example.learningprojectserver.dto.ai.AiUsageByFeatureDTO(
            u.feature, COUNT(u), SUM(u.promptTokens), SUM(u.completionTokens), SUM(u.costUsd))
        FROM AiUsageEntity u
        WHERE u.schoolCode = :schoolCode AND u.createdAt >= :since
        GROUP BY u.feature
        ORDER BY u.feature
    """)
    List<AiUsageByFeatureDTO> summarizeBySchool(@Param("schoolCode") String schoolCode, @Param("since") LocalDateTime since);
}
