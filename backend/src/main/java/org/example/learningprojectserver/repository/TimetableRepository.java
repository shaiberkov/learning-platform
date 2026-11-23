package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.dto.TimetableLessonDTO;
import org.example.learningprojectserver.entities.LessonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Lesson queries for the timetable builder (school-wide, with ids and rooms). */
@Repository
public interface TimetableRepository extends JpaRepository<LessonEntity, Long> {

    @Query("""
    SELECT new org.example.learningprojectserver.dto.TimetableLessonDTO(
        l.id, l.subject, l.dayOfWeek, l.startTime, l.endTime, c.name, t.userId, t.username, l.room
    )
    FROM LessonEntity l
    JOIN l.schedule s
    JOIN s.classRoom c
    JOIN l.teacher t
    WHERE c.school.schoolCode = :schoolCode
""")
    List<TimetableLessonDTO> findSchoolLessons(@Param("schoolCode") String schoolCode);

    @Query("""
    SELECT l FROM LessonEntity l
    JOIN FETCH l.schedule s
    JOIN FETCH s.classRoom c
    JOIN FETCH c.school
    JOIN FETCH l.teacher t
    WHERE l.id = :id
""")
    LessonEntity findWithRelations(@Param("id") Long id);
}
