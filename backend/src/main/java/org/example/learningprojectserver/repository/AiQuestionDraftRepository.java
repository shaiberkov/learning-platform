package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.AiQuestionDraftEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiQuestionDraftRepository extends JpaRepository<AiQuestionDraftEntity, Long> {

    List<AiQuestionDraftEntity> findByTeacherUserIdAndStatusOrderByIdAsc(String teacherUserId, AiQuestionDraftEntity.Status status);
}
