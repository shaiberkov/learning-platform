package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.AiConversationMessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface AiConversationMessageRepository extends JpaRepository<AiConversationMessageEntity, Long> {

    /** Newest first; the caller reverses. A null questionId means the free chat. */
    @Query("""
        SELECT m FROM AiConversationMessageEntity m
        WHERE m.userId = :userId
          AND ((:questionId IS NULL AND m.questionId IS NULL) OR m.questionId = :questionId)
          AND m.flagged = false
        ORDER BY m.createdAt DESC, m.id DESC
    """)
    List<AiConversationMessageEntity> findRecent(@Param("userId") String userId,
                                                 @Param("questionId") Long questionId,
                                                 Pageable pageable);

    @Query("""
        SELECT m FROM AiConversationMessageEntity m
        WHERE m.schoolCode = :schoolCode AND m.flagged = true
        ORDER BY m.createdAt DESC
    """)
    List<AiConversationMessageEntity> findFlaggedBySchool(@Param("schoolCode") String schoolCode, Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM AiConversationMessageEntity m WHERE m.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);

    @Modifying
    @Transactional
    @Query("DELETE FROM AiConversationMessageEntity m WHERE m.userId = :userId AND m.flagged = false")
    int deleteUnflaggedByUser(@Param("userId") String userId);
}
