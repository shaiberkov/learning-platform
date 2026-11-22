package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.TeacherTestResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherTestResultRepository extends JpaRepository<TeacherTestResultEntity,Long> {

    @Query("SELECT COUNT(r) FROM TeacherTestResultEntity r WHERE r.test.id = :testId AND r.student.userId = :userId")
    long countForTestAndStudent(@Param("testId") Long testId, @Param("userId") String userId);

    default boolean existsForTestAndStudent(Long testId, String userId) {
        return countForTestAndStudent(testId, userId) > 0;
    }


}
