package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    Page<NotificationEntity> findByUserIdOrderByIdDesc(String userId, Pageable pageable);

    // Missed notifications after the last one the client saw (oldest first).
    List<NotificationEntity> findByUserIdAndIdGreaterThanOrderByIdAsc(String userId, Long afterId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(String userId);

    NotificationEntity findByIdAndUserId(Long id, String userId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.readAt = :now WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") String userId, @Param("now") LocalDateTime now);
}
