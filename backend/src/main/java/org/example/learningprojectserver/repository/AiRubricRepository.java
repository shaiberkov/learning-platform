package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.AiRubricEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiRubricRepository extends JpaRepository<AiRubricEntity, Long> {

    List<AiRubricEntity> findByTeacherUserIdOrderByIdDesc(String teacherUserId);

    List<AiRubricEntity> findBySchoolCodeAndPublishedTrueOrderByIdDesc(String schoolCode);
}
