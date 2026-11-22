package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.TopicMasteryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TopicMasteryRepository extends JpaRepository<TopicMasteryEntity, Long> {

    Optional<TopicMasteryEntity> findByStudentUserIdAndSubjectAndSubTopic(String studentUserId, String subject, String subTopic);

    List<TopicMasteryEntity> findByStudentUserId(String studentUserId);

    List<TopicMasteryEntity> findByStudentUserIdIn(Collection<String> studentUserIds);

    /** Rows: class id, class name, grade name (may be null). */
    @Query("""
        SELECT c.id, c.name, g.gradeName
        FROM TeacherEntity t JOIN t.teachingClassRooms c LEFT JOIN c.grade g
        WHERE t.userId = :teacherId
        ORDER BY g.gradeName, c.name
    """)
    List<Object[]> findTeacherClassRooms(@Param("teacherId") String teacherId);

    /** Rows: student userId, username. */
    @Query("SELECT s.userId, s.username FROM StudentEntity s WHERE s.classRoom.id = :classRoomId ORDER BY s.username")
    List<Object[]> findStudentsOfClassRoom(@Param("classRoomId") Long classRoomId);
}
