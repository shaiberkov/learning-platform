package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.StudentMoodCheckinEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentMoodCheckinRepository extends JpaRepository<StudentMoodCheckinEntity, Long> {

    Optional<StudentMoodCheckinEntity> findFirstByStudentUserIdAndCheckinDate(String studentUserId, LocalDate checkinDate);

    /** The latest check-ins, newest first. */
    List<StudentMoodCheckinEntity> findTop10ByStudentUserIdOrderByCheckinDateDescIdDesc(String studentUserId);

    boolean existsByStudentUserIdAndEscalatedTrueAndCheckinDateAfter(String studentUserId, LocalDate after);

    /** Rows: student name, class name (or null), school manager user id (or null). */
    @Query("""
        SELECT s.username, c.name, m.userId FROM StudentEntity s
        LEFT JOIN s.classRoom c LEFT JOIN s.schoolName sc LEFT JOIN sc.schoolManager m
        WHERE s.userId = :userId
    """)
    List<Object[]> findStudentContact(@Param("userId") String userId);
}
