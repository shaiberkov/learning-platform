package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.TestAnswerRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TestAnswerRecordRepository extends JpaRepository<TestAnswerRecordEntity, Long> {

    List<TestAnswerRecordEntity> findByTestIdAndStudentUserIdOrderByIdAsc(Long testId, String studentUserId);

    boolean existsByTestIdAndStudentUserId(Long testId, String studentUserId);

    /**
     * Class accuracy per sub-topic from classmates' exam answers, excluding the student.
     * Rows: subTopic, correct answers, total answers, distinct students.
     */
    @Query("""
        SELECT r.subTopic, SUM(CASE WHEN r.correct = true THEN 1 ELSE 0 END), COUNT(r), COUNT(DISTINCT r.studentUserId)
        FROM TestAnswerRecordEntity r
        WHERE r.classRoomId = :classRoomId AND r.studentUserId <> :userId AND r.subTopic IN :subTopics
        GROUP BY r.subTopic
    """)
    List<Object[]> classAccuracyBySubTopic(@Param("classRoomId") Long classRoomId,
                                           @Param("userId") String userId,
                                           @Param("subTopics") Collection<String> subTopics);

    @Query("SELECT s.classRoom.id FROM StudentEntity s WHERE s.userId = :userId")
    Long findClassRoomIdOfStudent(@Param("userId") String userId);

    /** Rows: average score, number of results. */
    @Query("SELECT AVG(r.score), COUNT(r) FROM TeacherTestResultEntity r WHERE r.test.id = :testId")
    List<Object[]> teacherTestScoreStats(@Param("testId") Long testId);
}
