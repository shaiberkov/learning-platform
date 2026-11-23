package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.TeacherInsightEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherInsightRepository extends JpaRepository<TeacherInsightEntity, Long> {

    TeacherInsightEntity findFirstByTeacherUserIdOrderByCreatedAtDesc(String teacherUserId);
}
