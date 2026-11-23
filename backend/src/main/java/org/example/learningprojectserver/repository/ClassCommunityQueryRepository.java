package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.dto.community.ClassRefDTO;
import org.example.learningprojectserver.entities.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Class membership lookups for the class forum and the student-authored questions. */
public interface ClassCommunityQueryRepository extends Repository<UserEntity, Long> {

    @Query("""
        SELECT new org.example.learningprojectserver.dto.community.ClassRefDTO(c.id, c.name, sc.schoolCode)
        FROM StudentEntity s JOIN s.classRoom c LEFT JOIN c.school sc
        WHERE s.userId = :userId
    """)
    List<ClassRefDTO> findStudentClass(@Param("userId") String userId);

    @Query("""
        SELECT new org.example.learningprojectserver.dto.community.ClassRefDTO(c.id, c.name, sc.schoolCode)
        FROM TeacherEntity t JOIN t.teachingClassRooms c LEFT JOIN c.school sc
        WHERE t.userId = :teacherId
        ORDER BY c.name
    """)
    List<ClassRefDTO> findTeacherClasses(@Param("teacherId") String teacherId);

    @Query("SELECT t.userId FROM TeacherEntity t JOIN t.teachingClassRooms c WHERE c.id = :classId")
    List<String> findTeacherIdsOfClass(@Param("classId") Long classId);

    @Query("SELECT u.username FROM UserEntity u WHERE u.userId = :userId")
    List<String> findUsername(@Param("userId") String userId);
}
