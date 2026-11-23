package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.FlashcardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlashcardRepository extends JpaRepository<FlashcardEntity, Long> {

    List<FlashcardEntity> findByClassRoomIdOrderByIdDesc(Long classRoomId);

    /** The class the student belongs to, or null. */
    @Query("SELECT s.classRoom.id FROM StudentEntity s WHERE s.userId = :userId")
    Long findClassRoomIdOfStudent(@Param("userId") String userId);
}
