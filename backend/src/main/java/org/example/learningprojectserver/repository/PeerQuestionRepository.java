package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.PeerQuestionEntity;
import org.example.learningprojectserver.enums.PeerQuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PeerQuestionRepository extends JpaRepository<PeerQuestionEntity, Long> {

    List<PeerQuestionEntity> findByAuthorUserIdOrderByCreatedAtDesc(String authorUserId);

    long countByAuthorUserIdAndStatus(String authorUserId, PeerQuestionStatus status);

    List<PeerQuestionEntity> findByClassRoomIdAndStatusOrderByReviewedAtDesc(Long classRoomId, PeerQuestionStatus status);

    List<PeerQuestionEntity> findByClassRoomIdInAndStatusOrderByCreatedAtAsc(Collection<Long> classRoomIds,
                                                                             PeerQuestionStatus status);

    /** Moves a question between review states only if it is still in {@code from}; 0 when another review won. */
    @Modifying
    @Query("UPDATE PeerQuestionEntity q SET q.status = :to WHERE q.id = :id AND q.status = :from")
    int switchStatus(@Param("id") Long id, @Param("from") PeerQuestionStatus from, @Param("to") PeerQuestionStatus to);

    /** One classmate answer to an approved question, counted on its bank question (not a "time asked"). */
    @Modifying
    @Query("""
        UPDATE BankQuestionEntity q
        SET q.attempts = q.attempts + 1,
            q.correctAttempts = q.correctAttempts + :correct
        WHERE q.id = :id
    """)
    int recordBankAnswer(@Param("id") Long bankQuestionId, @Param("correct") int correct);
}
