package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.dto.TeacherTestColumnDTO;
import org.example.learningprojectserver.dto.TeacherTestScoreDTO;
import org.example.learningprojectserver.entities.GradebookAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GradebookAssignmentRepository extends JpaRepository<GradebookAssignmentEntity, Long> {
    List<GradebookAssignmentEntity> findByTeacherIdAndClassRoomIdOrderByAssignedDateAscIdAsc(String teacherId, Long classRoomId);

    /** The teacher's system tests that were given to at least one student of the class. */
    @Query("""
        SELECT DISTINCT new org.example.learningprojectserver.dto.TeacherTestColumnDTO(t.id, t.subject, t.topic, t.startTime)
        FROM TeacherTestEntity t JOIN t.students s
        WHERE t.teacher.userId = :teacherId AND s.classRoom.id = :classRoomId
        ORDER BY t.id
    """)
    List<TeacherTestColumnDTO> findTeacherTestsForClass(@Param("teacherId") String teacherId,
                                                         @Param("classRoomId") Long classRoomId);

    @Query("""
        SELECT new org.example.learningprojectserver.dto.TeacherTestScoreDTO(r.test.id, r.student.userId, r.score)
        FROM TeacherTestResultEntity r
        WHERE r.test.id IN :testIds
    """)
    List<TeacherTestScoreDTO> findTeacherTestScores(@Param("testIds") Collection<Long> testIds);
}
