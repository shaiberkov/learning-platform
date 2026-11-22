package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.LessonContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonContentRepository extends JpaRepository<LessonContentEntity, Long> {
    List<LessonContentEntity> findByTeacherUserIdOrderByUpdatedAtDesc(String teacherUserId);

    /** Lessons written by any teacher who teaches the student's class. */
    @Query("""
        SELECT l FROM LessonContentEntity l
        WHERE l.teacherUserId IN (
            SELECT t.userId FROM TeacherEntity t JOIN t.teachingClassRooms c JOIN c.students s
            WHERE s.userId = :studentId)
        ORDER BY l.updatedAt DESC
    """)
    List<LessonContentEntity> findLessonsForStudent(@Param("studentId") String studentId);
}
