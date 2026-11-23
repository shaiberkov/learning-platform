package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.StudyPlanTaskEntity;
import org.example.learningprojectserver.entities.TeacherTestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface StudyPlanTaskRepository extends JpaRepository<StudyPlanTaskEntity, Long> {
    List<StudyPlanTaskEntity> findByStudentUserIdAndTestIdInOrderByTaskDate(String studentUserId, Collection<Long> testIds);

    boolean existsByStudentUserIdAndTestId(String studentUserId, Long testId);

    @Query("SELECT t FROM TeacherTestEntity t JOIN t.students s WHERE s.userId = :userId")
    List<TeacherTestEntity> findTeacherTestsForStudent(@Param("userId") String userId);

    @Query("SELECT r.test.id FROM TeacherTestResultEntity r WHERE r.student.userId = :userId")
    List<Long> findSubmittedTestIds(@Param("userId") String userId);
}
