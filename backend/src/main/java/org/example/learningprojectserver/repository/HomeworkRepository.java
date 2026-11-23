package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.HomeworkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface HomeworkRepository extends JpaRepository<HomeworkEntity, Long> {
    List<HomeworkEntity> findByTeacherUserIdOrderByDueDateDesc(String teacherUserId);

    @Query("SELECT h FROM HomeworkEntity h WHERE :studentId MEMBER OF h.studentUserIds ORDER BY h.dueDate DESC")
    List<HomeworkEntity> findForStudent(@Param("studentId") String studentId);

    @Query("SELECT h FROM HomeworkEntity h WHERE h.reminderSent = false AND h.dueDate BETWEEN :from AND :to")
    List<HomeworkEntity> findDueForReminder(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
