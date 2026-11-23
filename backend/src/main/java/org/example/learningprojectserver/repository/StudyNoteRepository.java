package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.dto.NoteTopicDTO;
import org.example.learningprojectserver.entities.StudyNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyNoteRepository extends JpaRepository<StudyNoteEntity, Long> {

    Optional<StudyNoteEntity> findByIdAndStudentUserId(Long id, String studentUserId);

    /** The student's notes, newest first; each filter is skipped when null. */
    @Query("""
        SELECT n FROM StudyNoteEntity n
        WHERE n.studentUserId = :userId
          AND (:topic IS NULL OR n.topic = :topic)
          AND (:targetType IS NULL OR n.targetType = :targetType)
          AND (:targetId IS NULL OR n.targetId = :targetId)
        ORDER BY n.createdAt DESC, n.id DESC
    """)
    List<StudyNoteEntity> search(@Param("userId") String userId,
                                 @Param("topic") String topic,
                                 @Param("targetType") String targetType,
                                 @Param("targetId") Long targetId);

    /** Topic → number of notes, for the topic filter. */
    @Query("""
        SELECT new org.example.learningprojectserver.dto.NoteTopicDTO(n.topic, COUNT(n))
        FROM StudyNoteEntity n
        WHERE n.studentUserId = :userId AND n.topic IS NOT NULL
        GROUP BY n.topic ORDER BY n.topic
    """)
    List<NoteTopicDTO> countByTopic(@Param("userId") String userId);
}
