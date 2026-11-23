package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.ForumPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPostEntity, Long> {

    List<ForumPostEntity> findByThreadIdOrderByCreatedAtAsc(Long threadId);

    boolean existsByThreadIdAndApprovedTrue(Long threadId);

    long countByThreadId(Long threadId);

    @Modifying
    @Query("DELETE FROM ForumPostEntity p WHERE p.threadId = :threadId")
    int deleteByThreadId(@Param("threadId") Long threadId);
}
