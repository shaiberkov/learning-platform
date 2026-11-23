package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.DailyChallengeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyChallengeRepository extends JpaRepository<DailyChallengeEntity, Long> {

    Optional<DailyChallengeEntity> findBySchoolCodeAndGradeNameAndChallengeDate(String schoolCode, String gradeName,
                                                                               LocalDate challengeDate);

    /** Rows: school code, grade name (may be null). Empty when the user is not a student. */
    @Query("""
        SELECT s.schoolName.schoolCode, g.gradeName FROM StudentEntity s
        LEFT JOIN s.classRoom c LEFT JOIN c.grade g
        WHERE s.userId = :userId
    """)
    List<Object[]> findStudentScope(@Param("userId") String userId);
}
