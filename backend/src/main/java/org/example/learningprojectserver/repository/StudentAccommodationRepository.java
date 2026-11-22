package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.dto.StudentBriefDTO;
import org.example.learningprojectserver.entities.StudentAccommodationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentAccommodationRepository extends JpaRepository<StudentAccommodationEntity, Long> {
    Optional<StudentAccommodationEntity> findByStudentUserId(String studentUserId);

    List<StudentAccommodationEntity> findByStudentUserIdIn(Collection<String> studentUserIds);

    /** Students in every class the teacher teaches. Shared by homework, accommodations and challenges. */
    @Query("""
        SELECT new org.example.learningprojectserver.dto.StudentBriefDTO(s.userId, s.username, c.id, c.name)
        FROM TeacherEntity t JOIN t.teachingClassRooms c JOIN c.students s
        WHERE t.userId = :teacherId
        ORDER BY c.name, s.username
    """)
    List<StudentBriefDTO> findStudentsOfTeacher(@Param("teacherId") String teacherId);

    /** Class of a student, for the class challenges shown to that student. */
    @Query("SELECT s.classRoom.id FROM StudentEntity s WHERE s.userId = :studentId")
    Long findClassRoomIdOfStudent(@Param("studentId") String studentId);

    /** User ids of everyone in a class. */
    @Query("SELECT s.userId FROM StudentEntity s WHERE s.classRoom.id = :classRoomId")
    List<String> findStudentUserIdsInClass(@Param("classRoomId") Long classRoomId);
}
