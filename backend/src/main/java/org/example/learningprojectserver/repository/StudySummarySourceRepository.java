package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.dto.StudySummaryDTOs;
import org.example.learningprojectserver.entities.ReviewItemEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Read-only queries the study summary (sheet and podcast) builds on: the student's mistakes
 * notebook and the teacher tests assigned to them.
 */
@org.springframework.stereotype.Repository
public interface StudySummarySourceRepository extends org.springframework.data.repository.Repository<ReviewItemEntity, Long> {

    /** The student's mistakes, the ones missed most often first. */
    @Query("""
        SELECT r FROM ReviewItemEntity r
        WHERE r.studentUserId = :userId
        ORDER BY r.lapses DESC, r.id DESC
    """)
    List<ReviewItemEntity> findMistakes(@Param("userId") String userId, Pageable page);

    @Query("SELECT DISTINCT r.topic FROM ReviewItemEntity r WHERE r.studentUserId = :userId AND r.topic IS NOT NULL")
    List<String> findMistakeTopics(@Param("userId") String userId);

    @Query("SELECT DISTINCT r.subTopic FROM ReviewItemEntity r WHERE r.studentUserId = :userId AND r.subTopic IS NOT NULL")
    List<String> findMistakeSubTopics(@Param("userId") String userId);

    @Query("SELECT COUNT(r) FROM ReviewItemEntity r WHERE r.studentUserId = :userId")
    long countMistakes(@Param("userId") String userId);

    /** Published, non-template teacher tests assigned to the student (start time is filtered in Java). */
    @Query("""
        SELECT new org.example.learningprojectserver.dto.StudySummaryDTOs$TestOption(t.id, t.title, t.subject, t.topic, t.startTime)
        FROM TeacherTestEntity t JOIN t.students s
        WHERE s.userId = :userId
          AND (t.template IS NULL OR t.template = false)
          AND (t.published IS NULL OR t.published = true)
    """)
    List<StudySummaryDTOs.TestOption> findAssignedTests(@Param("userId") String userId);

    /** The sub-topics a test's questions cover (topic names only, never the questions). */
    @Query("SELECT DISTINCT q.subTopic FROM TestQuestionEntity q WHERE q.test.id = :testId AND q.subTopic IS NOT NULL")
    List<String> findTestSubTopics(@Param("testId") Long testId);
}
