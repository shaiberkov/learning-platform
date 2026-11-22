package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.dto.AttendanceCountDTO;
import org.example.learningprojectserver.dto.AttendanceLessonDTO;
import org.example.learningprojectserver.entities.AttendanceRecordEntity;
import org.example.learningprojectserver.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecordEntity, Long> {

    @Query("SELECT a FROM AttendanceRecordEntity a WHERE a.lesson.id = :lessonId AND a.lessonDate = :date")
    List<AttendanceRecordEntity> findByLessonAndDate(@Param("lessonId") Long lessonId, @Param("date") LocalDate date);

    @Query("""
        SELECT a.lesson.id, COUNT(a) FROM AttendanceRecordEntity a
        WHERE a.lesson.id IN :lessonIds AND a.lessonDate = :date
        GROUP BY a.lesson.id
    """)
    List<Object[]> countMarkedByLesson(@Param("lessonIds") Collection<Long> lessonIds, @Param("date") LocalDate date);

    @Query("""
        SELECT COUNT(a) FROM AttendanceRecordEntity a
        WHERE a.studentUserId = :studentId AND a.status = :status AND a.lessonDate BETWEEN :from AND :to
    """)
    long countByStudentAndStatus(@Param("studentId") String studentId, @Param("status") AttendanceStatus status,
                                 @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** The teacher's timetable lessons on one weekday. */
    @Query("""
        SELECT new org.example.learningprojectserver.dto.AttendanceLessonDTO(
            l.id, l.subject, l.startTime, l.endTime, c.id, c.name, SIZE(c.students))
        FROM LessonEntity l JOIN l.schedule s JOIN s.classRoom c
        WHERE l.teacher.userId = :teacherId AND l.dayOfWeek = :day
        ORDER BY l.startTime
    """)
    List<AttendanceLessonDTO> findTeacherLessonsOnDay(@Param("teacherId") String teacherId, @Param("day") DayOfWeek day);

    /** teacherId null = the whole school; className null = every class. */
    @Query("""
        SELECT new org.example.learningprojectserver.dto.AttendanceCountDTO(
            a.studentUserId, a.studentName, a.classRoomName, a.status, COUNT(a))
        FROM AttendanceRecordEntity a
        WHERE a.schoolCode = :schoolCode
          AND a.lessonDate BETWEEN :from AND :to
          AND (:teacherId IS NULL OR a.markedByTeacherId = :teacherId)
          AND (:className IS NULL OR a.classRoomName = :className)
        GROUP BY a.studentUserId, a.studentName, a.classRoomName, a.status
    """)
    List<AttendanceCountDTO> countForReport(@Param("schoolCode") String schoolCode,
                                            @Param("teacherId") String teacherId,
                                            @Param("className") String className,
                                            @Param("from") LocalDate from,
                                            @Param("to") LocalDate to);

    @Query("""
        SELECT DISTINCT a.classRoomName FROM AttendanceRecordEntity a
        WHERE a.schoolCode = :schoolCode AND (:teacherId IS NULL OR a.markedByTeacherId = :teacherId)
        ORDER BY a.classRoomName
    """)
    List<String> findReportClassNames(@Param("schoolCode") String schoolCode, @Param("teacherId") String teacherId);
}
