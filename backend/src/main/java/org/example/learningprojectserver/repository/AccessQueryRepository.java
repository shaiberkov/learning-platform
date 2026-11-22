package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

/** Lookups used only by the ownership checks in security/AccessGuard. */
public interface AccessQueryRepository extends Repository<UserEntity, Long> {

    @Query("SELECT s.schoolName.schoolCode FROM StudentEntity s WHERE s.userId = :userId")
    String findStudentSchoolCode(@Param("userId") String userId);

    @Query("SELECT t.teachingSchool.schoolCode FROM TeacherEntity t WHERE t.userId = :userId")
    String findTeacherSchoolCode(@Param("userId") String userId);

    @Query("SELECT m.school.schoolCode FROM SchoolManagerEntity m WHERE m.userId = :userId")
    String findManagerSchoolCode(@Param("userId") String userId);

    @Query("""
        SELECT COUNT(DISTINCT s.id)
        FROM TeacherEntity t
        JOIN t.teachingClassRooms c
        JOIN c.students s
        WHERE t.userId = :teacherId AND s.userId IN :studentIds
    """)
    long countStudentsInTeacherClasses(@Param("teacherId") String teacherId,
                                       @Param("studentIds") Collection<String> studentIds);

    @Query("SELECT COUNT(t) FROM TeacherTestEntity t JOIN t.students s WHERE t.id = :testId AND s.userId = :userId")
    long countTeacherTestAssignments(@Param("testId") Long testId, @Param("userId") String userId);

    @Query("SELECT COUNT(t) FROM TeacherTestEntity t WHERE t.id = :testId AND t.teacher.userId = :userId")
    long countTeacherTestsCreatedBy(@Param("testId") Long testId, @Param("userId") String userId);

    @Query("SELECT COUNT(p) FROM PracticeTestEntity p WHERE p.id = :testId AND p.student.userId = :userId")
    long countPracticeTestsOwnedBy(@Param("testId") Long testId, @Param("userId") String userId);
}
