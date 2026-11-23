package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.StudentReflectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentReflectionRepository extends JpaRepository<StudentReflectionEntity, Long> {

    Optional<StudentReflectionEntity> findByStudentUserIdAndWeekStart(String studentUserId, LocalDate weekStart);

    Optional<StudentReflectionEntity> findByIdAndStudentUserId(Long id, String studentUserId);

    List<StudentReflectionEntity> findTop12ByStudentUserIdOrderByWeekStartDesc(String studentUserId);

    /** Counts one practice answer toward this week's PRACTICE_QUESTIONS goal, if the student set one. */
    @Modifying
    @Query("""
        UPDATE StudentReflectionEntity r SET r.goalProgress = r.goalProgress + 1
        WHERE r.studentUserId = :userId AND r.weekStart = :weekStart AND r.goalType = :goalType
    """)
    int incrementProgress(@Param("userId") String userId, @Param("weekStart") LocalDate weekStart,
                          @Param("goalType") String goalType);

    /** Students of a school who have not written this week's reflection yet (Sunday reminder). */
    @Query("""
        SELECT s.userId FROM StudentEntity s
        WHERE s.schoolName IS NOT NULL
          AND s.userId NOT IN (SELECT r.studentUserId FROM StudentReflectionEntity r WHERE r.weekStart = :weekStart)
    """)
    List<String> findStudentsWithoutReflection(@Param("weekStart") LocalDate weekStart);
}
