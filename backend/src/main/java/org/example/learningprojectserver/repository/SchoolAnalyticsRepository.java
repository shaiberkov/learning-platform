package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.dto.GradeRecordDTO;
import org.example.learningprojectserver.entities.TeacherTestResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Read-only queries over graded teacher tests for the school dashboard, the parent view and the year archive. */
@Repository
public interface SchoolAnalyticsRepository extends JpaRepository<TeacherTestResultEntity, Long> {

    @Query("""
    SELECT new org.example.learningprojectserver.dto.GradeRecordDTO(
        s.userId, s.username, r.score, test.subject, test.topic, test.startTime,
        t.userId, t.username, c.name, g.gradeName
    )
    FROM TeacherTestResultEntity r
    JOIN r.student s
    JOIN r.test test
    LEFT JOIN r.teacher t
    LEFT JOIN s.classRoom c
    LEFT JOIN c.grade g
    WHERE s.schoolName.schoolCode = :schoolCode
""")
    List<GradeRecordDTO> findGradeRecordsBySchoolCode(@Param("schoolCode") String schoolCode);

    @Query("""
    SELECT new org.example.learningprojectserver.dto.GradeRecordDTO(
        s.userId, s.username, r.score, test.subject, test.topic, test.startTime,
        t.userId, t.username, c.name, g.gradeName
    )
    FROM TeacherTestResultEntity r
    JOIN r.student s
    JOIN r.test test
    LEFT JOIN r.teacher t
    LEFT JOIN s.classRoom c
    LEFT JOIN c.grade g
    WHERE s.userId = :studentId
""")
    List<GradeRecordDTO> findGradeRecordsByStudent(@Param("studentId") String studentId);

    @Query("SELECT COUNT(r) FROM PracticeTestResultEntity r WHERE r.student.userId = :studentId")
    long countPracticeResults(@Param("studentId") String studentId);

    @Query("SELECT AVG(r.score) FROM PracticeTestResultEntity r WHERE r.student.userId = :studentId")
    Double averagePracticeScore(@Param("studentId") String studentId);

    @Query("SELECT COUNT(t) FROM TeacherTestEntity t JOIN t.students s WHERE s.userId = :studentId")
    long countAssignedTeacherTests(@Param("studentId") String studentId);
}
