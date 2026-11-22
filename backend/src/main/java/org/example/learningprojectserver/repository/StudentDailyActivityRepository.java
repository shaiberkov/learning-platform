package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.StudentDailyActivityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentDailyActivityRepository extends JpaRepository<StudentDailyActivityEntity, Long> {
    Optional<StudentDailyActivityEntity> findFirstByStudentUserIdAndActivityDate(String studentUserId, LocalDate activityDate);

    List<StudentDailyActivityEntity> findByStudentUserIdAndActivityDateBetweenOrderByActivityDate(
            String studentUserId, LocalDate from, LocalDate to);

    @Query("""
        SELECT COALESCE(SUM(a.xp), 0) FROM StudentDailyActivityEntity a
        WHERE a.studentUserId IN :studentUserIds AND a.activityDate BETWEEN :from AND :to
    """)
    long sumXpForStudentsBetween(@Param("studentUserIds") Collection<String> studentUserIds,
                                 @Param("from") LocalDate from, @Param("to") LocalDate to);
}
