package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.BankQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankQuestionRepository extends JpaRepository<BankQuestionEntity, Long> {

    /** The teacher's own bank, newest first (AI question studio). */
    List<BankQuestionEntity> findByOwnerTeacherIdOrderByIdDesc(String ownerTeacherId);

    /** The teacher's own questions, plus (when includeShared) the ones other teachers of the school shared. */
    @Query("""
        SELECT q FROM BankQuestionEntity q
        WHERE (q.ownerTeacherId = :teacherId
               OR (:includeShared = true AND q.shared = true AND q.schoolCode = :schoolCode))
          AND (:subject IS NULL OR q.subject = :subject)
          AND (:topic IS NULL OR q.topic = :topic)
          AND (:level IS NULL OR q.level = :level)
          AND (:grade IS NULL OR q.grade = :grade)
          AND (:text IS NULL OR LOWER(q.questionText) LIKE LOWER(CONCAT('%', :text, '%')))
        ORDER BY q.createdAt DESC
    """)
    List<BankQuestionEntity> search(@Param("teacherId") String teacherId,
                                    @Param("schoolCode") String schoolCode,
                                    @Param("includeShared") boolean includeShared,
                                    @Param("subject") String subject,
                                    @Param("topic") String topic,
                                    @Param("level") String level,
                                    @Param("grade") String grade,
                                    @Param("text") String text);

    @Modifying
    @Query("""
        UPDATE BankQuestionEntity q
        SET q.timesAsked = q.timesAsked + 1,
            q.attempts = q.attempts + :attempts,
            q.correctAttempts = q.correctAttempts + :correct
        WHERE q.id = :id
    """)
    int recordUsage(@Param("id") Long id, @Param("attempts") int attempts, @Param("correct") int correct);
}
