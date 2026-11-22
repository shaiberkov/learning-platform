package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.dto.MessageDTO;
import org.example.learningprojectserver.entities.MessageEntity;
import org.example.learningprojectserver.entities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    // Scheduled messages (pending = true) stay hidden from the receiver until delivered.
    @Query("""
 SELECT new org.example.learningprojectserver.dto.MessageDTO(
 m.id,
 m.title,
 m.content,
 m.sentAt,
 m.sender.username,
 m.readAt,
 m.batchId,
 m.threadId,
 m.parentMessageId,
 m.attachmentId,
 a.fileName
)
FROM MessageEntity m
LEFT JOIN MessageAttachmentEntity a ON a.id = m.attachmentId
WHERE m.receiver.userId = :userId
AND (m.pending IS NULL OR m.pending = false)
""")
    List<MessageDTO> findMessagesByRecipientUserId(@Param("userId") String userId);

    @Query(value = """
 SELECT new org.example.learningprojectserver.dto.MessageDTO(
 m.id,
 m.title,
 m.content,
 m.sentAt,
 m.sender.username,
 m.readAt,
 m.batchId,
 m.threadId,
 m.parentMessageId,
 m.attachmentId,
 a.fileName
)
FROM MessageEntity m
LEFT JOIN MessageAttachmentEntity a ON a.id = m.attachmentId
WHERE m.receiver.userId = :userId
AND (m.pending IS NULL OR m.pending = false)
ORDER BY m.sentAt DESC
""", countQuery = """
SELECT COUNT(m) FROM MessageEntity m
WHERE m.receiver.userId = :userId
AND (m.pending IS NULL OR m.pending = false)
""")
    Page<MessageDTO> findMessagesPageByRecipientUserId(@Param("userId") String userId, Pageable pageable);

    // One row per send action of this sender: [batchId, threadId, title, content, sentAt,
    // scheduledFor, pendingFlag, recipients, readCount, attachmentId].
    @Query("""
SELECT m.batchId, MIN(m.threadId), MIN(m.title), MIN(m.content), MIN(m.sentAt), MIN(m.scheduledFor),
       MAX(CASE WHEN m.pending = true THEN 1 ELSE 0 END),
       COUNT(m),
       SUM(CASE WHEN m.readAt IS NOT NULL THEN 1 ELSE 0 END),
       MIN(m.attachmentId)
FROM MessageEntity m
WHERE m.sender.userId = :userId AND m.batchId IS NOT NULL AND m.parentMessageId IS NULL
GROUP BY m.batchId
ORDER BY MIN(m.sentAt) DESC
""")
    List<Object[]> findSentBatchesBySender(@Param("userId") String userId, Pageable pageable);

    @Query("""
SELECT m.receiver.username, m.readAt FROM MessageEntity m
WHERE m.batchId = :batchId AND m.sender.userId = :userId
""")
    List<Object[]> findReadReceipts(@Param("userId") String userId, @Param("batchId") String batchId);

    @Query("""
 SELECT new org.example.learningprojectserver.dto.MessageDTO(
 m.id,
 m.title,
 m.content,
 m.sentAt,
 m.sender.username,
 m.readAt,
 m.batchId,
 m.threadId,
 m.parentMessageId,
 m.attachmentId,
 a.fileName
)
FROM MessageEntity m
LEFT JOIN MessageAttachmentEntity a ON a.id = m.attachmentId
WHERE m.threadId = :threadId
AND (m.sender.userId = :userId OR m.receiver.userId = :userId)
AND (m.pending IS NULL OR m.pending = false)
ORDER BY m.sentAt ASC, m.id ASC
""")
    List<MessageDTO> findThreadForUser(@Param("userId") String userId, @Param("threadId") String threadId);

    // Ids of the thread messages the viewer sent (to mark "mine" without exposing user ids).
    @Query("SELECT m.id FROM MessageEntity m WHERE m.threadId = :threadId AND m.sender.userId = :userId")
    List<Long> findThreadMessageIdsSentBy(@Param("userId") String userId, @Param("threadId") String threadId);

    @Query("""
SELECT COUNT(m) FROM MessageEntity m
WHERE m.attachmentId = :attachmentId
AND (m.sender.userId = :userId OR (m.receiver.userId = :userId AND (m.pending IS NULL OR m.pending = false)))
""")
    long countAttachmentAccess(@Param("userId") String userId, @Param("attachmentId") Long attachmentId);

    @Query("SELECT m FROM MessageEntity m WHERE m.pending = true AND m.scheduledFor <= :now")
    List<MessageEntity> findDueScheduledMessages(@Param("now") LocalDateTime now);

}
