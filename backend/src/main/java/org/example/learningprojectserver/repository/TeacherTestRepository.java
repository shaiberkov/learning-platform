package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.TeacherEntity;
import org.example.learningprojectserver.entities.TeacherTestEntity;
import org.example.learningprojectserver.entities.TestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TeacherTestRepository extends JpaRepository<TeacherTestEntity, Long> {
    @Query("SELECT t FROM TeacherTestEntity t WHERE t.id = :testId")
    TeacherTestEntity findTeacherTestByTestId(@Param("testId") Long testId);

    @Query("SELECT COUNT(s) > 0 FROM TeacherTestEntity t JOIN t.students s WHERE t.id = :testId AND s.userId = :userId")
    boolean isStudentAssigned(@Param("testId") Long testId, @Param("userId") String userId);

    @Query("SELECT t FROM TeacherTestEntity t WHERE t.teacher.userId = :teacherId ORDER BY t.id DESC")
    List<TeacherTestEntity> findByTeacherUserId(@Param("teacherId") String teacherId);

    @Query("""
        SELECT t.id FROM TeacherTestEntity t
        WHERE t.published = false
          AND (t.template IS NULL OR t.template = false)
          AND t.publishAt IS NOT NULL AND t.publishAt <= :now
    """)
    List<Long> findIdsDueForPublish(@Param("now") LocalDateTime now);
}
