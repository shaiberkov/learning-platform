package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.ForumThreadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumThreadRepository extends JpaRepository<ForumThreadEntity, Long> {

    /** The class's most recently active threads (the forum list and the similar-question search). */
    List<ForumThreadEntity> findTop200ByClassRoomIdOrderByLastActivityAtDesc(Long classRoomId);
}
